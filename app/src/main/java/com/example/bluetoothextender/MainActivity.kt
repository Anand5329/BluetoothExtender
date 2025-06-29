package com.example.bluetoothextender

import android.Manifest
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
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

    private enum class DeviceType {
        SOURCE,
        TARGET
    }

    private lateinit var currentDeviceSetupType: DeviceType

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

    private val a2dpConnectionStateChangeReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(conetext: Context?, intent: Intent?) {
                assert(intent?.action == BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)

                val state: Int? = intent?.getIntExtra(
                    BluetoothProfile.EXTRA_STATE,
                    BluetoothProfile.STATE_DISCONNECTED
                )

                if (state == BluetoothProfile.STATE_CONNECTED) {
//                val device: BluetoothDevice? = BluetoothUtils.getDataFromIntent(intent, BluetoothProfile.EXTRA_STATE,
//                    BluetoothDevice::class)
                    Log.v(TAG, "A2DP device state: connected")

                }
            }
        }
    private val a2dpConnectionIntentFilter: IntentFilter =
        IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)

    private val a2dpPlayingStateChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(conetext: Context?, intent: Intent?) {
            assert(intent?.action == BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)

            val state: Int? = intent?.getIntExtra(
                BluetoothProfile.EXTRA_STATE,
                BluetoothProfile.STATE_DISCONNECTED
            )

            if (state == BluetoothA2dp.STATE_PLAYING) {
                Log.v(TAG, "A2DP device state: playing")
            } else {
                Log.v(TAG, "A2DP device state: not playing")
            }
        }
    }
    private val a2dpPlayingIntentFilter: IntentFilter =
        IntentFilter(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)

    private lateinit var btPermission: String

    private lateinit var readThread: BluetoothReader
    private lateinit var readDevice: BluetoothDevice

    private lateinit var writeThread: BluetoothWriter
    private lateinit var writeDevice: BluetoothDevice

    private var previousDevice: BluetoothDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // TODO("Make actually edge to edge visually")
        setContent {
            HomePage( //TODO("implement buttons disable when in process")
                action1 = this::chooseReadDevice,
                action2 = this::chooseWriteDevice
            )
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
            getSystemService(COMPANION_DEVICE_SERVICE) as CompanionDeviceManager?
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

//        val socket = getSocket(uuid, device)

        assert(checkBluetoothPermission(this))
//        if (socket == null) {
//            Log.e(TAG, "Socket creation failed for device: ${device?.name}")
//            return
//        }

        if (!this::currentDeviceSetupType.isInitialized) {
            Log.e(TAG, "No device chosen, assuming read device")
            currentDeviceSetupType = DeviceType.SOURCE
        }

        when (currentDeviceSetupType) {
            DeviceType.SOURCE -> {
                readDevice = device!!
//                readThread = BluetoothReader(socket, bluetoothHandler)
                previousDevice = readDevice
            }

            DeviceType.TARGET -> {
                writeDevice = device!!
//                writeThread = BluetoothWriter(socket, bluetoothHandler)
                previousDevice = writeDevice
            }
        }

        if (this::readDevice.isInitialized && this::writeDevice.isInitialized) {
            Log.v(TAG, "Threads setup, connections ready, can start transmitting")
            Log.v(
                TAG,
                "Read device class: ${readDevice.bluetoothClass.deviceClass}; Write device class: ${writeDevice.bluetoothClass.deviceClass}"
            )
            setupAudioConnection(readDevice)
//            setupHandsfreeConnection(readDevice)
//            startTransferring()

        }

    }

    private fun setupAudioConnection(source: BluetoothDevice?) {
        // TODO("only one A2DP device is supported on android, so it'll have to be the source and this device as sink")
        // https://developer.android.com/reference/android/bluetooth/BluetoothA2dp?_gl=1*1aqc4ua*_up*MQ..*_ga*MTk5OTQzMzA1LjE3NDczNDM1NjE.*_ga_6HH9YJMN9M*czE3NDczNDM1NjAkbzEkZzAkdDE3NDczNDM1NzEkajAkbDAkaDE5NDI1MTQ5OTE.#:~:text=supports%20one%20connected%20Bluetooth%20A2dp
        // It seems android doesn't support being an A2DP sink. So try another profile?

        var bluetoothAudioHandler: BluetoothA2dp? = null

        val profileListener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                if (profile == BluetoothProfile.A2DP) {
                    bluetoothAudioHandler = proxy as BluetoothA2dp
                    try {
                        bluetoothAudioHandler!!.javaClass.getMethod(
                            "connect",
                            BluetoothDevice::class.java
                        ).invoke(bluetoothAudioHandler, source)
                        Log.v(TAG, "Destination Connected via A2DP profile")
                        assert(checkBluetoothPermission(this@MainActivity))
                        val isPlaying: Boolean =
                            bluetoothAudioHandler!!.isA2dpPlaying(source!!)
                        Log.v(TAG, "isPlaying over A2DP: $isPlaying")
                        val state: Int = bluetoothAudioHandler!!.getConnectionState(source)
                        Log.v(TAG, "Connection state: $state")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error encountered while connecting to A2DP", e)
                    }
                } else {
                    Log.v(TAG, "Profile found: $profile")
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.A2DP) {
                    try {
                        bluetoothAudioHandler!!.javaClass.getMethod(
                            "disconnect",
                            BluetoothDevice::class.java
                        ).invoke(bluetoothAudioHandler, source)
                        Log.v(TAG, "Destination Disconnected from A2DP")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error encountered while disconnecting from A2DP", e)
                    }
                    bluetoothAudioHandler = null
                }
            }
        }

        bluetoothAdapter.getProfileProxy(this, profileListener, BluetoothProfile.A2DP)
    }

    private fun setupHandsfreeConnection(source: BluetoothDevice?) {
        var bluetoothHeadset: BluetoothHeadset? = getProxyProfileListenerFor(
            BluetoothProfile.HEADSET
        )
        assert(checkBluetoothPermission(this))
        val isConnected: Boolean? = bluetoothHeadset?.isAudioConnected(source!!)
        Log.v(TAG, "BluetoothHeadset isAudioConnected: $isConnected")
    }

    private fun <T> getProxyProfileListenerFor(getProfile: Int): T? {

        var bluetoothProxyListener: T? = null

        val profileListener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                if (profile == getProfile) {
                    bluetoothProxyListener = proxy as T
                } else {
                    Log.v(TAG, "Profile found: $profile")
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == getProfile) {
                    bluetoothProxyListener = null
                }
            }
        }

        bluetoothAdapter.getProfileProxy(this, profileListener, BluetoothProfile.HEADSET)

        return bluetoothProxyListener
    }

    private fun chooseReadDevice() {
        checkSetupAndSetDeviceType(DeviceType.SOURCE)
        ensureBluetoothEnabled()
        setupCompanionDeviceSearch(previousDevice)
    }

    private fun chooseWriteDevice() {
        checkSetupAndSetDeviceType(DeviceType.TARGET)
        ensureBluetoothEnabled()
        setupCompanionDeviceSearch(previousDevice)
    }

    private fun checkSetupAndSetDeviceType(deviceType: DeviceType): Boolean {
        if (bluetoothSetupInProgress) {
            Log.e(TAG, "Setup already in progress.")
            return false
        }
        currentDeviceSetupType = deviceType
        return true
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
        registerReceiver(a2dpConnectionStateChangeReceiver, a2dpConnectionIntentFilter)
        registerReceiver(a2dpPlayingStateChangeReceiver, a2dpPlayingIntentFilter)
    }

    private fun unregisterReceivers() {
        unregisterReceiver(uuidReceiver)
        unregisterReceiver(bluetoothReceiver)
        unregisterReceiver(a2dpConnectionStateChangeReceiver)
        unregisterReceiver(a2dpPlayingStateChangeReceiver)
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