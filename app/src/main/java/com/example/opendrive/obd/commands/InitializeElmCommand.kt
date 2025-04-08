package com.example.opendrive.obd.commands

import com.example.opendrive.obd.ObdCommand
import com.example.opendrive.util.Constants

// Simple command for ELM327 initialization (AT commands)
// Expects "OK" or the command itself in the response for success
class InitializeElmCommand(command: String) : ObdCommand<Boolean>(command) {

    override fun parseResponse(response: String): Boolean? {
        // Basic check: Does the response contain "OK" or the command itself (for echo)?
        // More robust checks might be needed for specific AT commands.
        val cleanedResponse = response.replace("\\s".toRegex(), "").uppercase() // Remove whitespace
        return cleanedResponse.contains("OK") || cleanedResponse.contains(command.uppercase())
    }

    override val responseTimeoutMillis: Long = 3000L // Give longer for reset etc.
}