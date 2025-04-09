package com.example.bluetoothextender

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.ParcelUuid
import android.os.Parcelable
import android.util.Log
import androidx.annotation.RequiresPermission
import java.util.UUID
import kotlin.reflect.KClass


class BluetoothReceiver : BroadcastReceiver() {

    private inline fun <reified T : Parcelable> getDataFromIntent(
        intent: Intent?,
        key: String,
        clazz: KClass<T>
    ): T? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intent?.getParcelableExtra(key, clazz.java)
        } else {
            val extra: Parcelable? = intent?.getParcelableExtra<Parcelable>(key)
            if (extra is T?) {
                return extra
            } else {
                throw ClassCastException("Extra data Couldn't be cast to type ${T::class}, of type ${extra!!::class.qualifiedName}")
            }
        }
    }

    private inline fun <reified T : Parcelable> getArrayDataFromIntent(
        intent: Intent?,
        key: String,
        clazz: KClass<T>
    ): Array<T>? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intent?.getParcelableArrayExtra(key, clazz.java)
        } else {
            val extraArray: Array<Parcelable>? = intent?.getParcelableArrayExtra(key)
            if (extraArray == null) {
                return extraArray
            }
            for (extra in extraArray) {
                if (extra !is T) {
                    throw ClassCastException("Constituent item couldn't be cast to type ${T::class}, of type ${extra::class.qualifiedName}")
                }
            }
            return extraArray as Array<T>
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onReceive(context: Context?, intent: Intent?) {

        when (intent?.action) {
            BluetoothDevice.ACTION_UUID -> {
                val uuids =
                    getArrayDataFromIntent(intent, BluetoothDevice.EXTRA_UUID, Parcelable::class)
                val device: BluetoothDevice? =
                    getDataFromIntent(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class)

                val supportedUUID: UUID? =
                    UUID.fromString(uuids?.first { it !in RESERVED_UUIDS }.toString())
                // TODO("send uuid and device back to activity to handle socket")
                val btSocket: BluetoothSocket? =
                    device?.createRfcommSocketToServiceRecord(supportedUUID)
                Log.v(TAG, "socket: $btSocket")
//                B.cancelDiscovery()
                btSocket?.connect()
            }
        }
    }

    companion object {
        private val TAG: String = "BluetoothReceiver"

        val AudioSink: ParcelUuid = ParcelUuid.fromString("0000110B-0000-1000-8000-00805F9B34FB")

        val AudioSource: ParcelUuid = ParcelUuid.fromString("0000110A-0000-1000-8000-00805F9B34FB")

        val AdvAudioDist: ParcelUuid = ParcelUuid.fromString("0000110D-0000-1000-8000-00805F9B34FB")

        val HSP: ParcelUuid = ParcelUuid.fromString("00001108-0000-1000-8000-00805F9B34FB")

        val HSP_AG: ParcelUuid = ParcelUuid.fromString("00001112-0000-1000-8000-00805F9B34FB")

        val Handsfree: ParcelUuid = ParcelUuid.fromString("0000111E-0000-1000-8000-00805F9B34FB")

        val Handsfree_AG: ParcelUuid = ParcelUuid.fromString("0000111F-0000-1000-8000-00805F9B34FB")

        val AvrcpController: ParcelUuid =
            ParcelUuid.fromString("0000110E-0000-1000-8000-00805F9B34FB")

        val AvrcpTarget: ParcelUuid = ParcelUuid.fromString("0000110C-0000-1000-8000-00805F9B34FB")

        val ObexObjectPush: ParcelUuid =
            ParcelUuid.fromString("00001105-0000-1000-8000-00805f9b34fb")

        val Hid: ParcelUuid = ParcelUuid.fromString("00001124-0000-1000-8000-00805f9b34fb")

        val Hogp: ParcelUuid = ParcelUuid.fromString("00001812-0000-1000-8000-00805f9b34fb")

        val PANU: ParcelUuid = ParcelUuid.fromString("00001115-0000-1000-8000-00805F9B34FB")

        val NAP: ParcelUuid = ParcelUuid.fromString("00001116-0000-1000-8000-00805F9B34FB")

        val BNEP: ParcelUuid = ParcelUuid.fromString("0000000f-0000-1000-8000-00805F9B34FB")

        val PBAP_PSE: ParcelUuid = ParcelUuid.fromString("0000112f-0000-1000-8000-00805F9B34FB")

        val MAP: ParcelUuid = ParcelUuid.fromString("00001132-0000-1000-8000-00805F9B34FB")

        val MNS: ParcelUuid = ParcelUuid.fromString("00001133-0000-1000-8000-00805F9B34FB")

        val RESERVED_UUIDS: Array<ParcelUuid> = arrayOf(
            AudioSink, AudioSource, AdvAudioDist, HSP, Handsfree, AvrcpController, AvrcpTarget,
            ObexObjectPush, PANU, NAP, MAP, MNS
        )
    }
}