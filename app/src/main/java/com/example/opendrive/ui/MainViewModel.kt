package com.example.opendrive.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.opendrive.bluetooth.BluetoothDeviceScanner
import com.example.opendrive.bluetooth.BluetoothState
import com.example.opendrive.di.Injection
import com.example.opendrive.obd.ObdResponse
import com.example.opendrive.repository.ObdRepository
import com.example.opendrive.util.Constants
import com.example.opendrive.util.PermissionUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@SuppressLint("MissingPermission") // Permissions checked in Activity before calling methods
class MainViewModel(
    private val context: Context, // App context
    private val repository: ObdRepository,
    private val deviceScanner: BluetoothDeviceScanner
) : ViewModel() {

    private val TAG = "MainViewModel"

    private val _statusText = MutableStateFlow("Status: Disconnected")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _rpm = MutableStateFlow<Int?>(null)
    val rpm: StateFlow<Int?> = _rpm.asStateFlow()

    private val _speed = MutableStateFlow<Int?>(null)
    val speed: StateFlow<Int?> = _speed.asStateFlow()

    private val _coolantTemp = MutableStateFlow<Int?>(null)
    val coolantTemp: StateFlow<Int?> = _coolantTemp.asStateFlow()

    // Expose connection state directly from repository
    val connectionState: StateFlow<BluetoothState> = repository.connectionState

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()

    private var dataPollingJob: Job? = null

    init {
        // Update status text based on connection state changes
        viewModelScope.launch {
            repository.connectionState.collect { state ->
                updateStatusText(state)
                when (state) {
                    is BluetoothState.Connected -> startDataPolling()
                    is BluetoothState.Disconnected, is BluetoothState.Error -> stopDataPolling()
                    else -> {} // Do nothing for Connecting, Scanning, DevicesFound here
                }
            }
        }
    }

    private fun updateStatusText(state: BluetoothState) {
        _statusText.value = when (state) {
            is BluetoothState.Connected -> "Status: Connected to ${state.device.name}"
            is BluetoothState.Connecting -> "Status: Connecting to ${state.device.name}..."
            is BluetoothState.Disconnected -> "Status: Disconnected"
            is BluetoothState.Error -> "Status: Error - ${state.message}"
            is BluetoothState.Scanning -> "Status: Scanning..."
            is BluetoothState.DevicesFound -> "Status: Select a device"
        }
    }

    fun loadPairedDevices() {
        if (!PermissionUtils.hasBluetoothPermissions(context)) {
            _statusText.value = "Status: Bluetooth permissions needed"
            _pairedDevices.value = emptyList()
            return
        }
        if (!deviceScanner.isBluetoothEnabled()) {
            _statusText.value = "Status: Please enable Bluetooth"
            _pairedDevices.value = emptyList()
            return
        }
        _statusText.value = "Status: Loading paired devices..."
        _pairedDevices.value = deviceScanner.getPairedDevices()
        if (_pairedDevices.value.isEmpty()) {
            _statusText.value = "Status: No paired OBD devices found"
        } else {
            _statusText.value = "Status: Select a paired device"
        }
    }

    fun connectDevice(device: BluetoothDevice) {
        if (!PermissionUtils.hasBluetoothPermissions(context)) {
            _statusText.value = "Status: Bluetooth permissions needed"
            return
        }
        repository.connect(device)
    }

    fun disconnectDevice() {
        repository.disconnect()
        // State flow collection will handle stopping polling and updating status
    }

    private fun startDataPolling() {
        stopDataPolling() // Ensure only one polling job runs
        Log.d(TAG, "Starting data polling...")
        dataPollingJob = viewModelScope.launch {
            while (isActive && repository.connectionState.value is BluetoothState.Connected) {
                // Request data points sequentially with slight delay
                requestSinglePid { repository.requestRpm() }?.let { _rpm.value = it }
                delay(50) // Small delay between requests
                requestSinglePid { repository.requestSpeed() }?.let { _speed.value = it }
                delay(50)
                requestSinglePid { repository.requestCoolantTemp() }?.let { _coolantTemp.value = it }

                delay(Constants.POLLING_INTERVAL_MS) // Wait before next poll cycle
            }
            Log.d(TAG, "Data polling stopped.")
        }
    }

    // Helper to request a single PID and extract the value
    private suspend fun <T> requestSinglePid(requestAction: suspend () -> Flow<ObdResponse<T>>): T? {
        return try {
            requestAction().mapNotNull { response ->
                when (response) {
                    is ObdResponse.Success -> response.value
                    is ObdResponse.Error -> {
                        Log.w(TAG, "OBD Error for command ${response.command.command}: ${response.message}")
                        null
                    }
                    is ObdResponse.Timeout -> {
                        Log.w(TAG, "OBD Timeout for command ${response.command.command}")
                        null
                    }
                }
            }.firstOrNull() // Take the first successful result or null
        } catch (e: Exception) {
            Log.e(TAG, "Exception during PID request: ${e.message}")
            null
        }
    }


    private fun stopDataPolling() {
        if (dataPollingJob?.isActive == true) {
            Log.d(TAG, "Stopping data polling job.")
            dataPollingJob?.cancel()
        }
        dataPollingJob = null
        // Reset values when polling stops? Optional.
        // _rpm.value = null
        // _speed.value = null
        // _coolantTemp.value = null
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared, disconnecting.")
        repository.disconnect() // Ensure disconnection when ViewModel is destroyed
    }
}


// Factory class to provide dependencies to the ViewModel
class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                context.applicationContext,
                Injection.provideObdRepository(context),
                Injection.provideBluetoothDeviceScanner(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}