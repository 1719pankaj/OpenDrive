package com.example.opendrive.di

import android.content.Context
import com.example.opendrive.bluetooth.BluetoothConnectionManager
import com.example.opendrive.bluetooth.BluetoothDeviceScanner
import com.example.opendrive.repository.ObdRepository
import com.example.opendrive.repository.ObdRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

// Simple manual dependency injection setup
object Injection {

    private var repositoryInstance: ObdRepository? = null
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Singleton pattern for the repository
    fun provideObdRepository(context: Context): ObdRepository {
        return repositoryInstance ?: synchronized(this) {
            repositoryInstance ?: createObdRepository(context.applicationContext).also {
                repositoryInstance = it
            }
        }
    }

    private fun createObdRepository(appContext: Context): ObdRepository {
        // ConnectionManager could also be a singleton if needed across app features
        val connectionManager = BluetoothConnectionManager()
        return ObdRepositoryImpl(connectionManager, applicationScope)
    }

    // Provide scanner separately if needed directly in UI (though better via ViewModel)
    fun provideBluetoothDeviceScanner(context: Context): BluetoothDeviceScanner {
        return BluetoothDeviceScanner(context.applicationContext)
    }
}