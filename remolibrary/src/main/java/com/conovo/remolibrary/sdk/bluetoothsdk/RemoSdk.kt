package com.remo.bluetoothtestapp.bluetoothsdk

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.github.ivbaranov.rxbluetooth.RxBluetooth
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import com.remo.bluetoothtestapp.bluetoothsdk.impl.RemoSdkImpl
import com.remo.bluetoothtestapp.bluetoothsdk.model.BondState
import com.remo.bluetoothtestapp.bluetoothsdk.model.ConnectionState
import com.remo.bluetoothtestapp.bluetoothsdk.model.RemoConnection
import com.remo.bluetoothtestapp.bluetoothsdk.model.Sample
import com.remo.bluetoothtestapp.bluetoothsdk.protocol.AcquisitionMode
import com.remo.bluetoothtestapp.bluetoothsdk.util.RxBluetoothUtil

interface RemoSdk {

    companion object {

        private var inited = false

        fun init(context: Context) {
            if (inited)
                throw IllegalStateException("RemoSdk.init must be called once!") as Throwable
            inited = true
            RxBluetoothUtil.init(context.applicationContext)
        }

        fun init(client: RxBluetooth) {
            if (inited)
                throw IllegalStateException("RemoSdk.init must be called once!")
            inited = true
            RxBluetoothUtil.init(client)
        }

        fun getSdk(context: Context, factory: RemoSdkFactory = RemoSdkImpl.Factory): RemoSdk {
            if (inited.not())
                throw IllegalStateException("RemoSdk.getSdk called before init!")
            return factory.make(context.applicationContext, RxBluetoothUtil.getRxBluetooth())
        }
    }

    fun isBluetoothAvailable(): Boolean

    fun isBluetoothEnabled(): Boolean

    fun enableBluetooth(activity: Activity, enableBluetoothRequestCode: Int)

    fun observeBluetoothState(): Observable<Int>

    fun bondedDevices(): List<BluetoothDevice>

    fun observeBondState(): Observable<BondState>

    fun observeBondedDevices(): Observable<List<BluetoothDevice>>

    fun waitBondedDevices(): Single<List<BluetoothDevice>>

    fun observeDevices(): Observable<BluetoothDevice>

//    fun stopDataStream(connection: RemoConnection): Single<Boolean>

    fun bond(device: BluetoothDevice): Single<Boolean>

    fun observeConnectionState(): Observable<ConnectionState>

    fun connect(device: BluetoothDevice): Observable<RemoConnection>

    fun observeSamples(connection: RemoConnection, asq: AcquisitionMode): Flowable<Sample>

    fun startSessionAndObserveSamples(connection: RemoConnection): Flowable<Sample>
}
