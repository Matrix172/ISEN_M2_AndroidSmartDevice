package fr.isen.dejeantedimario.androidsmartdevice

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

    // Lanceur pour demander l'activation du Bluetooth
    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Bluetooth activé
                Toast.makeText(this, "Bluetooth activé", Toast.LENGTH_SHORT).show()
            } else {
                // Bluetooth non activé ou l'utilisateur a refusé
                Toast.makeText(this, "Activation Bluetooth refusée", Toast.LENGTH_SHORT).show()
            }
        }

    // Lanceur pour demander les permissions Bluetooth
    private val requestBluetoothPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                BluetoothUtils.enableBluetooth(this, enableBluetoothLauncher)
            } else {
                Toast.makeText(this, "Permissions Bluetooth refusées", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidSmartDeviceTheme {
                ScanScreen(
                    onStartScan = {
                        // Appel de `startScan` avec les bons paramètres
                        BluetoothUtils.startScan(
                            this,
                            this,
                            requestBluetoothPermissionsLauncher,
                            enableBluetoothLauncher
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(onStartScan: () -> Unit) {
    val context = LocalContext.current
    var isScanning by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(text = "My Bluetooth")
            },
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
            Text(
                text = "Scan",
                fontSize = 28.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image pour démarrer le scan
                Image(
                    painter = painterResource(id = R.drawable.start_scan),
                    contentDescription = "Start SCAN",
                    modifier = Modifier
                        .size(120.dp)
                        .clickable {
                            onStartScan()  // Utilisation de la lambda pour démarrer le scan
                            isScanning = true
                        }
                )

                // Image pour arrêter le scan
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
