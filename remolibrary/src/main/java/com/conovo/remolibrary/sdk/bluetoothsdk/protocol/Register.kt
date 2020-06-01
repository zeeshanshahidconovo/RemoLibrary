package com.remo.bluetoothtestapp.bluetoothsdk.protocol

internal enum class Register(val value: Byte) {
    AcquisitionMode('1'.toByte()),
    OperatingMode('2'.toByte()),
    MotorMode('3'.toByte()) // added 13-3-2020
}
