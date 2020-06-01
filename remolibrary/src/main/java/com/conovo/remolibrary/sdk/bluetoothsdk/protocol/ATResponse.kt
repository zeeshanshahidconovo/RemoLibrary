package com.remo.bluetoothtestapp.bluetoothsdk.protocol

@UseExperimental(ExperimentalUnsignedTypes::class)
internal data class ATResponse(
        var value: UShort,
        var ack: ACK
) {

    constructor() : this(0u, ACK.None)

    fun isOK(): Boolean = ack.isOK()

    fun isError(): Boolean = ack.isError()

    companion object {

        fun createFromReply(reply: List<Byte>): ATResponse =
                ATResponse().apply {
                    if (reply.size >= 3) {
                        if ((reply[reply.size - 2] == ATProtocol.TERMINATOR[0]) && (reply[reply.size - 1] == ATProtocol.TERMINATOR[1])) {
                            ack = ACK.parse(reply[0])

                            if (reply.size > 3) {
                                val data = reply.slice(1 until reply.size - 2)
                                val dataAsString = String(data.toByteArray())

                                value = if (dataAsString == "yes")
                                    1u
                                else
                                    dataAsString.toUShortOrNull() ?: 0u
                            }
                        }
                    }
                }
    }
}
