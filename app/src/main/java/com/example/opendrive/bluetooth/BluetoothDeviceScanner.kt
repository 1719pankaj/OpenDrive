package com.example.opendrive.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.example.opendrive.util.PermissionUtils

// Handles scanning for paired Bluetooth devices
class BluetoothDeviceScanner(context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val appContext = context.applicationContext

    @SuppressLint("MissingPermission") // Permissions checked before calling
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!PermissionUtils.hasBluetoothPermissions(appContext)) {
            Log.e("DeviceScanner", "Bluetooth permissions not granted!")
            return emptyList()
        }
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e("DeviceScanner", "Bluetooth not enabled or available")
            return emptyList()
        }

        return try {
            bluetoothAdapter.bondedDevices.toList()
        } catch (e: SecurityException) {
            Log.e("DeviceScanner", "SecurityException getting bonded devices", e)
            emptyList()
        }
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true
}