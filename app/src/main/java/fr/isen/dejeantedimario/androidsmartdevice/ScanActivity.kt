package fr.isen.dejeantedimario.androidsmartdevice

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
                scanLeDevice(true)
            } else {
                showToast("Activation du Bluetooth refusée.")
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.entries.all { it.value }) {
                scanLeDevice(true)
            } else {
                showToast("Permissions nécessaires non accordées.")
            }
        }

    // Liste des périphériques scannés avec RSSI
    private var scannedDevicesWithRssi by mutableStateOf<List<Pair<BluetoothDevice, Int>>>(emptyList())
    private var isScanning by mutableStateOf(false) // Correction : `var` au lieu de `val`.

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidSmartDeviceTheme {
                ScanScreen(
                    onStartScan = { initScanBLE() },
                    devices = scannedDevicesWithRssi,
                    isScanning = isScanning, // Correctement passé au composant.
                    onDeviceClick = { device ->
                        val intent = Intent(this, DeviceDetailActivity::class.java).apply {
                            putExtra("DEVICE_ADDRESS", device.address)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        //Dès la détection, on affiche les devices avec leur RSSI
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device
            val rssi = result.rssi
            if (!scannedDevicesWithRssi.any { it.first.address == device.address }) {
                scannedDevicesWithRssi = scannedDevicesWithRssi + (device to rssi)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            showToast("Scan échoué avec le code $errorCode")
            isScanning = false
        }
    }

    private fun initScanBLE() {
        if (bluetoothAdapter?.isEnabled == true) {
            scanLeDeviceWithPermission()
        } else {
            enableBluetooth()
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
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            showToast("Scanner BLE non disponible.")
            return
        }

        if (scanning) {
            isScanning = true
            showToast("Scan BLE démarré.")
            try {
                scanner.startScan(scanCallback)
                // Arrêter le scan après 10 secondes
                Handler(Looper.getMainLooper()).postDelayed({
                    stopScan(scanner)
                }, 10000)
            } catch (e: SecurityException) {
                showToast("Permissions insuffisantes pour démarrer le scan.")
                isScanning = false
            }
        } else {
            stopScan(scanner)
        }
    }

    private fun stopScan(scanner: BluetoothLeScanner) {
        try {
            scanner.stopScan(scanCallback)
            isScanning = false
            showToast("Scan BLE arrêté.")
        } catch (e: SecurityException) {
            showToast("Permissions insuffisantes pour arrêter le scan.")
        }
    }

    private fun getAllPermissionsForBLE(): Array<String> {
        var allPermissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            allPermissions += arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        }
        return allPermissions
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onStartScan: () -> Unit,
    devices: List<Pair<BluetoothDevice, Int>>,
    isScanning: Boolean,
    onDeviceClick: (BluetoothDevice) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(text = "My Bluetooth") },
            colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = Color(0xFF6200EE))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                        }
                )

                Image(
                    painter = painterResource(id = R.drawable.stop_scan),
                    contentDescription = "Stop SCAN",
                    modifier = Modifier
                        .size(120.dp)
                        .clickable {
                            // Arrêter le scan
                            // Implémentez l'arrêt ici
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
            DeviceList(devices = devices, onDeviceClick = onDeviceClick)
        }
    }
}

@Composable
fun DeviceList(
    devices: List<Pair<BluetoothDevice, Int>>,
    onDeviceClick: (BluetoothDevice) -> Unit
) {
    val context = LocalContext.current

    LazyColumn {
        items(devices) { (device, rssi) ->
            val name = if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                device.name ?: "Inconnu"
            } else {
                "Permission requise"
            }

            val address = if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                device.address
            } else {
                "Permission requise"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable { onDeviceClick(device) }
            ) {
                Text(
                    text = "$name - $address (RSSI: $rssi)",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}
