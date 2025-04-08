package com.example.opendrive.util

import java.util.UUID

object Constants {
    // Standard Serial Port Profile UUID
    val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    const val ELM_PROMPT = ">"
    const val OBD_TIMEOUT_MS = 1000L // Timeout for waiting for OBD response
    const val POLLING_INTERVAL_MS = 500L // How often to request data
}