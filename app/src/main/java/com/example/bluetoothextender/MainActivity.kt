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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.bluetoothextender.ui.home.HomePage
import java.io.IOException
import java.util.UUID
import java.util.regex.Pattern

class MainActivity : ComponentActivity() {

    private val bluetoothReceiver: BluetoothReceiver = BluetoothReceiver()
    private val bluetoothIntentFilter: IntentFilter = IntentFilter(BluetoothDevice.ACTION_UUID)

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private var bluetoothSetupInProgress: Boolean = false

    private lateinit var permRequester: ActivityResultLauncher<String>
    private lateinit var btIntentStarter: ActivityResultLauncher<Intent>
    private lateinit var deviceChooser: ActivityResultLauncher<IntentSenderRequest>

    private val uuidReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            assert(intent?.action == BluetoothReceiver.SEND_UUID)
            Log.v(TAG, "Received uuid and device.")
            val supportedUUID: String? = intent?.getStringExtra(BluetoothReceiver.SUPPORTED_UUID)
            val device: BluetoothDevice? = BluetoothUtils.getDataFromIntent(
                intent,
                BluetoothReceiver.DEVICE,
                BluetoothDevice::class
            )
            this@MainActivity.setupTransferThreads(supportedUUID, device)
        }
    }
    private val uuidIntentFilter: IntentFilter = IntentFilter(BluetoothReceiver.SEND_UUID)

    private lateinit var btPermission: String

    private lateinit var readThread: BluetoothReader
    private lateinit var readDevice: BluetoothDevice

    private lateinit var writeThread: BluetoothWriter
    private lateinit var writeDevice: BluetoothDevice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // TODO("Make actually edge to edge visually")
        setContent {
            HomePage()
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
                    RESULT_OK -> {
                        Log.v(TAG, "Found device!")
                        fetchSupportedUuidForDevice(getDevice(result))
                    }

                    RESULT_CANCELED -> {
                        Log.e(TAG, "Bluetooth device selection cancelled")
                    }
                }
            }

        Log.v(TAG, "Starting setup of bluetooth...")
        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
//        ensureBluetoothEnabled()
//        setupCompanionDeviceSearch()
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
    }

    private fun setupCompanionDeviceSearch(alreadyConnectedDevice: BluetoothDevice? = null) {
        val deviceFilterBuilder: BluetoothDeviceFilter.Builder = BluetoothDeviceFilter.Builder()
        if (alreadyConnectedDevice != null) {
            checkBluetoothPermission(this)
            val notDevicePattern: String = "^(?!" + alreadyConnectedDevice.name.replace(
                regex = "\\s".toRegex(),
                replacement = "\\\\s"
            ) + ").*"
            deviceFilterBuilder.setNamePattern(Pattern.compile(notDevicePattern))
        }
        val deviceFilter: BluetoothDeviceFilter = deviceFilterBuilder.build()
        val pairingRequest: AssociationRequest =
            AssociationRequest.Builder()
                .addDeviceFilter(deviceFilter)
                .build()

        registerReceivers()

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
                unregisterReceivers()
            }
        }, null)
    }

    private fun getDevice(activityResult: ActivityResult): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            activityResult.data?.getParcelableExtra(
                CompanionDeviceManager.EXTRA_ASSOCIATION,
                AssociationInfo::class.java
            )?.associatedDevice?.bluetoothDevice
        else activityResult.data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
    }

    private fun fetchSupportedUuidForDevice(device: BluetoothDevice?) {
        assert(checkBluetoothPermission(this))
        Log.v(TAG, "Fetching UUID for device: ${device?.name}")
        device?.fetchUuidsWithSdp()
    }

    private fun connectToDeviceWithUuid(device: BluetoothDevice?, uuid: UUID): BluetoothSocket? {
        assert(checkBluetoothPermission(this))
        val btSocket: BluetoothSocket? = device?.createRfcommSocketToServiceRecord(uuid)
        Log.v(TAG, "connecting to device ${device?.name} using UUID: $uuid\nsocket: $btSocket")
        bluetoothAdapter.cancelDiscovery()
        btSocket?.connect()

        return btSocket
    }

    private fun getSocket(uuid: String?, device: BluetoothDevice?): BluetoothSocket? {
        if (uuid == null) {
            Log.e(TAG, "No supported UUIDs found for device.")
            unregisterReceivers()
            ensureBluetoothEnabled()
            return null
        }
        return connectToDeviceWithUuid(device, UUID.fromString(uuid))
    }

    private fun setupTransferThreads(uuid: String?, device: BluetoothDevice?) {
        unregisterReceivers()

        val socket = getSocket(uuid, device)

        assert(checkBluetoothPermission(this))
        if (socket == null) {
            Log.e(TAG, "Socket creation failed for device: ${device?.name}")
            return
        }

        // TODO("figure out how to manage choosing devices on separate threads")
        // this will be needed when the ui has two buttons to choose devices
        if (!this::readDevice.isInitialized) {
            readDevice = device!!
            readThread = BluetoothReader(socket, bluetoothHandler)
            if (!this::writeDevice.isInitialized) {
                chooseWriteDevice(readDevice)
            }
        } else if (!this::writeDevice.isInitialized) {
            writeDevice = device!!
            writeThread = BluetoothWriter(socket, bluetoothHandler)
            Log.v(TAG, "Threads setup, connections ready, can start transmitting")
            Log.v(
                TAG,
                "Read device class: ${readDevice.bluetoothClass.deviceClass}; Write device class: ${writeDevice.bluetoothClass.deviceClass}"
            )
        }

    }

    // TODO("figure out how to choose devices in any order")
    private fun chooseReadDevice(writeDevice: BluetoothDevice? = null) {
        ensureBluetoothEnabled()
        setupCompanionDeviceSearch(writeDevice)
    }

    private fun chooseWriteDevice(readDevice: BluetoothDevice? = null) {
        ensureBluetoothEnabled()
        setupCompanionDeviceSearch(readDevice)
    }

    private fun startTransferring() {
        readThread.start()
    }

    private fun stopTransferring() {
        readThread.cancel()
        readThread.join()

        writeThread.cancel()
    }

    val bluetoothHandler: Handler = object : Handler(Looper.getMainLooper()) {

        private var bluetoothWriter: BluetoothWriter? = null

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            when (msg.what) {
                MESSAGE_READ -> {
                    val buffer: ByteArray = msg.obj as ByteArray
                    bluetoothWriter?.write(buffer)
                }

                MESSAGE_WRITTEN -> Log.v(TAG, "Message sent.")
                MESSAGE_WRITE_FAILED -> {
                    this@MainActivity.stopTransferring()
                }
            }
        }
    }

    private fun registerReceivers() {
        bluetoothSetupInProgress = true
        registerReceiver(uuidReceiver, uuidIntentFilter, receiverFlags)
        registerReceiver(bluetoothReceiver, bluetoothIntentFilter)
    }

    private fun unregisterReceivers() {
        unregisterReceiver(uuidReceiver)
        unregisterReceiver(bluetoothReceiver)
        bluetoothSetupInProgress = false
    }

    private inner class BluetoothReader(val socket: BluetoothSocket, val handler: Handler) :
        Thread() {

        private val buffer: ByteArray = ByteArray(1024)
        override fun run() {
            var numBytes: Int

            while (true) {
                numBytes = try {
                    socket.inputStream.read(buffer)
                } catch (e: IOException) {
                    Log.v(TAG, "Input stream disconnected", e)
                    break
                }

                val readMessage = handler.obtainMessage(MESSAGE_READ, numBytes, -1, buffer)
                readMessage.sendToTarget()
            }
        }

        fun cancel() {
            socket.close()
        }
    }

    private inner class BluetoothWriter(val socket: BluetoothSocket, val handler: Handler) {

        fun write(buffer: ByteArray) {
            try {
                socket.outputStream.write(buffer)
            } catch (e: IOException) {
                Log.v(TAG, "Connection interrupted", e)

                val errorMessage = handler.obtainMessage(MESSAGE_WRITE_FAILED, e)
                errorMessage.sendToTarget()
                return
            }

            val writtenMessage = handler.obtainMessage(MESSAGE_WRITTEN, buffer)
            writtenMessage.sendToTarget()
        }

        fun cancel() {
            socket.close()
        }
    }

    companion object {
        private val TAG: String = "MainActivity"

        // based on supported build
        private val receiverFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) RECEIVER_EXPORTED else 0

        private fun checkBluetoothPermission(context: Context): Boolean {
            return Build.VERSION.SDK_INT <= Build.VERSION_CODES.R ||
                    (ActivityCompat
                        .checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == PackageManager.PERMISSION_GRANTED
                            )
        }

        val MESSAGE_READ: Int = 0
        val MESSAGE_WRITTEN: Int = 1
        val MESSAGE_WRITE_FAILED: Int = 2
    }
}