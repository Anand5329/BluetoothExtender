package com.example.bluetoothextender

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BluetoothConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.v(TAG, "Intent received: ${intent}")
        if (intent?.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
            Log.v(TAG, "Bluetooth device connection changed: ${intent.data}")
        }
    }

    companion object {
        val TAG: String = "BluetoothConnectionReceiver"
    }
}