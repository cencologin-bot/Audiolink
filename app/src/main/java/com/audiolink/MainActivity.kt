package com.audiolink

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiolink.ui.AL
import com.audiolink.ui.AlAction
import com.audiolink.ui.AudioLinkPill
import com.audiolink.ui.AudioLinkTheme
import com.audiolink.ui.CardState

class MainActivity : ComponentActivity() {

    private var micGranted by mutableStateOf(false)
    private var serviceEnabled by mutableStateOf(false)

    private val micLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { micGranted = it }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudioLinkTheme {
                HomeScreen(
                    micGranted = micGranted,
                    serviceEnabled = serviceEnabled,
                    onRequestMic = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    onOpenAccessibility = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        micGranted = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        serviceEnabled = isServiceEnabled()
    }

    private fun isServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        val me = ComponentName(this, AudioLinkService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }
}

@Composable
private fun HomeScreen(
    micGranted: Boolean,
    serviceEnabled: Boolean,
    onRequestMic: () -> Unit,
    onOpenAccessibility: () -> Unit,
) {
    var card by remember { mutableStateOf<CardState?>(null) }
    var showDisclosure by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(AL.colors.screen)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 22.dp, vertical = 28.dp)) {
            Text("Audio Link", color = AL.colors.text, fontSize = 30.sp, fontWeight = FontWeight.Medium)
            Text(
                "Escribe /audio en un campo de texto para activarlo.",
                color = AL.colors.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(28.dp))

            InfoRow(
                title = "Detección de /audio",
                value = if (serviceEnabled) "Activada"
                else "Desactivada. Sin el servicio de accesibilidad, /audio no se detecta.",
                action = if (serviceEnabled) "Ajustes" else "Activar",
                onAction = { if (serviceEnabled) onOpenAccessibility() else showDisclosure = true },
            )
            InfoRow(
                title = "Micrófono",
                value = if (micGranted) "Concedido" else "No concedido",
                action = if (micGranted) null else "Conceder",
                onAction = onRequestMic,
            )

            Spacer(Modifier.height(24.dp))
            Row {
                AlAction("Grabar", { card = CardState.RECORDING }, filled = true, dot = AL.colors.danger)
                Spacer(Modifier.width(8.dp))
                AlAction("Escuchar", { card = CardState.LISTEN })
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().imePadding(),
        ) {
            card?.let { current ->
                AudioLinkPill(
                    state = current,
                    onStateChange = { card = it },
                    onClose = { card = null },
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                )
            }
        }
    }

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            title = { Text("Permiso de accesibilidad") },
            text = {
                Text(
                    "Audio Link usa el servicio de accesibilidad de Android solo para detectar cuando " +
                        "escribes /audio en un campo de texto y colocar la tarjeta encima de ese campo.\n\n" +
                        "No guarda, registra ni envía lo que escribes, no lee conversaciones y no captura " +
                        "contraseñas. Lo activas tú en los ajustes y puedes desactivarlo cuando quieras; " +
                        "entonces /audio dejará de funcionar.",
                )
            },
            confirmButton = {
                TextButton(onClick = { showDisclosure = false; onOpenAccessibility() }) {
                    Text("Aceptar y abrir ajustes")
                }
            },
            dismissButton = { TextButton(onClick = { showDisclosure = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun InfoRow(title: String, value: String, action: String?, onAction: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = AL.colors.muted, fontSize = 12.sp)
            Text(value, color = AL.colors.text, fontSize = 15.sp)
        }
        if (action != null) AlAction(action, onAction)
    }
}
