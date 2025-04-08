package com.example.opendrive.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.opendrive.R
import com.example.opendrive.bluetooth.BluetoothState
import com.example.opendrive.databinding.ActivityMainBinding // Import ViewBinding class
import com.example.opendrive.util.PermissionUtils
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission") // Permissions are checked before use
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding // ViewBinding instance
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(this)
    }

    // Activity Result Launcher for Permissions
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.entries.all { it.value }) {
                // All permissions granted
                viewModel.loadPairedDevices() // Try loading devices again
            } else {
                // Permission denied
                Toast.makeText(this, "Bluetooth permissions are required to connect.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater) // Inflate layout using ViewBinding
        setContentView(binding.root) // Set content view to the binding's root

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.buttonConnect.setOnClickListener {
            handleConnectButtonClick()
        }
        // Other UI setup if needed
    }

    private fun handleConnectButtonClick() {
        when (viewModel.connectionState.value) {
            is BluetoothState.Connected, is BluetoothState.Connecting -> {
                viewModel.disconnectDevice()
            }
            is BluetoothState.Disconnected, is BluetoothState.Error -> {
                if (PermissionUtils.hasBluetoothPermissions(this)) {
                    viewModel.loadPairedDevices() // Trigger loading devices to show dialog
                } else {
                    requestBluetoothPermissions()
                }
            }
            else -> { /* Do nothing for Scanning/DevicesFound states triggered internally */ }
        }
    }


    private fun observeViewModel() {
        // Observe connection state and update UI elements accordingly
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.connectionState.collect { state ->
                        binding.buttonConnect.text = when (state) {
                            is BluetoothState.Connected -> "Disconnect from ${state.device.name}"
                            is BluetoothState.Connecting -> "Connecting..."
                            else -> "Scan & Connect"
                        }
                        binding.buttonConnect.isEnabled = state !is BluetoothState.Connecting // Disable during connection attempt
                    }
                }

                launch {
                    viewModel.statusText.collect { status ->
                        binding.textStatus.text = status
                    }
                }

                launch {
                    viewModel.rpm.collect { rpmValue ->
                        binding.textRpmValue.text = rpmValue?.toString() ?: "---"
                    }
                }

                launch {
                    viewModel.speed.collect { speedValue ->
                        binding.textSpeedValue.text = speedValue?.toString() ?: "---"
                    }
                }
                launch {
                    viewModel.coolantTemp.collect { tempValue ->
                        binding.textTempValue.text = tempValue?.toString() ?: "---"
                    }
                }

                // Observe paired devices to show selection dialog
                launch {
                    viewModel.pairedDevices.collect { devices ->
                        // Only show dialog if we are in a disconnected state and devices are loaded
                        if (devices.isNotEmpty() && viewModel.connectionState.value is BluetoothState.Disconnected) {
                            showDeviceSelectionDialog(devices)
                        }
                    }
                }
            }
        }
    }

    private fun requestBluetoothPermissions() {
        requestPermissionLauncher.launch(PermissionUtils.bluetoothPermissions)
    }

    private fun showDeviceSelectionDialog(devices: List<BluetoothDevice>) {
        val deviceNames = devices.map { it.name ?: it.address } // Use address if name is null
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNames)

        AlertDialog.Builder(this)
            .setTitle("Select Paired OBD Device")
            .setAdapter(adapter) { dialog, which ->
                val selectedDevice = devices[which]
                viewModel.connectDevice(selectedDevice)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                viewModel.disconnectDevice() // Ensure state goes back to disconnected if cancelled
            }
            .setOnCancelListener {
                viewModel.disconnectDevice() // Ensure state goes back to disconnected if dialog is cancelled
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // ViewModel's onCleared handles disconnection, no explicit call needed here
        // unless there's cleanup specific to the Activity lifecycle.
    }
}