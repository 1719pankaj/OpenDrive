package com.example.opendrive.bluetooth

import android.bluetooth.BluetoothDevice

// Represents the state of the Bluetooth connection
sealed class BluetoothState {
    data object Disconnected : BluetoothState()
    data object Scanning : BluetoothState()
    data class DevicesFound(val devices: List<BluetoothDevice>) : BluetoothState()
    data class Connecting(val device: BluetoothDevice) : BluetoothState()
    data class Connected(val device: BluetoothDevice) : BluetoothState()
    data class Error(val message: String, val device: BluetoothDevice? = null) : BluetoothState()
}