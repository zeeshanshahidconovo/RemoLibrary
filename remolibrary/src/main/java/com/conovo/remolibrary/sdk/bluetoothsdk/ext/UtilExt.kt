package com.remo.bluetoothtestapp.bluetoothsdk.ext

fun <T> Iterable<T>.toCSV(): String = joinToString(",", "", "")

fun CharSequence.fromCSV(): List<String> = split(",")
