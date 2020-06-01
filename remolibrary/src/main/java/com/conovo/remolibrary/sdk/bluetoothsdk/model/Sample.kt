package com.remo.bluetoothtestapp.bluetoothsdk.model

import com.remo.bluetoothtestapp.bluetoothsdk.ext.toCSV
import com.remo.bluetoothtestapp.bluetoothsdk.protocol.AcquisitionMode
import kotlin.experimental.xor

@UseExperimental(ExperimentalUnsignedTypes::class)
data class Sample(
        var validPacket: Boolean,
        var validSample: Boolean,
        var date: Long,
        var measure: List<Short>,
        var counter: UShort,
        var acc: Vec3,
        var gyro: Vec3,
        var mag: Vec3
) {

    constructor() : this(false, false, 0, emptyList<Short>(), 0u, Vec3(), Vec3(), Vec3())

    fun toCSV(): String = "$date,$counter,${measure.toCSV()},${acc.toCSV()},${gyro.toCSV()},${mag.toCSV()}"

    internal companion object {
        var PACKET_LEN = 41 // const val and 39 .. removed, 13-3-2020
        var PACKET_DATA_LEN = PACKET_LEN - 7 // const val and -5 removed, 13-3-2020
        var MULT = 1 // added 13-3-2020
        var SIZE = 1 // added 13-3-2020

        const val FIRST_BYTE = '%'.toByte()
        const val LAST_BYTE = '&'.toByte()

        const val ACC_MAX_VALUE = 3
        const val ACC_MIN_VALUE = -3

        const val GYRO_MAX_VALUE = 100
        const val GYRO_MIN_VALUE = -100

        const val MAG_MAX_VALUE = 2
        const val MAG_MIN_VALUE = -2

        fun crc(bytes: List<Byte>): Byte =
                bytes.reduceIndexed { index, acc, byte ->
                    if (index != bytes.size - 2)
                        acc xor byte
                    else
                        acc
                }

        fun isValidPacketNew(bytes: List<Byte>, asq: AcquisitionMode): Boolean {
            var flag = false

           if (asq == AcquisitionMode.RAW){
               PACKET_LEN = 40
               MULT = 23

           }else{
               PACKET_LEN = 41
               MULT = 1

           }
            SIZE = PACKET_LEN * MULT
            PACKET_DATA_LEN = PACKET_LEN - 7

            if (bytes.size == PACKET_LEN &&
                    (bytes.first() == FIRST_BYTE) &&
                    (bytes.last() == LAST_BYTE) && crc(bytes) == bytes[bytes.size - 2]) flag = true

            return flag
        }

//        // just to check validity ... old...
//        fun isValidPacket(bytes: List<Byte>): Boolean =
//                (bytes.size == PACKET_LEN) &&
//                        (bytes.first() == FIRST_BYTE) &&
//                        (bytes.last() == LAST_BYTE) &&
//                        (crc(bytes) == bytes[bytes.size - 2]) // ==>> CHECK THIS <<==
//
//        // function below plots data in Sample
//        fun fromPacket(packet: List<Byte>): Sample =
//                Sample().apply {
//                    date = System.currentTimeMillis()
//
//                    if (isValidPacketNew(packet)) { // added new 13-3-2020 (ignore.. not called)
//                        validPacket = true
//
//                        var firstIndex = 0
//                        var lastIndex = packet.lastIndex
//
//                        ++firstIndex
//                        --lastIndex
//                        --lastIndex
//
//                        counter = packet[firstIndex + 0].toUByte().toUInt().shl(8).or(packet[firstIndex + 1].toUByte().toUInt()).toUShort()
//                        ++firstIndex
//                        ++firstIndex
//
//                        val packetDataLen = lastIndex - firstIndex + 1
//                        if (packetDataLen == PACKET_DATA_LEN) {
//                            validSample = true
//
//                            val sampleMeasures = ShortArray(8)
//                            for (i in 0 until 8)
//                                sampleMeasures[i] = packet[firstIndex + (i * 2)].toUByte().toUInt().shl(8).or(packet[firstIndex + (i * 2) + 1].toUByte().toUInt()).toShort()
//                            measure = sampleMeasures.asList()
//
//                            firstIndex += 16
//
//                            for (i in 0 until 3)
//                                acc[i] = packet[firstIndex + (i * 2)].toUByte().toUInt().shl(8).or(packet[firstIndex + (i * 2) + 1].toUByte().toUInt()).toShort().toFloat() / 100
//
//                            firstIndex += 6
//
//                            for (i in 0 until 3)
//                                gyro[i] = packet[firstIndex + (i * 2)].toUByte().toUInt().shl(8).or(packet[firstIndex + (i * 2) + 1].toUByte().toUInt()).toShort().toFloat() / 100
//
//                            firstIndex += 6
//
//                            for (i in 0 until 3)
//                                mag[i] = packet[firstIndex + (i * 2)].toUByte().toUInt().shl(8).or(packet[firstIndex + (i * 2) + 1].toUByte().toUInt()).toShort().toFloat() / 100
//
//                            firstIndex += 6
//                        }
//                    }
//                }


        // new
        fun fromPacketNewFunc(packet: List<Byte>, asq: AcquisitionMode): Sample =
                Sample().apply {
                    date = System.currentTimeMillis()

                    if (isValidPacketNew(packet, asq)) { // added new 13-3-2020
                        validPacket = true

                        var firstIndex = 0
                        var lastIndex = packet.lastIndex

                        ++firstIndex
                        --lastIndex
                        --lastIndex

                        counter = packet[firstIndex + 0].toUByte().toUInt().shl(8).or(packet[firstIndex + 1].toUByte().toUInt()).toUShort()
                        ++firstIndex
                        ++firstIndex
                        ++firstIndex
                        ++firstIndex
//                        --lastIndex
//                        --lastIndex
//                        --lastIndex
//                        --lastIndex


                        val packetDataLen = lastIndex - firstIndex + 1
                        if (packetDataLen == PACKET_DATA_LEN) {
                            validSample = true

                            val sampleMeasures = ShortArray(8)
                            for (i in 0 until 8)
                                sampleMeasures[i] = packet[firstIndex + (i * 2)].toUByte().toUInt().shl(8).or(packet[firstIndex + (i * 2) + 1].toUByte().toUInt()).toShort()
                            measure = sampleMeasures.asList()

                            firstIndex += 16

                            for (i in 0 until 3)
                                acc[i] = packet[firstIndex + (i * 2)].toUByte().toUInt().shl(8).or(packet[firstIndex + (i * 2) + 1].toUByte().toUInt()).toShort().toFloat() / 100

                            firstIndex += 6

                            for (i in 0 until 3)
                                gyro[i] = packet[firstIndex + (i * 2)].toUByte().toUInt().shl(8).or(packet[firstIndex + (i * 2) + 1].toUByte().toUInt()).toShort().toFloat() / 100

                            firstIndex += 6

                            for (i in 0 until 3)
                                mag[i] = packet[firstIndex + (i * 2)].toUByte().toUInt().shl(8).or(packet[firstIndex + (i * 2) + 1].toUByte().toUInt()).toShort().toFloat() / 100

                            firstIndex += 6
                        }
                    }
                }
    }
}
