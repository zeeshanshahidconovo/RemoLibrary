package com.remo.bluetoothtestapp.bluetoothsdk.protocol

@UseExperimental(ExperimentalUnsignedTypes::class)
internal data class ATCommand(
        val register: Register,
        val operation: Operation,
        val value: UShort
) {

    fun getBytes(): ByteArray =
            getBytes(value.toString().toByteArray())

    fun getBytes(data: ByteArray): ByteArray =
            byteArrayOf(
                    'A'.toByte(),
                    'T'.toByte(),
                    'S'.toByte(),
                    register.value,
                    operation.value,
                    *data,
                    *ATProtocol.TERMINATOR
            )
}
