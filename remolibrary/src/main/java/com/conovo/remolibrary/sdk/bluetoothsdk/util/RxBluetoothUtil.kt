package com.remo.bluetoothtestapp.bluetoothsdk.util

import android.content.Context
import com.github.ivbaranov.rxbluetooth.RxBluetooth
import java.util.concurrent.atomic.AtomicInteger

internal class RxBluetoothUtil {

    companion object {

        private var rxBluetooth: RxBluetooth? = null

        private val discoveryCount = AtomicInteger(0)

        fun isInited() = rxBluetooth != null

        fun init(context: Context) {
            if (isInited())
                throw IllegalStateException("RxBluetoothUtil.init must be called once!")
            rxBluetooth = RxBluetooth(context.applicationContext)
        }

        fun init(client: RxBluetooth) {
            if (isInited())
                throw IllegalStateException("RxBluetoothUtil.init must be called once!")
            rxBluetooth = client
        }

        fun getRxBluetooth(): RxBluetooth {
            if (isInited().not())
                throw IllegalStateException("RxBluetoothUtil.getRxBluetooth called before init!")
            return rxBluetooth!!
        }

        fun startDiscovery(): Boolean {
            if (isInited().not())
                throw IllegalStateException("RxBluetoothUtil.startDiscovery called before init!")

            val prev = discoveryCount.getAndIncrement()

            return if (prev == 0)
                rxBluetooth!!.startDiscovery()
            else
                rxBluetooth!!.isDiscovering
        }

        fun cancelDiscovery(): Boolean {
            if (isInited().not())
                throw IllegalStateException("RxBluetoothUtil.cancelDiscovery called before init!")

            var prev: Int
            var next: Int
            do {
                prev = discoveryCount.get()
                next = if (prev > 0) prev - 1 else 0
            } while (!discoveryCount.compareAndSet(prev, next))

            return if (next == 0)
                rxBluetooth!!.cancelDiscovery()
            else
                rxBluetooth!!.isDiscovering.not()
        }
    }
}
