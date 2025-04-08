package com.example.opendrive.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.util.Log
import com.example.opendrive.bluetooth.BluetoothConnectionManager
import com.example.opendrive.bluetooth.BluetoothState
import com.example.opendrive.obd.ObdCommand
import com.example.opendrive.obd.ObdResponse
import com.example.opendrive.obd.commands.*
import com.example.opendrive.util.Constants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@SuppressLint("MissingPermission")
class ObdRepositoryImpl(
    private val connectionManager: BluetoothConnectionManager,
    private val externalScope: CoroutineScope // Use viewModelScope or application scope
) : ObdRepository {

    private val TAG = "ObdRepository"

    override val connectionState: StateFlow<BluetoothState> = connectionManager.state

    private val initCommands = listOf(
        InitializeElmCommand("ATZ"),  // Reset ELM
        InitializeElmCommand("ATE0"), // Echo off
        InitializeElmCommand("ATL0"), // Linefeeds off
        InitializeElmCommand("ATH0"), // Headers off
        InitializeElmCommand("ATS0"), // Spaces off
        InitializeElmCommand("ATSP0") // Auto protocol
    )

    init {
        // Observe connection state to trigger ELM initialization
        externalScope.launch {
            connectionManager.state.collect { state ->
                if (state is BluetoothState.Connected) {
                    initializeElmDevice()
                }
            }
        }
    }

    // Sends initialization commands upon successful connection
    private suspend fun initializeElmDevice() {
        Log.d(TAG, "Initializing ELM327...")
        try {
            initCommands.forEach { cmd ->
                executeCommandInternal(cmd).collect { response ->
                    when (response) {
                        is ObdResponse.Success -> Log.i(TAG, "Init command '${cmd.command}' successful.")
                        is ObdResponse.Error -> Log.e(TAG, "Init command '${cmd.command}' error: ${response.message}")
                        is ObdResponse.Timeout -> Log.w(TAG, "Init command '${cmd.command}' timed out.")
                    }
                    // Add a small delay between init commands if needed
                    delay(100)
                }
            }
            Log.i(TAG, "ELM327 Initialization complete.")
        } catch (e: Exception) {
            Log.e(TAG, "Error during ELM initialization", e)
            connectionManager.disconnect() // Disconnect if init fails
        }
    }

    override fun connect(device: BluetoothDevice) {
        connectionManager.connect(device, externalScope)
    }

    override fun disconnect() {
        connectionManager.disconnect()
    }

    // Generic command execution
    override suspend fun <T> executeCommand(command: ObdCommand<T>): Flow<ObdResponse<T>> {
        return executeCommandInternal(command)
    }

    // Internal implementation to send command and listen for relevant response
    private suspend fun <T> executeCommandInternal(command: ObdCommand<T>): Flow<ObdResponse<T>> = channelFlow {
        if (connectionManager.state.value !is BluetoothState.Connected) {
            Log.w(TAG, "Cannot execute command '${command.command}': Not connected.")
            send(ObdResponse.Error("Not connected", command))
            return@channelFlow
        }

        val responseFlow = connectionManager.rawDataFlow
            .filter { response ->
                // Basic filter: Check if response seems relevant to the command
                // For PIDs (mode 01), response starts with "41" + PID
                // For AT commands, might contain "OK" or the command itself
                val cleanedResp = response.replace("\\s".toRegex(), "").uppercase()
                val cleanedCmd = command.command.replace("\\s".toRegex(), "").uppercase()

                when {
                    cleanedCmd.startsWith("01") && cleanedCmd.length >= 4 -> // PID command
                        cleanedResp.startsWith("41${cleanedCmd.substring(2,4)}")
                    cleanedCmd.startsWith("AT") -> // AT command
                        cleanedResp.contains("OK") || cleanedResp.contains(cleanedCmd) || cleanedResp.contains("?") // Handle errors too
                    else -> // Other modes might need different checks
                        true // Default to pass for now
                }
            }
            .map { rawResponse ->
                // Attempt to parse the filtered response
                val parsedValue = command.parseResponse(rawResponse)
                if (parsedValue != null) {
                    ObdResponse.Success(parsedValue, command, rawResponse) as ObdResponse<T>
                } else {
                    // Consider response invalid if parsing fails
                    Log.w(TAG, "Parsing failed for command '${command.command}' with response: $rawResponse")
                    ObdResponse.Error("Parsing failed", command, rawResponse) as ObdResponse<T>
                }
            }
            .onStart { Log.d(TAG, "Listening for response to ${command.command}") }


        // Send the command
        connectionManager.sendCommand(command.command)

        // Collect responses with a timeout
        try {
            withTimeout(command.responseTimeoutMillis) {
                // Collect the first valid response or error related to this command
                responseFlow.collect { response ->
                    send(response) // Emit the parsed response/error
                    // We might want to close the channel after the first success for simple commands,
                    // or keep it open for continuous monitoring commands if designed differently.
                    close() // Close channel after first relevant response for this simple model
                }
                // If flow completes without emitting (unlikely with filter unless timeout), means no valid response received
                // This case is handled by the timeout exception below
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Timeout waiting for response to ${command.command}")
            send(ObdResponse.Timeout(command))
        } finally {
            Log.d(TAG,"Stopped listening for response to ${command.command}")
        }
    }.buffer(1) // Buffer to avoid blocking sender if collector is slow


    // Convenience functions
    override suspend fun requestRpm(): Flow<ObdResponse<Int>> = executeCommand(RpmCommand())
    override suspend fun requestSpeed(): Flow<ObdResponse<Int>> = executeCommand(SpeedCommand())
    override suspend fun requestCoolantTemp(): Flow<ObdResponse<Int>> = executeCommand(EngineCoolantTempCommand())

}