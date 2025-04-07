package com.example.bluetoothextender

import BluetoothUtils
import android.Manifest
import android.app.ComponentCaller
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.companion.CompanionDeviceManager
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.app.ActivityCompat
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
//        btUtils.ensureBluetoothEnabled()
//        TODO("check permissions and ask to turn on")
        ensureBluetoothEnabled()
//        btUtils.setupCompanionDeviceSearch()
        Log.v(TAG, "Bluetooth setup done!")
    }

    fun ensureBluetoothEnabled() {
        val bluetoothManager: BluetoothManager? =
            this.getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        // starts a sub activity from activity with the passed intent, i.e. to enable bluetooth.
        // when subactivity exits, it returns RESULT_ENABLE_BT to activity's onActivityResult() as requestCode for processing
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        startActivityForResult(enableBtIntent, BluetoothUtils.RESULT_ENABLE_BT, null)
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