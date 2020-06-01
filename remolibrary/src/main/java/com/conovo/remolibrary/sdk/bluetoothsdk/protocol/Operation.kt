package com.remo.bluetoothtestapp.bluetoothsdk.protocol

internal enum class Operation(val value: Byte) {
    Write('='.toByte()),
    Read('?'.toByte()),
    Help('&'.toByte()),
}
