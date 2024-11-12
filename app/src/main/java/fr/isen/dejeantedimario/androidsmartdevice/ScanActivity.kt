package fr.isen.dejeantedimario.androidsmartdevice

import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
//import androidx.activity.result.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.isen.dejeantedimario.androidsmartdevice.ui.theme.AndroidSmartDeviceTheme

class ScanActivity : ComponentActivity() {

    private val enableBtLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Bluetooth est activé, vous pouvez commencer à l'utiliser
            } else {
                // L'utilisateur a refusé d'activer Bluetooth, vous pouvez afficher un message d'erreur
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidSmartDeviceTheme {
                ScanScreen(
                    enableBluetooth = { enableBluetooth() }
                )
            }
        }
    }

    // Fonction pour activer Bluetooth
    private fun enableBluetooth() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtLauncher.launch(enableBtIntent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(enableBluetooth: () -> Unit) {
    // État pour gérer si le scan est actif ou non
    var isScanning by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Vérification si Bluetooth est disponible
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    // Fonction pour vérifier si Bluetooth est disponible
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null
    }

    // Fonction pour afficher un message Toast
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // Fonction pour démarrer le scan
    fun startScan() {
        if (isBluetoothAvailable()) {
            enableBluetooth()
            isScanning = true
        } else {
            showToast("Bluetooth n'est pas disponible sur cet appareil.")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(text = "My Bluetooth")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = Color(0xFF6200EE))
        )

        // Colonne principale du UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Affichage du titre de la page
            Text(
                text = "Scan",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Images côte à côte pour "Start" et "Stop" scan
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp), // Espace entre les images
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image de démarrage du scan
                Image(
                    painter = painterResource(id = R.drawable.start_scan),
                    contentDescription = "Start SCAN",
                    modifier = Modifier
                        .size(120.dp)
                        .clickable {
                            // Vérification et démarrage du scan
                            startScan()
                        }
                )

                // Image pour arrêter le scan
                Image(
                    painter = painterResource(id = R.drawable.stop_scan),
                    contentDescription = "Stop SCAN",
                    modifier = Modifier
                        .size(120.dp)
                        .clickable {
                            // Arrêter le scan
                            isScanning = false
                        }
                )
            }

            // Affichage de la barre de chargement si le scan est en cours
            if (isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth() // Remplit la largeur du conteneur
                        .padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Texte indiquant l'état actuel du scan
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
        ScanScreen(enableBluetooth = {})
    }
}
