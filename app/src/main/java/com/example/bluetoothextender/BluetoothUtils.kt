import android.app.Activity
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat.getSystemService

class BluetoothUtils(val context: Context, val activity: Activity) {

    val bluetoothManager: BluetoothManager? = getSystemService(context, BluetoothManager::class.java)
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.getAdapter()

    public fun setup() {
        if (bluetoothAdapter == null) {
            System.exit(0)
        }

        var RESULT_ENABLE_BT: Int = 0

        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            // starts a sub activity from activity with the passed intent, i.e. to enable bluetooth.
            // when subactivity exits, it returns RESULT_ENABLE_BT to activity's onActivityResult() for processing
            startActivityForResult(activity, enableBtIntent, RESULT_ENABLE_BT, null)
        }
    }

}


