package com.love.yourself

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
 * Prueba 1 / 1.5. Dos botones sobre el mismo camino de codigo:
 *
 *  - Control (0 s): captura esta misma app. Solo verifica que la cadena
 *    permiso -> VirtualDisplay -> PNG este sana.
 *  - Diferido (5 s): da tiempo de abrir Instagram. Este es el que responde
 *    si la ventana de un reel esta marcada FLAG_SECURE.
 *
 * Como solo cambia el retraso, si el control sale con color y el diferido sale
 * negro, la diferencia no es un bug del codigo: es Instagram bloqueando.
 */
private const val RETRASO_DIFERIDO_MS = 5_000L
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
    var estado by remember { mutableStateOf("Listo. Elige un modo.") }
    // Que retraso usar cuando vuelva el dialogo de consentimiento.
    var retrasoPendiente by remember { mutableStateOf(0L) }

    val lanzadorConsentimiento = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        val data = resultado.data
        if (resultado.resultCode == android.app.Activity.RESULT_OK && data != null) {
            estado = if (retrasoPendiente > 0L) {
                "Permiso dado. ABRE INSTAGRAM YA: capturo en ${retrasoPendiente / 1000} s."
            } else {
                "Permiso dado. Capturando esta app; revisa Logcat con el tag LYSFase1."
            }
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultado.resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
                putExtra(ScreenCaptureService.EXTRA_RETRASO_MS, retrasoPendiente)
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

    // El dialogo de consentimiento es de un solo uso: hay que pedirlo en cada
    // captura, asi que ambos botones pasan por aqui.
    fun pedirCaptura(retraso: Long) {
        retrasoPendiente = retraso
        val faltaPermiso = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (faltaPermiso) {
            lanzadorNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            lanzadorConsentimiento.launch(intentDeConsentimiento(context))
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Prueba 1.5 - MediaProjection", style = MaterialTheme.typography.titleLarge)
        Text(estado, style = MaterialTheme.typography.bodyMedium)

        Button(onClick = { pedirCaptura(0L) }) {
            Text("Control: capturar ahora")
        }
        Text(
            "Captura esta misma app. Confirma que la tuberia funciona.",
            style = MaterialTheme.typography.bodySmall
        )

        Button(onClick = { pedirCaptura(RETRASO_DIFERIDO_MS) }) {
            Text("Instagram: capturar en 5 s")
        }
        Text(
            "Acepta el permiso y abre un reel antes de que se acabe la cuenta.",
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            "Los PNG quedan en Android/data/com.love.yourself.fase1/files/Pictures " +
                "como frame-control-... y frame-diferido-.... El veredicto sale en " +
                "Logcat con el tag LYSFase1.",
            style = MaterialTheme.typography.bodySmall
        )

        Button(onClick = {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }) {
            Text("Fase 02: abrir Accesibilidad")
        }
        Text(
            "Activa ahi \"LYS Fase 02a\" (guarda a archivo) y/o \"LYS Fase 02b\" " +
                "(en vivo por Logcat). Se pueden usar juntas.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun intentDeConsentimiento(context: Context): Intent {
    val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    return manager.createScreenCaptureIntent()
}
