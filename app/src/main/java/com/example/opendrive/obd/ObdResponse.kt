package com.example.opendrive.obd

// Wrapper for OBD command results, including potential errors or raw data
sealed class ObdResponse<T> {
    data class Success<T>(val value: T, val command: ObdCommand<T>, val rawResponse: String) : ObdResponse<T>()
    data class Error<T>(val message: String, val command: ObdCommand<T>, val rawResponse: String? = null) : ObdResponse<T>()
    data class Timeout<T>(val command: ObdCommand<T>) : ObdResponse<T>()
    // Could add more states like NoData, etc.
}