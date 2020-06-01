package com.remo.bluetoothtestapp.bluetoothsdk.protocol

@UseExperimental(ExperimentalUnsignedTypes::class)
 enum class AcquisitionMode(val value: UShort) {
    RAW(1u),
    RMS(2u),
    RAW_IMU(3u) // added 13-3-2020
}
