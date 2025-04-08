package com.example.opendrive.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.example.opendrive.obd.ObdCommand
import com.example.opendrive.util.Constants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

// Manages the Bluetooth connection lifecycle and raw data I/O
@SuppressLint("MissingPermission") // Permissions checked before use
class BluetoothConnectionManager {

    private val TAG = "BluetoothConnManager"

    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var listeningJob: Job? = null
    private var connectJob: Job? = null

    private val _state = MutableStateFlow<BluetoothState>(BluetoothState.Disconnected)
    val state: StateFlow<BluetoothState> = _state.asStateFlow()

    // Flow to emit raw, cleaned responses ending with '>'
    private val _rawDataFlow = MutableSharedFlow<String>()
    val rawDataFlow: SharedFlow<String> = _rawDataFlow.asSharedFlow()

    // Function to initiate connection
    fun connect(device: BluetoothDevice, scope: CoroutineScope) {
        if (_state.value is BluetoothState.Connected || _state.value is BluetoothState.Connecting) {
            Log.w(TAG, "Already connecting or connected.")
            return
        }
        _state.value = BluetoothState.Connecting(device)
        Log.d(TAG, "Attempting to connect to ${device.name}")

        connectJob?.cancel() // Cancel any previous connection attempt
        connectJob = scope.launch(Dispatchers.IO) {
            try {
                // Use the standard SPP UUID
                bluetoothSocket = device.createRfcommSocketToServiceRecord(Constants.SPP_UUID)
                bluetoothSocket?.connect() // Blocking call

                // Connection successful
                outputStream = bluetoothSocket?.outputStream
                inputStream = bluetoothSocket?.inputStream
                _state.value = BluetoothState.Connected(device)
                Log.i(TAG, "Connected successfully to ${device.name}")

                // Start listening for incoming data
                startListening(this) // Use the same scope

            } catch (e: IOException) {
                Log.e(TAG, "IOException during connection to ${device.name}", e)
                _state.value = BluetoothState.Error("Connection failed: ${e.message}", device)
                disconnect() // Clean up resources
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException during connection", e)
                _state.value = BluetoothState.Error("Permission denied", device)
                disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected exception during connection", e)
                _state.value = BluetoothState.Error("Connection error: ${e.message}", device)
                disconnect()
            }
        }
    }

    // Function to start the background listener
    private fun startListening(scope: CoroutineScope) {
        listeningJob?.cancel() // Cancel previous listener
        listeningJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(1024)
            var bytes: Int
            val responseBuilder = StringBuilder()

            Log.d(TAG, "Starting input stream listener...")
            while (isActive && bluetoothSocket?.isConnected == true) {
                try {
                    bytes = inputStream?.read(buffer) ?: -1 // Blocking read
                    if (bytes > 0) {
                        val readMessage = String(buffer, 0, bytes, Charset.defaultCharset())
                        responseBuilder.append(readMessage)

                        // Check if the accumulated response ends with the ELM prompt '>'
                        val completeResponse = responseBuilder.toString()
                        if (completeResponse.contains(Constants.ELM_PROMPT)) {
                            // Process potentially multiple responses separated by '>'
                            completeResponse.split(Constants.ELM_PROMPT).forEach { part ->
                                val trimmedPart = part.trim()
                                if (trimmedPart.isNotEmpty()) {
                                    Log.v(TAG, "Raw response received: $trimmedPart")
                                    _rawDataFlow.emit(trimmedPart) // Emit the cleaned part
                                }
                            }
                            // Clear the builder after processing
                            responseBuilder.clear()
                        }
                    } else if (bytes == -1) {
                        // End of stream usually means disconnected
                        Log.w(TAG, "Input stream ended, likely disconnected.")
                        if (isActive) { // Avoid setting state if scope is already cancelled
                            _state.value = BluetoothState.Error("Device disconnected")
                        }
                        break // Exit loop
                    }

                } catch (e: IOException) {
                    if (isActive) { // Only report error if the job wasn't cancelled externally
                        Log.e(TAG, "IOException during read", e)
                        _state.value = BluetoothState.Error("Read error: ${e.message}")
                    }
                    break // Exit loop on error
                }
            }
            Log.d(TAG, "Input stream listener stopped.")
            // Ensure state reflects disconnection if loop exits normally while connected
            if (_state.value is BluetoothState.Connected) {
                _state.value = BluetoothState.Disconnected // Or Error based on exit reason
            }
            disconnect() // Ensure cleanup happens when listening stops
        }
    }

    // Function to send a command string
    suspend fun sendCommand(commandString: String) {
        if (_state.value !is BluetoothState.Connected || outputStream == null) {
            Log.w(TAG, "Cannot send command: Not connected.")
            return
        }
        // ELM commands need to end with a carriage return
        val commandToSend = "$commandString\r"
        withContext(Dispatchers.IO) {
            try {
                Log.v(TAG, "Sending command: $commandString")
                outputStream?.write(commandToSend.toByteArray(Charset.defaultCharset()))
                outputStream?.flush()
            } catch (e: IOException) {
                Log.e(TAG, "IOException during write", e)
                // Consider setting state to Error and disconnecting
                _state.value = BluetoothState.Error("Write error: ${e.message}")
                disconnect()
            }
        }
    }

    // Disconnect function
    fun disconnect() {
        Log.d(TAG, "Disconnecting...")
        connectJob?.cancel()
        listeningJob?.cancel()
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "IOException during close", e)
        } finally {
            inputStream = null
            outputStream = null
            bluetoothSocket = null
            if (_state.value !is BluetoothState.Error) { // Don't overwrite error state
                _state.value = BluetoothState.Disconnected
            }
            Log.i(TAG,"Disconnected.")
        }
    }
}