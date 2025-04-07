package com.example.bluetoothextender

import BluetoothUtils
import android.app.ComponentCaller
import android.bluetooth.BluetoothDevice
import android.companion.CompanionDeviceManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.bluetoothextender.ui.theme.BluetoothExtenderTheme

class MainActivity : ComponentActivity() {

    val btUtils: BluetoothUtils by lazy{ BluetoothUtils(baseContext, this) }
    val TAG: String = "MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BluetoothExtenderTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        Log.v(TAG, "Starting setup of bluetooth...")
        btUtils.ensureBluetoothEnabled()
        btUtils.setupCompanionDeviceSearch()
        Log.v(TAG, "Bluetooth setup done!")
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        caller: ComponentCaller
    ) {
        super.onActivityResult(requestCode, resultCode, data, caller)

        when (requestCode) {
            BluetoothUtils.RESULT_ENABLE_BT -> when (resultCode) {
                RESULT_OK -> {
                    Log.v(TAG, "Starting device search...")
                }

                RESULT_CANCELED -> {
                    Log.v(TAG, "Device search cancelled!")
                    TODO("Show error message about BT disabled")
                }

                else -> Log.e(TAG, "Unknown result code for $requestCode: $resultCode")
            }
            BluetoothUtils.SELECT_DEVICE_REQUEST_CODE -> when (resultCode) {
                RESULT_OK -> {
                    Log.v(TAG, "Device found, connecting...")
                    val deviceToPair: BluetoothDevice? = data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
                    TODO("figure out why this is not called from startIntentSenderForResult")
                }

                RESULT_CANCELED -> Log.e(TAG, "Bluetooth selection failed.")
                else -> Log.e(TAG, "Unknown result code for $requestCode: $resultCode")
            }

            else -> Log.e(TAG, "Unknown request code: $requestCode")
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BluetoothExtenderTheme {
        Greeting("Android")
    }
}