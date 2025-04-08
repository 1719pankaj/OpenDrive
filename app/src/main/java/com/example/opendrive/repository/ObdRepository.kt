package com.example.opendrive.repository

import android.bluetooth.BluetoothDevice
import com.example.opendrive.bluetooth.BluetoothState
import com.example.opendrive.obd.ObdCommand
import com.example.opendrive.obd.ObdResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

// Interface for accessing OBD data and connection state
interface ObdRepository {
    val connectionState: StateFlow<BluetoothState>

    fun connect(device: BluetoothDevice)
    fun disconnect()

    // Function to execute a command and get a flow of responses for it
    // This allows specific handling for each command if needed.
    suspend fun <T> executeCommand(command: ObdCommand<T>): Flow<ObdResponse<T>>

    // Simplified functions for common PIDs (optional convenience)
    suspend fun requestRpm(): Flow<ObdResponse<Int>>
    suspend fun requestSpeed(): Flow<ObdResponse<Int>>
    suspend fun requestCoolantTemp(): Flow<ObdResponse<Int>>
}