package com.remo.bluetoothtestapp.bluetoothsdk.model

data class Vec3(
        var x: Float,
        var y: Float,
        var z: Float
) {

    constructor() : this(0f, 0f, 0f)

    fun toCSV(): String = "$x,$y,$z"

    operator fun get(index: Int): Float =
            when (index) {
                0 -> x
                1 -> y
                2 -> z
                else -> throw IndexOutOfBoundsException("Received index $index. Expected one in [0, 1, 2]")
            }

    operator fun set(index: Int, value: Float): Unit =
            when (index) {
                0 -> x = value
                1 -> y = value
                2 -> z = value
                else -> throw IndexOutOfBoundsException("Received index $index. Expected one in [0, 1, 2]")
            }
}
