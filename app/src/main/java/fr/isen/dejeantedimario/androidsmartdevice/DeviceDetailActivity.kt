package fr.isen.dejeantedimario.androidsmartdevice

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class DeviceDetailActivity : ComponentActivity() {
    private var bluetoothGatt: BluetoothGatt? = null
    private var isConnected by mutableStateOf(false)
    private var isLedOn by mutableStateOf(false) // État pour la LED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Récupère les informations du périphérique
        val deviceName = intent.getStringExtra("DEVICE_NAME") ?: "Nom non disponible"
        val deviceAddress = intent.getStringExtra("DEVICE_ADDRESS") ?: "Adresse non disponible"

        // BluetoothManager pour établir la connexion
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)

        setContent {
            DeviceDetailScreen(
                deviceName = deviceName,
                deviceAddress = deviceAddress,
                isConnected = isConnected,
                onConnect = { connectToDevice(device) },
                onDisconnect = { disconnectDevice() },
                isLedOn = isLedOn, // Passer l'état de la LED
                onToggleLed = { toggleLed() } // Passer la fonction pour changer l'état de la LED
            )
        }
    }

    @SuppressLint("MissingPermission") // Les permissions ont déjà été demandées
    private fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(
            this,
            false,
            object : BluetoothGattCallback() {
                override fun onConnectionStateChange(
                    gatt: BluetoothGatt?,
                    status: Int,
                    newState: Int
                ) {
                    super.onConnectionStateChange(gatt, status, newState)
                    isConnected = newState == BluetoothGatt.STATE_CONNECTED
                    runOnUiThread {
                        if (isConnected) {
                            showToast("Connecté à ${device.name ?: "Périphérique inconnu"}")
                        } else {
                            showToast("Déconnecté de ${device.name ?: "Périphérique inconnu"}")
                        }
                    }
                }
            },
            BluetoothDevice.TRANSPORT_LE  // Le transport Low Energy
        )
    }

    @SuppressLint("MissingPermission")
    private fun disconnectDevice() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close() // Toujours fermer la connexion lorsque tu as terminé
        bluetoothGatt = null
        isConnected = false
        showToast("Périphérique déconnecté")
    }

    private fun toggleLed() {
        isLedOn = !isLedOn // Inverser l'état de la LED
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    deviceName: String,
    deviceAddress: String,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    isLedOn: Boolean, // Ajouter l'état de la LED
    onToggleLed: () -> Unit // Ajouter la fonction pour changer l'état de la LED
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Bandeau supérieur avec le nom et l'adresse du périphérique
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = deviceName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Adresse MAC : $deviceAddress",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Statut de connexion
        Text(
            text = if (isConnected) "Statut : Connecté" else "Statut : Déconnecté",
            fontSize = 18.sp,
            color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFFF0000),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp)
        )

        // Boutons Connecter et Déconnecter
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onConnect,
                enabled = !isConnected,
                modifier = Modifier.padding(8.dp)
            ) {
                Text(text = "Connecter")
            }
            Button(
                onClick = onDisconnect,
                enabled = isConnected,
                modifier = Modifier.padding(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text(text = "Déconnecter")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Image de la LED
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = if (isLedOn) R.drawable.ledon else R.drawable.ledoff),
                contentDescription = if (isLedOn) "ledon1" else "ledoff1",
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 24.dp)
                    .clickable {
                        onToggleLed() // Appeler la fonction pour changer l'état de la LED
                    }
            )

            Image(
                painter = painterResource(id = if (isLedOn) R.drawable.ledon else R.drawable.ledoff),
                contentDescription = if (isLedOn) "ledon2" else "ledoff2",
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 24.dp)
                    .clickable {
                        onToggleLed() // Appeler la fonction pour changer l'état de la LED
                    }
            )

            Image(
                painter = painterResource(id = if (isLedOn) R.drawable.ledon else R.drawable.ledoff),
                contentDescription = if (isLedOn) "ledon3" else "ledoff3",
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 24.dp)
                    .clickable {
                        onToggleLed() // Appeler la fonction pour changer l'état de la LED
                    }
            )
        }
    }
}
