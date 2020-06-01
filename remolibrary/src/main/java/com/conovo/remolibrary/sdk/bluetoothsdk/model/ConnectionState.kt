package com.remo.bluetoothtestapp.bluetoothsdk.model

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import com.github.ivbaranov.rxbluetooth.events.ConnectionStateEvent

data class ConnectionState(
        val state: State,
        val previousState: State,
        val device: BluetoothDevice
) {

    enum class State(val value: Int) {
        None(-1000),
        Disconnected(BluetoothAdapter.STATE_DISCONNECTED),
        Connecting(BluetoothAdapter.STATE_CONNECTING),
        Connected(BluetoothAdapter.STATE_CONNECTED),
        Disconnecting(BluetoothAdapter.STATE_DISCONNECTING),
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

    internal constructor(event: ConnectionStateEvent) : this(State.parse(event.state), State.parse(event.previousState), event.bluetoothDevice)
}
