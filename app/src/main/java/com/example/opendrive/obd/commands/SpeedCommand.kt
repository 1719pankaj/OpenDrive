package com.example.opendrive.obd.commands

import com.example.opendrive.obd.ObdCommand

// Command to get Vehicle Speed (PID 0D)
class SpeedCommand : ObdCommand<Int>("010D") {

    override fun parseResponse(response: String): Int? {
        // Expected response format like "41 0D AA" (AA is the data byte)
        val cleaned = response.replace("\\s".toRegex(), "").uppercase()
        // Basic validation: starts with 410D and has at least 2 hex digits after
        if (!cleaned.startsWith("410D") || cleaned.length < 6) {
            return null
        }
        return try {
            cleaned.substring(4, 6).toInt(16) // Formula: A (result is in km/h)
        } catch (e: NumberFormatException) {
            null
        } catch (e: IndexOutOfBoundsException) {
            null
        }
    }
}