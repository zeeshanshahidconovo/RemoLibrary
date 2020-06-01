package com.remo.bluetoothtestapp.bluetoothsdk.util

fun <T> extractColumn(rows: Sequence<List<T>>, index: Int): Sequence<T> =
        sequence {
            rows.takeWhile { row ->
                index < row.size
            }.forEach { row ->
                yield(row[index])
            }
        }

fun <T> transpose(rows: Sequence<List<T>>): Sequence<Sequence<T>> =
        generateSequence(0) { index -> index + 1 }
                .map { index -> extractColumn(rows, index) }
                .takeWhile { column ->
                    column.any()
                }
