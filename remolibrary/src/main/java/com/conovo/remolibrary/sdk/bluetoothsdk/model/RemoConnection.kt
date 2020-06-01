package com.remo.bluetoothtestapp.bluetoothsdk.model

import android.bluetooth.BluetoothDevice
import com.github.ivbaranov.rxbluetooth.BluetoothConnection

data class RemoConnection(
        val device: BluetoothDevice,
        internal val connection: BluetoothConnection
)
