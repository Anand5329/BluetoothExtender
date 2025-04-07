import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import androidx.core.content.ContextCompat.getSystemService

class BluetoothUtils(val context: Context, val activity: Activity) {

    val bluetoothManager: BluetoothManager? =
        getSystemService(context, BluetoothManager::class.java)
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    fun ensureBluetoothEnabled() {
        if (bluetoothAdapter == null) {
            activity.finish()
            System.exit(0)
        }

        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            // starts a sub activity from activity with the passed intent, i.e. to enable bluetooth.
            // when subactivity exits, it returns RESULT_ENABLE_BT to activity's onActivityResult() as requestCode for processing
            startActivityForResult(activity, enableBtIntent, RESULT_ENABLE_BT, null)
        }
    }

    fun setupCompanionDeviceSearch() {
        val deviceFilter: BluetoothDeviceFilter = BluetoothDeviceFilter.Builder().build()
        val pairingRequest: AssociationRequest =
            AssociationRequest.Builder()
                .addDeviceFilter(deviceFilter)
                .build()

        val deviceManager: CompanionDeviceManager? =
            context.getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager?
        deviceManager?.associate(pairingRequest, object : CompanionDeviceManager.Callback() {
            override fun onAssociationPending(intentSender: IntentSender) {
                intentSender.let {
                    startIntentSenderForResult(
                        activity,
                        it,
                        SELECT_DEVICE_REQUEST_CODE,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                }
//                startIntentSenderForResult(
//                    activity, intentSender, SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0, null
//                )
            }

            override fun onAssociationCreated(associationInfo: AssociationInfo) {
                // association created
                Log.v(TAG, "Association created: $associationInfo")
            }

            // before Android 13
            override fun onDeviceFound(chooseLauncher: IntentSender) {
                startIntentSenderForResult(
                    activity, chooseLauncher, SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0, null
                )
            }

            override fun onFailure(errorMessage: CharSequence?) {
                Log.e(TAG,
                    "Bluetooth connection failed: " + (errorMessage?.toString()
                        ?: "no error message")
                )
            }
        }, null)
    }

    companion object {
        val RESULT_ENABLE_BT: Int = 1110
        val SELECT_DEVICE_REQUEST_CODE: Int = 1112

        val TAG: String = "BluetoothUtils"
    }
}
