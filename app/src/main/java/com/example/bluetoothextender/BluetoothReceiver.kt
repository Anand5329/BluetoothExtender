package com.example.bluetoothextender

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.ParcelUuid
import android.os.Parcelable
import android.util.Log
import java.util.UUID


class BluetoothReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        when (intent?.action) {
            BluetoothDevice.ACTION_UUID -> {
                val uuids =
                    BluetoothUtils.getArrayDataFromIntent(
                        intent,
                        BluetoothDevice.EXTRA_UUID,
                        Parcelable::class
                    )
                val device: BluetoothDevice? =
                    BluetoothUtils.getDataFromIntent(
                        intent,
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class
                    )

                val supportedUUID: UUID? =
                    UUID.fromString(uuids?.first { it !in RESERVED_UUIDS }.toString())

                Log.v(TAG, "Sending UUID and device back...")
                val sendBackUUID: Intent = Intent(SEND_UUID)
                sendBackUUID.putExtra(SUPPORTED_UUID, supportedUUID.toString())
                sendBackUUID.putExtra(DEVICE, device)
                context?.sendBroadcast(sendBackUUID)
            }
        }
    }

    companion object {
        private val TAG: String = "BluetoothReceiver"

        //        val MAIN_ACTION = "com.example.bluetoothextender.SEND_TO_MAIN"
        val SEND_UUID = "android.intent.action.SEND_UUID"
        val SUPPORTED_UUID = "SupportedUUID"
        val DEVICE = "Device"

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