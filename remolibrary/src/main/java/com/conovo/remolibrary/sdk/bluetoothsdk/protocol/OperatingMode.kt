package com.remo.bluetoothtestapp.bluetoothsdk.protocol

@UseExperimental(ExperimentalUnsignedTypes::class)
internal enum class OperatingMode(val value: UShort) {
    ClientConnected(1u),
    TxMode(2u),
}
