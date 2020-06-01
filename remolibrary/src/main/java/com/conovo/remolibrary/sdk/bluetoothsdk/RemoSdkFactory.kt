package com.remo.bluetoothtestapp.bluetoothsdk

import android.content.Context
import com.github.ivbaranov.rxbluetooth.RxBluetooth

interface RemoSdkFactory {
    fun make(context: Context, rxBluetooth: RxBluetooth): RemoSdk
}
