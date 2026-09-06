package com.love.yourself

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.love.yourself.lab.ScreenCaptureService
import com.love.yourself.ui.theme.LoveYourselfTheme

/**
 * Prueba 1. Un boton: pedir consentimiento de MediaProjection y capturar un
 * frame. Todo lo demas del proyecto viene despues de saber si esto entrega
 * pixeles o negro.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoveYourselfTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PantallaCaptura(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun PantallaCaptura(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var estado by remember { mutableStateOf("Listo. Toca capturar.") }

    val lanzadorConsentimiento = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        val data = resultado.data
        if (resultado.resultCode == android.app.Activity.RESULT_OK && data != null) {
            estado = "Permiso dado. Capturando; revisa Logcat con el tag LYSFase1."
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultado.resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
            }
            ContextCompat.startForegroundService(context, intent)
        } else {
            estado = "Permiso rechazado."
        }
    }

    val lanzadorNotificaciones = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        lanzadorConsentimiento.launch(intentDeConsentimiento(context))
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Prueba 1 - MediaProjection", style = MaterialTheme.typography.titleLarge)
        Text(estado, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = {
            val faltaPermiso = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            if (faltaPermiso) {
                lanzadorNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                lanzadorConsentimiento.launch(intentDeConsentimiento(context))
            }
        }) {
            Text("Capturar un frame")
        }
        Text(
            "El PNG queda en Android/data/com.love.yourself.fase1/files/Pictures. " +
                "La ruta exacta y el veredicto salen en Logcat.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun intentDeConsentimiento(context: Context): Intent {
    val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    return manager.createScreenCaptureIntent()
}
