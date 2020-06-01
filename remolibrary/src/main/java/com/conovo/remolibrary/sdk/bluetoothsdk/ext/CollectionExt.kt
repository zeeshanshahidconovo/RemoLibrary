package com.remo.bluetoothtestapp.bluetoothsdk.ext

fun <T> Sequence<List<T>>.extractColumn(index: Int): Sequence<T>
        = com.remo.bluetoothtestapp.bluetoothsdk.util.extractColumn(this, index)

fun <T> Sequence<List<T>>.transpose(): Sequence<Sequence<T>>
        = com.remo.bluetoothtestapp.bluetoothsdk.util.transpose(this)
