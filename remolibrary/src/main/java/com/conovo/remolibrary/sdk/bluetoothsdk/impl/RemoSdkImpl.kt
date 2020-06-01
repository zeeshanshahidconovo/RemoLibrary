package com.remo.bluetoothtestapp.bluetoothsdk.impl

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import com.github.ivbaranov.rxbluetooth.BluetoothConnection
import com.github.ivbaranov.rxbluetooth.RxBluetooth
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import com.remo.bluetoothtestapp.bluetoothsdk.RemoSdk
import com.remo.bluetoothtestapp.bluetoothsdk.RemoSdkFactory
import com.remo.bluetoothtestapp.bluetoothsdk.exception.AcquisitionModeException
import com.remo.bluetoothtestapp.bluetoothsdk.exception.OperatingModeException
import com.remo.bluetoothtestapp.bluetoothsdk.exception.WriteException
import com.remo.bluetoothtestapp.bluetoothsdk.ext.*
import com.remo.bluetoothtestapp.bluetoothsdk.model.BondState
import com.remo.bluetoothtestapp.bluetoothsdk.model.ConnectionState
import com.remo.bluetoothtestapp.bluetoothsdk.model.RemoConnection
import com.remo.bluetoothtestapp.bluetoothsdk.model.Sample
import com.remo.bluetoothtestapp.bluetoothsdk.protocol.*
import com.remo.bluetoothtestapp.bluetoothsdk.util.RxBluetoothUtil
import java.io.IOException
import java.util.*

internal class RemoSdkImpl private constructor(
        private val context: Context,
        private val rxBluetooth: RxBluetooth
) : RemoSdk {

    object Factory : RemoSdkFactory {
        override fun make(context: Context, rxBluetooth: RxBluetooth): RemoSdk {
            return RemoSdkImpl(context.applicationContext, rxBluetooth)
        }
    }

    private object Constant {
        const val NAME_FILTER = "MORE"
        const val NAME_FILTER_REGEX = "$NAME_FILTER\\d+"

        const val SERVICE_UUID = "00001101-0000-1000-8000-00805f9b34fb"
    }

    override fun isBluetoothAvailable(): Boolean =
            rxBluetooth.isBluetoothAvailable

    override fun isBluetoothEnabled(): Boolean =
            rxBluetooth.isBluetoothEnabled

    override fun enableBluetooth(activity: Activity, enableBluetoothRequestCode: Int) =
            rxBluetooth.enableBluetooth(activity, enableBluetoothRequestCode)

    override fun observeBluetoothState(): Observable<Int> =
            rxBluetooth.observeBluetoothState()
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .orEmpty()
                    .filterNotNull()

    override fun bondedDevices(): List<BluetoothDevice> =
            rxBluetooth.bondedDevices
                    .orEmpty()
                    .filterNotNull()
                    .filter { device ->
                        Regex(Constant.NAME_FILTER_REGEX).matches(device.name)
                    }
                    .sortedBy { device ->
                        device.name
                    }

    override fun observeBondState(): Observable<BondState> =
            rxBluetooth.observeBondState()
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .orEmpty()
                    .filterNotNull()
                    .filter { state ->
                        Regex(Constant.NAME_FILTER_REGEX).matches(state.bluetoothDevice.name)
                    }
                    .map { state ->
                        BondState(state)
                    }

    override fun observeBondedDevices(): Observable<List<BluetoothDevice>> =
            observeBondState()
                    .map {
                        bondedDevices()
                    }
                    .startWith(Observable.defer {
                        Observable.just(bondedDevices())
                    })
                    .distinctUntilChanged { t1, t2 ->
                        (t1.size == t2.size) && t1.zip(t2).all { pair ->
                            (pair.first.name == pair.second.name) && (pair.first.address == pair.second.address) && (pair.first.bondState == pair.second.bondState)
                        }
                    }

    override fun waitBondedDevices(): Single<List<BluetoothDevice>> =
            observeBluetoothState()
                    .firstOrError { state ->
                        state == BluetoothAdapter.STATE_ON
                    }
                    .map {
                        bondedDevices()
                    }

    private fun observeDevicesBlindly(): Observable<BluetoothDevice> =
            rxBluetooth.observeDevices()
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .filter { device ->
                        device.name != null && Regex(Constant.NAME_FILTER_REGEX).matches(device.name)
                    }

    override fun observeDevices(): Observable<BluetoothDevice> =
            Observable.using({
                RxBluetoothUtil.startDiscovery()
            }, { isDiscovering ->
                if (isDiscovering)
                    observeDevicesBlindly()
                else
                    Observable.empty()
            }, {
                RxBluetoothUtil.cancelDiscovery()
            })

    override fun bond(device: BluetoothDevice): Single<Boolean> {
        if (device.bondState == BluetoothDevice.BOND_BONDED)
            return Single.just(true)

        if (device.bondState == BluetoothDevice.BOND_NONE) {
            val bondingStarted = device.createBond()
            if (!bondingStarted)
                return Single.just(false)
        }

        return observeBondState()
                .filter { state ->
                    device.address == state.device.address
                }
                .map { state ->
                    state.previousState to state.state
                }
                .startWith(Observable.defer {
                    Observable.just(BondState.State.None to BondState.State.parse(device.bondState))
                })
                .firstOrError { pair ->
                    ((pair.first != BondState.State.None) && (pair.second == BondState.State.None)) || (pair.second == BondState.State.Bonded)
                }
                .map { pair ->
                    pair.second == BondState.State.Bonded
                }
    }

    override fun observeConnectionState(): Observable<ConnectionState> =
            rxBluetooth.observeConnectionState()
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .orEmpty()
                    .filterNotNull()
                    .filter { state ->
                        Regex(Constant.NAME_FILTER_REGEX).matches(state.bluetoothDevice.name)
                    }
                    .map { state ->
                        ConnectionState(state)
                    }

//    override fun stopDataStream(connection: RemoConnection): Single<Boolean> {
//        connection.cl
//    }

    override fun connect(device: BluetoothDevice): Observable<RemoConnection> =
            connectWorkaround(device, UUID.fromString(Constant.SERVICE_UUID))
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation())
                    .flatMapObservable { socket ->
                        Observable.using({
                            BluetoothConnection(socket)
                        }, { connection ->
                            Observable.create<RemoConnection> { emitter ->
                                emitter.onNext(RemoConnection(device, connection))
                            }
                        }, { connection ->
                            connection.closeConnection()
                        })
                    }

//    override fun disconnect(device: BluetoothDevice): Observable<RemoConnection> =
//            connectWorkaround(device, UUID.fromString(Constant.SERVICE_UUID))
//                    .subscribeOn(Schedulers.io())
//                    .observeOn(Schedulers.computation())
//                    .flatMapObservable { socket ->
//                        Observable.using({
//                            BluetoothConnection(socket)
//                        }, { connection ->
//                            Observable.create<RemoConnection> { emitter ->
//                                emitter.onNext(RemoConnection(device, connection))
//                            }
//                        }, { connection ->
//                            connection.closeConnection()
//                        })
//                    }

    private fun connectWorkaround(bluetoothDevice: BluetoothDevice, uuid: UUID): Single<BluetoothSocket> =
            Single.create { emitter ->
                var bluetoothSocket: BluetoothSocket? = null
                try {
                    bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid)
                    bluetoothSocket.connect()
                    emitter.onSuccess(bluetoothSocket)
                } catch (e: IOException) {
                    if (bluetoothSocket != null) {
                        try {
                            bluetoothSocket.close()
                        } catch (suppressed: IOException) {
                            if (SDK_INT >= 19) {
                                e.addSuppressed(suppressed)
                            }
                        }
                    }
                    emitter.onError(e)
                }
            }

    private fun observeByteStream(connection: BluetoothConnection): Flowable<Byte> =
            connection.observeByteStream()

    private fun observePackets(connection: BluetoothConnection, asq: AcquisitionMode): Flowable<out List<Byte>> =
            observeByteStream(connection)
                    .scan(LinkedList<Byte>()) { list, byte ->
                        list.addLast(byte)
                        while (list.size > Sample.PACKET_LEN)
                            list.removeFirst()
                        list
                    }
                    .filter { bytes ->
                        Sample.isValidPacketNew(bytes, asq) // ==>> this, added new 13-3-2020
                    }

    override fun observeSamples(connection: RemoConnection, asq: AcquisitionMode): Flowable<Sample> =  // ==>> this has user experimental
            observePackets(connection.connection, asq)
                    .map { packet ->
//                        Sample.fromPacket(packet) // ==>> need to fix this
                        Sample.fromPacketNewFunc(packet, asq)
                    }
                    .filter { sample ->
                        sample.validSample  // ==>> this returns false
                    }

    private fun waitReply(connection: BluetoothConnection): Single<out List<Byte>> =
            observeByteStream(connection)
                    .takeWhileInclusive { value ->
                        value != ATProtocol.LF
                    }
                    .toList {
                        LinkedList<Byte>()
                    }

    private fun waitResponse(connection: BluetoothConnection): Single<ATResponse> =
            waitReply(connection)
                    .map { reply ->
                        ATResponse.createFromReply(reply)
                    }

    private fun writeBytes(connection: BluetoothConnection, bytes: ByteArray): Single<Boolean> =
            Single.defer {
                Single.just(connection.send(bytes))
            }

    private fun writeCommand(connection: BluetoothConnection, command: ATCommand): Single<Boolean> = // ==>> this has user experimental
            writeBytes(connection, command.getBytes())

    private fun writeCommandAndWaitResponse(connection: BluetoothConnection, command: ATCommand): Single<ATResponse> =  // ==>> this has user experimental
            writeCommand(connection, command)
                    .flatMap { success ->
                        if (success)
                            waitResponse(connection)
                        else
                            Single.error(WriteException())
                    }

    private fun setAcquisitionMode(connection: BluetoothConnection, mode: AcquisitionMode): Single<Boolean> =  // ==>> this has user experimental
            writeCommandAndWaitResponse(connection, ATCommand(Register.AcquisitionMode, Operation.Write, mode.value))
                    .map { response ->
                        response.isOK()
                    }

    private fun setOperatingMode(connection: BluetoothConnection, mode: OperatingMode): Single<Boolean> =  // ==>> this has user experimental
            writeCommandAndWaitResponse(connection, ATCommand(Register.OperatingMode, Operation.Write, mode.value))
                    .map { response ->
                        response.isOK()
                    }

    override fun startSessionAndObserveSamples(connection: RemoConnection): Flowable<Sample> =
            setAcquisitionMode(connection.connection, AcquisitionMode.RMS) // ==>> this has user experimental
                    .flatMap { success ->
                        if (success)
                            setOperatingMode(connection.connection, OperatingMode.TxMode) // ==>> this has user experimental
                        else
                            Single.error(AcquisitionModeException())
                    }
                    .toFlowable()
                    .flatMap { success ->
                        if (success)
                            observeSamples(connection, AcquisitionMode.RMS).skip(1) // First sample is weird!
                        else
                            Flowable.error(OperatingModeException())
                    }


}
