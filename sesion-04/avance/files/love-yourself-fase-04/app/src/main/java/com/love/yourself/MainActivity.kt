package com.love.yourself

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.net.Uri
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.love.yourself.lab.CapturaContinuaService
import com.love.yourself.ui.theme.LoveYourselfTheme

/**
 * Fase 04. Igual que la 03, pero ademas dibuja el resultado encima de
 * Instagram. Dos permisos: dibujar sobre otras apps, y grabar pantalla.
 *
 * El overlay atraviesa los toques, asi que Instagram se sigue usando igual.
 * Esta fase muestra; todavia no interrumpe.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoveYourselfTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Pantalla(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun Pantalla(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var estado by remember { mutableStateOf("Listo.") }
    var puedeDibujar by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    // El permiso de dibujar encima se concede en Ajustes, fuera de la app, y al
    // volver no llega ningun resultado. Sin esto la pantalla seguiria diciendo
    // que falta el permiso despues de haberlo dado.
    val duenoDeCiclo = LocalLifecycleOwner.current
    DisposableEffect(duenoDeCiclo) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                puedeDibujar = Settings.canDrawOverlays(context)
            }
        }
        duenoDeCiclo.lifecycle.addObserver(observador)
        onDispose { duenoDeCiclo.lifecycle.removeObserver(observador) }
    }

    val lanzadorConsentimiento = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        val data = resultado.data
        if (resultado.resultCode == android.app.Activity.RESULT_OK && data != null) {
            estado = "Midiendo. Abre Instagram y usalo normal.\n" +
                "Para terminar, toca Detener en la notificacion."
            val intent = Intent(context, CapturaContinuaService::class.java).apply {
                putExtra(CapturaContinuaService.EXTRA_RESULT_CODE, resultado.resultCode)
                putExtra(CapturaContinuaService.EXTRA_RESULT_DATA, data)
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
        Text("Fase 04 — medir y mostrar", style = MaterialTheme.typography.titleLarge)
        Text(estado, style = MaterialTheme.typography.bodyMedium)

        if (!puedeDibujar) {
            Button(onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.packageName)
                    )
                )
            }) {
                Text("1. Permitir dibujar sobre otras apps")
            }
            Text(
                "Sin este permiso la medicion igual corre y el CSV se escribe, " +
                    "pero no veras nada encima de Instagram.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(onClick = {
            puedeDibujar = Settings.canDrawOverlays(context)
            val faltaPermiso = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            if (faltaPermiso) {
                lanzadorNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                lanzadorConsentimiento.launch(intentDeConsentimiento(context))
            }
        }) {
            Text(if (puedeDibujar) "Empezar a medir" else "Empezar sin overlay")
        }

        Button(onClick = {
            context.startService(
                Intent(context, CapturaContinuaService::class.java)
                    .setAction(CapturaContinuaService.ACCION_DETENER)
            )
            estado = "Detenido. Saca el CSV con adb pull."
        }) {
            Text("Detener")
        }

        Text(
            "El overlay muestra el tiempo de sesion y los cortes por segundo de " +
                "los ultimos 15 s. Atraviesa los toques: Instagram se usa igual.",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "Antes de empezar anota el porcentaje de bateria. Al terminar, " +
                "anotalo de nuevo: eso y si el telefono se calento son parte del resultado.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun intentDeConsentimiento(context: Context): Intent {
    val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    return manager.createScreenCaptureIntent()
}
