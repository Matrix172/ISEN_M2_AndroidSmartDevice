package fr.isen.dejeantedimario.androidsmartdevice

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.BluetoothProfile.STATE_CONNECTED
import android.bluetooth.BluetoothProfile.STATE_DISCONNECTED
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
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
import androidx.core.app.ActivityCompat

class DeviceDetailActivity : ComponentActivity() {
    private var bluetoothGatt: BluetoothGatt? = null
    private var ledCharacteristic: BluetoothGattCharacteristic? = null
    private var isConnected by mutableStateOf(false)
    private var ledStates = mutableStateListOf(false, false, false) // États pour les 3 LEDs
    private var servicesDiscovered by mutableStateOf(false)

    // Utilisation de mutableStateOf pour les variables des clics
    private var nbclicbouton1 by mutableStateOf(0)
    private var nbclicbouton2 by mutableStateOf(0)
    private var nbclictotal by mutableStateOf(0)

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Récupère les informations du périphérique
        val deviceName = intent.getStringExtra("DEVICE_NAME") ?: "Nom non disponible"
        val deviceAddress = intent.getStringExtra("DEVICE_ADDRESS") ?: "Adresse non disponible"

        // BluetoothManager pour établir la connexion
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)

        setContent {
            DeviceDetailScreen(
                deviceName = deviceName,
                deviceAddress = deviceAddress,
                isConnected = isConnected,
                onConnect = { connectToDevice(device) },
                onDisconnect = { disconnectDevice() },
                ledStates = ledStates,
                writeToLEDCharacteristic = { state -> writeToLEDCharacteristic(state) }, // Passer la fonction ici
                nbclicbouton1 = nbclicbouton1,
                nbclicbouton2 = nbclicbouton2,
                nbclictotal = nbclictotal,
                //onClicBouton = { numeroDeBouton -> clicbouton(numeroDeBouton) } // Passer la fonction de clics
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == STATE_CONNECTED) {
                    Log.d("BLEGATT", "Connected to GATT server. Discovering services...")
                    gatt.discoverServices()
                    runOnUiThread { isConnected = true }
                } else if (newState == STATE_DISCONNECTED) {
                    Log.d("BLEGATT", "Disconnected from GATT server.")
                    runOnUiThread { isConnected = false }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    ledCharacteristic = gatt.services.flatMap { it.characteristics }
                        .firstOrNull() // Prend la première caractéristique disponible
                    Log.d("BLE", "Services discovered: ${gatt.services.map { it.uuid }}")

                    for (service in gatt.services) {
                        Log.d("LedActivity", "Service UUID : ${service.uuid}")
                        for (characteristic in service.characteristics) {
                            Log.d("LedActivity", "Caractéristique UUID : ${characteristic.uuid}")
                            if (characteristic.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
                                ledCharacteristic = characteristic
                                runOnUiThread {
                                    servicesDiscovered = true
                                }
                                Log.d("LedActivity", "Caractéristique écrivable trouvée : ${characteristic.uuid}")
                                runOnUiThread {
                                    Toast.makeText(this@DeviceDetailActivity, "Caractéristique pour les LEDs trouvée", Toast.LENGTH_SHORT).show()
                                }
                                break
                            }
                        }
                        if (servicesDiscovered) break
                    }

                    if (!servicesDiscovered) {
                        Log.d("LedActivity", "Aucune caractéristique écrivable trouvée")
                        runOnUiThread {
                            Toast.makeText(this@DeviceDetailActivity, "Aucune caractéristique écrivable trouvée", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("BLE", "Service discovery failed with status $status")
                }
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("BLELED", "Characteristic written successfully: ${characteristic.uuid}")
                } else {
                    Log.e("BLELED", "Failed to write characteristic: ${characteristic.uuid}")
                }
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun disconnectDevice() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        isConnected = false
        showToast("Périphérique déconnecté")
        finish()
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun writeToLEDCharacteristic(state: LEDStateEnum) {
        val characteristic = ledCharacteristic ?: run {
            Toast.makeText(this, "Caractéristique LED non disponible", Toast.LENGTH_SHORT).show()
            return
        }

        characteristic.value = state.hex
        Log.d("CHAR", "characteristic.value : ${characteristic.value.joinToString { "0x${it.toUByte().toString(16).toUpperCase()}" }}")

        val success = bluetoothGatt?.writeCharacteristic(characteristic) ?: false

        if (success) {
            Log.d("BLE", "Écriture de la caractéristique réussie : ${state.name}")
        } else {
            Log.e("BLE", "Écriture de la caractéristique échouée")
        }
    }

    // Modification de la fonction clicbouton pour mettre à jour les états
    private fun clicbouton(numeroDeBouton: Int) {

        // Récupérer l'appui sur un bouton
        // Regarder quel bouton vient d'être actionné et incrémenter
        when (numeroDeBouton) {
            1 -> nbclicbouton1++  // Incrémenter le compteur pour le bouton 1
            2 -> nbclicbouton2++  // Incrémenter le compteur pour le bouton 2
        }

        // Calculer le nombre total de clics
        nbclictotal = nbclicbouton1 + nbclicbouton2
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun DeviceDetailScreen(
    deviceName: String,
    deviceAddress: String,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    ledStates: MutableList<Boolean>,
    writeToLEDCharacteristic: (LEDStateEnum) -> Unit,
    nbclicbouton1: Int,  // Utilisation des variables d'état
    nbclicbouton2: Int,
    nbclictotal: Int,
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

        // Affichage du nombre de clics
        Text(
            text = "Clics bouton 1: $nbclicbouton1",
            fontSize = 16.sp,
            modifier = Modifier.padding(8.dp)
        )
        Text(
            text = "Clics bouton 2: $nbclicbouton2",
            fontSize = 16.sp,
            modifier = Modifier.padding(8.dp)
        )
        Text(
            text = "Clics totaux: $nbclictotal",
            fontSize = 16.sp,
            modifier = Modifier.padding(8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Images de la LED
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            for (i in 1..3) {
                Image(
                    painter = painterResource(id = if (ledStates[i-1]) R.drawable.ledon else R.drawable.ledoff),
                    contentDescription = "LED $i",
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 24.dp)
                        .clickable {
                            ledStates[i - 1] = !ledStates[i - 1]
                            val state = when (i) {
                                1 -> LEDStateEnum.LED_1
                                2 -> LEDStateEnum.LED_2
                                3 -> LEDStateEnum.LED_3
                                else -> LEDStateEnum.NONE
                            }
                            writeToLEDCharacteristic(state)
                        }
                )
            }
        }
    }
}
