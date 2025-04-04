import android.bluetooth.*
import android.content.Intent
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat.getSystemService

class BluetoothUtils {

    val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.getAdapter()

    public fun setup() {
        if (bluetoothAdapter == null) {
            System.exit(0)
        }

        var RESULT_ENABLE_BT: int = 0

        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, RESULT_ENABLE_BT)
        }
    }

}


