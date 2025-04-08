package com.example.opendrive.obd.commands

import com.example.opendrive.obd.ObdCommand

// Command to get Engine Coolant Temperature (PID 05)
class EngineCoolantTempCommand : ObdCommand<Int>("0105") {

    override fun parseResponse(response: String): Int? {
        // Expected response format like "41 05 AA" (AA is the data byte)
        val cleaned = response.replace("\\s".toRegex(), "").uppercase()
        // Basic validation: starts with 4105 and has at least 2 hex digits after
        if (!cleaned.startsWith("4105") || cleaned.length < 6) {
            return null
        }
        return try {
            val a = cleaned.substring(4, 6).toInt(16)
            a - 40 // Formula: A - 40 (result is in degrees Celsius)
        } catch (e: NumberFormatException) {
            null
        } catch (e: IndexOutOfBoundsException) {
            null
        }
    }
}