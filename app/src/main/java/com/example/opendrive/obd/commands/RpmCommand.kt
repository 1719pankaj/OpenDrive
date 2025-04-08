package com.example.opendrive.obd.commands

import com.example.opendrive.obd.ObdCommand
import kotlin.math.roundToInt

// Command to get Engine RPM (PID 0C)
class RpmCommand : ObdCommand<Int>("010C") {

    override fun parseResponse(response: String): Int? {
        // Expected response format like "41 0C AA BB" (AA BB are the data bytes)
        val cleaned = response.replace("\\s".toRegex(), "").uppercase() // Remove spaces, uppercase
        // Basic validation: starts with 410C and has at least 4 hex digits after
        if (!cleaned.startsWith("410C") || cleaned.length < 8) {
            return null
        }
        return try {
            val a = cleaned.substring(4, 6).toInt(16) // Get first byte (AA)
            val b = cleaned.substring(6, 8).toInt(16) // Get second byte (BB)
            ((a * 256) + b) / 4 // Formula: ((A*256)+B)/4
        } catch (e: NumberFormatException) {
            null // Invalid hex characters
        } catch (e: IndexOutOfBoundsException) {
            null // Response too short
        }
    }
}