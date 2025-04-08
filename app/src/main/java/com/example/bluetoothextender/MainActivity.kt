package com.example.bluetoothextender

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.example.bluetoothextender.ui.theme.BluetoothExtenderTheme
import java.util.UUID

class MainActivity : ComponentActivity() {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private lateinit var permRequester: ActivityResultLauncher<String>
    private lateinit var btIntentStarter: ActivityResultLauncher<Intent>
    private lateinit var deviceChooser: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var connectBondIntent: ActivityResultLauncher<Intent>

    private lateinit var btPermission: String

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

        // choosing the correct permission based on Android version
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            btPermission = Manifest.permission.BLUETOOTH_CONNECT
        } else {
            btPermission = Manifest.permission.BLUETOOTH_ADMIN
        }

        permRequester =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (!isGranted) {
                    Log.v(TAG, "Bluetooth Denied. Exiting...")
                    finish()
                    System.exit(0)
                } else {
                    ensureBluetoothEnabled()
                }
            }

        btIntentStarter =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                when (result.resultCode) {
                    RESULT_OK -> setupCompanionDeviceSearch()
                    RESULT_CANCELED -> {
                        Log.v(TAG, "Turning on bluetooth denied. Try again.")
                        ensureBluetoothEnabled()
                    }
                }
            }

        deviceChooser =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                when (result.resultCode) {
                    RESULT_OK -> connectToDevice(getDevice(result))
                    RESULT_CANCELED -> Log.e(TAG, "Bluetooth device selection cancelled")
                }
            }

        Log.v(TAG, "Starting setup of bluetooth...")
        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        ensureBluetoothEnabled()
    }

    private fun ensureBluetoothEnabled() {
        if (bluetoothAdapter == null) {
            finish()
            System.exit(0)
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permRequester.launch(btPermission)
            return
        }

        if (bluetoothAdapter.isEnabled == false) {
            // starts a sub activity from activity with the passed intent, i.e. to enable bluetooth.
            btIntentStarter.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }
        setupCompanionDeviceSearch()
    }

    private fun setupCompanionDeviceSearch() {
        val deviceFilter: BluetoothDeviceFilter = BluetoothDeviceFilter.Builder().build()
        val pairingRequest: AssociationRequest =
            AssociationRequest.Builder()
                .addDeviceFilter(deviceFilter)
                .build()

        val deviceManager: CompanionDeviceManager? =
            getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager?
        deviceManager?.associate(pairingRequest, object : CompanionDeviceManager.Callback() {
            override fun onAssociationPending(intentSender: IntentSender) {
                val request: IntentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
                deviceChooser.launch(request)
            }

            override fun onAssociationCreated(associationInfo: AssociationInfo) {
                // association created
                Log.v(TAG, "Association created: $associationInfo")
            }

            // before Android 13
            override fun onDeviceFound(chooseLauncher: IntentSender) {

                val request: IntentSenderRequest =
                    IntentSenderRequest.Builder(chooseLauncher).build()
                deviceChooser.launch(request)
            }

            override fun onFailure(errorMessage: CharSequence?) {
                Log.e(
                    TAG,
                    "Bluetooth connection failed: " + (errorMessage?.toString()
                        ?: "no error message")
                )
            }
        }, null)
    }

    private fun getDevice(activityResult: ActivityResult): BluetoothDevice? {
        val device: BluetoothDevice? =
            activityResult.data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
        return device
    }


    private fun connectToDevice(device: BluetoothDevice?) {
        assert(
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.R ||
                    (ActivityCompat
                        .checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                            )
        )
        Log.v(TAG, "Connecting to device: ${device?.name}")
        val btSocket: BluetoothSocket? = device?.createRfcommSocketToServiceRecord(SERIAL_BT_UUID)
        Log.v(TAG, "socket: $btSocket")
    }

    companion object {
        private val TAG: String = "MainActivity"

        val SERIAL_BT_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
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