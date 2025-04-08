package com.example.opendrive.obd

// Base class for all OBD-II commands
abstract class ObdCommand<T>(val command: String) {
    // Abstract function to parse the raw response string into the desired type T
    // Returns null if parsing fails or response is invalid
    abstract fun parseResponse(response: String): T?

    // How long to potentially wait for a response (can be overridden)
    open val responseTimeoutMillis: Long = com.example.opendrive.util.Constants.OBD_TIMEOUT_MS
}