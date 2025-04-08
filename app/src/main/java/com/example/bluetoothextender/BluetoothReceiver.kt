package com.example.bluetoothextender

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Parcelable
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import java.util.UUID

class BluetoothReceiver : BroadcastReceiver() {

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onReceive(context: Context?, intent: Intent?) {

        when (intent?.action) {
            BluetoothDevice.ACTION_UUID -> {
                val uuids = intent.getParcelableExtra(
                    BluetoothDevice.EXTRA_UUID,
                    Array<Parcelable>::class.java
                )
                val device: BluetoothDevice? = intent.getParcelableExtra(
                    BluetoothDevice.EXTRA_DEVICE,
                    BluetoothDevice::class.java
                )
//                Log.v(TAG, uuids?.map { it.toString() }?.reduceOrNull { acc, s -> "$acc\n$s" } ?: "null")
//                Log.v(TAG, device?.address ?: "null")

                val supportedUUID: UUID? = UUID.fromString(uuids?.last()?.toString())
                val btSocket: BluetoothSocket? =
                    device?.createRfcommSocketToServiceRecord(supportedUUID)
                Log.v(TAG, "socket: $btSocket")
//                B.cancelDiscovery()
                btSocket?.connect()
//                TODO("decide how to get a supported UUID - right now using last")
            }
        }
//
    }

    companion object {
        private val TAG: String = "BluetoothReceiver"
    }
}