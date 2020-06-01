package com.remo.bluetoothtestapp.bluetoothsdk.protocol

internal class ATProtocol {

    companion object {
        const val CR: Byte = 13
        const val LF: Byte = 10
        val TERMINATOR: ByteArray = byteArrayOf(CR, LF)
    }
}
