markdown
# OpenDrive Hardware Interface Notes

## 1. Introduction

This document details the hardware interface targeted by the OpenDrive application, focusing on the interaction between the Android device and the vehicle's On-Board Diagnostics (OBD-II) system via an ELM327 adapter.

## 2. Target Interface: ELM327 OBD-II Adapters (Bluetooth)

OpenDrive is specifically designed to communicate with **ELM327-based OBD-II adapters** that utilize **Bluetooth Classic** for wireless communication.

*   **ELM327 Chip:** This microcontroller acts as a bridge between the vehicle's diagnostic bus protocols (CAN, KWP2000, etc.) and a standard serial interface (emulated over Bluetooth).
*   **Bluetooth:** Provides the wireless link between the adapter plugged into the vehicle and the Android application.

*Note: Adapters using Wi-Fi or USB interfaces are currently **not** supported.*

## 3. Physical Connector: OBD-II Port (SAE J1962)

*   The ELM327 adapter plugs into the vehicle's standard **OBD-II diagnostic port**.
*   This is typically a 16-pin female connector located under the dashboard near the steering column (location varies by vehicle manufacturer and model).
*   The adapter draws power directly from this port (Pins 16 for +12V, Pins 4/5 for Ground).

## 4. Communication Layers

### 4.1. Android Device <-> ELM327 Adapter

*   **Protocol:** Bluetooth Classic
*   **Profile:** **Serial Port Profile (SPP)**. This profile emulates a standard RS-232 serial cable connection over Bluetooth.
*   **Standard SPP UUID:** `00001101-0000-1000-8000-00805F9B34FB` (Used by the Android app to establish the connection).
*   **Data Format:** Bidirectional ASCII text streams. The Android app sends commands as text strings, and the ELM327 responds with text strings.

### 4.2. ELM327 Adapter <-> Vehicle ECU

*   **Protocols:** The ELM327 chip automatically detects and handles various OBD-II communication protocols used by vehicle ECUs (Engine Control Units). Common examples include:
    *   ISO 15765-4 (CAN Bus - 11-bit/29-bit ID, 250/500 kbaud)
    *   ISO 14230-4 (KWP2000 - Keyword Protocol 2000)
    *   ISO 9141-2
    *   SAE J1850 (PWM and VPW)
*   **Abstraction:** OpenDrive does **not** directly interact with these low-level vehicle protocols. It sends standardized commands *to the ELM327*, which then translates them into the appropriate protocol for the connected vehicle. The `ATSP0` command typically tells the ELM327 to auto-detect the protocol.

## 5. ELM327 Command Set

Communication with the ELM327 involves two main types of commands sent from the Android app:

### 5.1. AT Commands (Hayes Command Set)

*   Used for configuring the ELM327 adapter itself.
*   Sent as ASCII strings starting with "AT".
*   **Essential Initialization Commands:**
    *   `ATZ`: Reset the ELM327 chip.
    *   `ATE0`: Echo Off (Prevents the adapter from echoing back sent commands).
    *   `ATL0`: Linefeeds Off (Simplifies response parsing).
    *   `ATH0`/`ATH1`: Headers On/Off (Controls whether CAN bus header information is included in responses). Usually Off (`ATH0`) for basic PID reading.
    *   `ATS0`/`ATS1`: Spaces On/Off (Controls formatting of responses). Usually Off (`ATS0`).
    *   `ATSP0`: Set Protocol to Automatic. Tells the ELM327 to try all OBD-II protocols until one works with the vehicle. Specific protocols can also be set (e.g., `ATSP6` for CAN 500kbaud/11-bit).
    *   `ATDP`: Display Protocol (Shows the currently active OBD-II protocol).
*   **Response:** Usually "OK >" or specific information followed by ">". The `>` character acts as the prompt indicating the adapter is ready for the next command.

### 5.2. OBD-II Commands (Hexadecimal)

*   Used to request specific data or actions from the vehicle's ECU(s).
*   Sent as ASCII strings representing hexadecimal values.
*   **Format:** Generally `[Mode][PID]`
    *   **Mode:** Specifies the type of data being requested (e.g., `01` for Current Powertrain Diagnostic Data, `03` for Request Emission-Related DTCs).
    *   **PID:** Parameter ID, specifies the exact data point (e.g., `0C` for Engine RPM, `0D` for Vehicle Speed).
*   **Examples:**
    *   `010C`: Request Engine RPM.
    *   `010D`: Request Vehicle Speed.
    *   `03`: Request stored Diagnostic Trouble Codes.
*   **Response Format:** Typically `[Mode+0x40][PID] [Data Bytes] >`
    *   Example Response to `010C`: `41 0C 0A 3B >`
        *   `41`: Mode 01 response identifier (01 + 40 hex).
        *   `0C`: The requested PID echoed back.
        *   `0A 3B`: The actual data bytes (Hexadecimal). These need to be parsed according to the PID's formula.
        *   `>`: Prompt.

## 6. Data Parsing

*   Responses from the ELM327 are ASCII strings.
*   Relevant data is usually embedded as hexadecimal values within the response string.
*   The application needs to:
    1.  Read the complete response string (often ending with `>`).
    2.  Validate the response format (check for the correct mode/PID echo).
    3.  Extract the relevant hexadecimal data bytes.
    4.  Convert the hex bytes to decimal values.
    5.  Apply the specific formula defined by the OBD-II standard for that PID to get the meaningful unit (e.g., RPM formula for `010C`: `((A * 256) + B) / 4`, where A and B are the first and second data bytes).

## 7. Hardware Considerations & Limitations

*   **Adapter Quality:** The market is flooded with inexpensive ELM327 clones of varying quality. Poor quality adapters can lead to:
    *   Slow response times.
    *   Dropped Bluetooth connections.
    *   Inaccurate data reporting.
    *   Inability to connect to certain vehicles or protocols.
    *   Freezing or requiring power cycling.
*   **Power Consumption:** Adapters draw power continuously from the OBD-II port when plugged in, even when the vehicle is off. This can potentially drain the vehicle battery over extended periods if left plugged in.
*   **Bluetooth Stability:** Bluetooth communication can be affected by interference or range limitations. Robust error handling in the app is necessary.
*   **Vehicle Compatibility:** While OBD-II is standardized, some manufacturers implement specific PIDs or have quirks that may not be handled by all adapters or generic software.

## 8. Future Interface Support

Currently, only Bluetooth Classic SPP is supported. Future versions *could* explore support for:

*   ELM327 Wi-Fi adapters.
*   ELM327 USB adapters (requires Android USB Host support).
*(These are low priority compared to enhancing core Bluetooth functionality).*