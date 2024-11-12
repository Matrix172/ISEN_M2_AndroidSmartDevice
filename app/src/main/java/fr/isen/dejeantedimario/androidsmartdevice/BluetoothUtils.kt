package fr.isen.dejeantedimario.androidsmartdevice

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat

object BluetoothUtils {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    // Vérifier si Bluetooth est disponible
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null
    }

    // Vérifier si Bluetooth est activé
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    // Activer le Bluetooth via un intent si nécessaire
    fun enableBluetooth(activity: Activity, enableBluetoothLauncher: ActivityResultLauncher<Intent>) {
        if (bluetoothAdapter == null) {
            showToast(activity, "Bluetooth n'est pas disponible sur cet appareil.")
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            showToast(activity, "Bluetooth déjà activé.")
        }
    }

    // Vérifier et demander les permissions Bluetooth (Android 12+)
    fun hasBluetoothPermissions(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
    }

    fun requestBluetoothPermissions(
        context: Context,
        permissionsLauncher: ActivityResultLauncher<Array<String>>
    ) {
        if (!hasBluetoothPermissions(context)) {
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        }
    }

    // Démarrer le scan en vérifiant les permissions et l'état de Bluetooth
    fun startScan(context: Context, activity: Activity, permissionsLauncher: ActivityResultLauncher<Array<String>>, enableBluetoothLauncher: ActivityResultLauncher<Intent>): Boolean {
        return if (isBluetoothAvailable()) {
            if (hasBluetoothPermissions(context)) {
                enableBluetooth(activity, enableBluetoothLauncher)
                true
            } else {
                requestBluetoothPermissions(context, permissionsLauncher)
                false
            }
        } else {
            showToast(context, "Bluetooth n'est pas disponible sur cet appareil.")
            false
        }
    }

    // Afficher un message Toast
    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
