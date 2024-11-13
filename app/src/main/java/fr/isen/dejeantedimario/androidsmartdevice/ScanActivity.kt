package fr.isen.dejeantedimario.androidsmartdevice

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import fr.isen.dejeantedimario.androidsmartdevice.ui.theme.AndroidSmartDeviceTheme

class ScanActivity : ComponentActivity() {

    // Bluetooth Adapter
    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bluetoothEnableResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                showToast("Bluetooth activé.")
                // Vous pouvez maintenant démarrer le scan BLE ici si nécessaire
                scanLeDevice(true)
            } else {
                showToast("Activation du Bluetooth refusée.")
            }
        }

    // Demander les permissions
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.entries.all { it.value }) {
                scanLeDevice(true)
            } else {
                showToast("Permissions nécessaires non accordées.")
            }
        }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidSmartDeviceTheme {
                ScanScreen(
                    onStartScan = { initScanBLE() }//scanLeDevice(true) }
                )
            }
        }
        //initScanBLE()
    }

    // Afficher un message Toast
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun hasBLEMessageError(): String {
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter!!.isEnabled) {
                return ""
            } else {
                return "Bluetooth n'est pas activé."
            }
        }
        return "Pas de Bluetooth disponible."
    }

    private fun initScanBLE() {
        if (bluetoothAdapter?.isEnabled == true) {
            scanLeDeviceWithPermission()
        } else {
            getAllPermissionsForBLE()
            //activer le bluetooth
            scanLeDeviceWithPermission()
            //showToast("Bluetooth non activé.")
        }
    }

    private fun scanLeDeviceWithPermission() {
        if (allPermissionsGranted()) {
            scanLeDevice(true)
        } else {
            requestPermissionLauncher.launch(getAllPermissionsForBLE())
        }
    }

    private fun allPermissionsGranted(): Boolean {
        val permissions = getAllPermissionsForBLE()
        return permissions.all { permission ->
            ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun enableBluetooth() {
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter!!.isEnabled) {
                // Demander à l'utilisateur d'activer le Bluetooth
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                bluetoothEnableResult.launch(enableBtIntent)
            } else {
                showToast("Bluetooth déjà activé.")
            }
        } else {
            showToast("Bluetooth n'est pas disponible sur cet appareil.")
        }
    }

    private fun scanLeDevice(scanning: Boolean) {
        // Si le Bluetooth est activé et les permissions accordées, commence le scan
        val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

        if (bluetoothAdapter?.isEnabled == false) {
            //turn on bluetooth
            enableBluetooth()
        }

        if (scanning) {
            //bluetoothLeScanner?.startScan()
            showToast("Scan BLE démarré.")
        } else {
            //bluetoothLeScanner?.stopScan()
            showToast("Scan BLE arrêté.")
        }
    }

    private fun getAllPermissionsForBLE(): Array<String> {
        var allPermissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_ADMIN
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            allPermissions = allPermissions.plus(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            allPermissions = allPermissions.plus(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
        return allPermissions
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(onStartScan: () -> Unit) {
    var isScanning by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(text = "My Bluetooth") },
            modifier = Modifier.fillMaxWidth(),
            colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = Color(0xFF6200EE))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Scan", fontSize = 28.sp, modifier = Modifier.padding(bottom = 8.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.start_scan),
                    contentDescription = "Start SCAN",
                    modifier = Modifier
                        .size(120.dp)
                        .clickable {
                            onStartScan()
                            isScanning = true
                        }
                )

                Image(
                    painter = painterResource(id = R.drawable.stop_scan),
                    contentDescription = "Stop SCAN",
                    modifier = Modifier
                        .size(120.dp)
                        .clickable {
                            isScanning = false
                        }
                )
            }

            if (isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isScanning) "Le scan est en cours..." else "Le scan est arrêté.",
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScanScreenPreview() {
    AndroidSmartDeviceTheme {
        ScanScreen(onStartScan = {})
    }
}
