package com.remo.bluetoothtestapp.bluetoothsdk.protocol

internal enum class ACK(val value: Byte) {
    None(0),
    Ok('O'.toByte()),
    Error('E'.toByte()),
    ;

    fun isOK(): Boolean = this == ACK.Ok

    fun isError(): Boolean = this == ACK.Error

    companion object {

        fun parse(value: Byte): ACK {
            for (ack in ACK.values())
                if (value == ack.value)
                    return ack
            return ACK.None
        }
    }
}
