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
        Log.d(TAG, "Starting setup of bluetooth...")
        btUtils.ensureBluetoothEnabled()
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
                    Log.d(TAG, "Starting device search...")
                    btUtils.setupCompanionDeviceSearch()
                }
                RESULT_CANCELED -> TODO("Show error message about BT disabled")
            }
            BluetoothUtils.SELECT_DEVICE_REQUEST_CODE -> when (resultCode) {
                RESULT_OK -> {
                    val deviceToPair: BluetoothDevice? = data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
                    Log.d(TAG, "Device found, connecting...")
                    TODO("Connect to device")


                }
            }
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