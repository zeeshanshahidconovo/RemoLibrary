package com.remo.bluetoothtestapp.bluetoothsdk.model

import android.bluetooth.BluetoothDevice
import com.github.ivbaranov.rxbluetooth.events.BondStateEvent

data class BondState(
        val state: State,
        val previousState: State,
        val device: BluetoothDevice
) {

    enum class State(val value: Int) {
        None(BluetoothDevice.BOND_NONE),
        Bonding(BluetoothDevice.BOND_BONDING),
        Bonded(BluetoothDevice.BOND_BONDED),
        ;

        companion object {

            fun parse(value: Int): State {
                for (state in State.values())
                    if (value == state.value)
                        return state
                return State.None
            }
        }
    }

    internal constructor(event: BondStateEvent) : this(State.parse(event.state), State.parse(event.previousState), event.bluetoothDevice)
}
