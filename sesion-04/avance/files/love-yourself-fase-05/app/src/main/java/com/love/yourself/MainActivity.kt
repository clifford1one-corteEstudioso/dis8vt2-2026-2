package com.love.yourself

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.love.yourself.lab.CapturaContinuaService
import com.love.yourself.lab.Config
import com.love.yourself.lab.SesionService
import com.love.yourself.ui.theme.LoveYourselfTheme

/**
 * Pantalla de configuracion. Una vez dados los dos permisos, la app trabaja
 * sola y no hay que volver aca.
 *
 * Es a proposito: una intervencion que hay que encender a mano no sirve para
 * alguien que se pierde cuarenta minutos sin darse cuenta. Quien se acuerda de
 * abrir esta pantalla ya esta siendo consciente.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoveYourselfTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Configuracion(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun Configuracion(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var puedeDibujar by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var servicioActivo by remember { mutableStateOf(servicioDeAccesibilidadActivo(context)) }
    var nota by remember { mutableStateOf("") }

    // Los dos permisos se conceden en Ajustes, fuera de la app, y al volver no
    // llega ningun resultado. Sin esto la pantalla seguiria pidiendolos.
    val duenoDeCiclo = LocalLifecycleOwner.current
    DisposableEffect(duenoDeCiclo) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                puedeDibujar = Settings.canDrawOverlays(context)
                servicioActivo = servicioDeAccesibilidadActivo(context)
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
            nota = "Midiendo cortes. El CSV queda en files/Documents/registros."
            val intent = Intent(context, CapturaContinuaService::class.java).apply {
                putExtra(CapturaContinuaService.EXTRA_RESULT_CODE, resultado.resultCode)
                putExtra(CapturaContinuaService.EXTRA_RESULT_DATA, data)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
    val lanzadorNotificaciones = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { lanzadorConsentimiento.launch(intentDeConsentimiento(context)) }

    val listo = puedeDibujar && servicioActivo

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Love Yourself", style = MaterialTheme.typography.headlineSmall)
        Text(
            if (listo) {
                "Listo. No tienes que volver aquí: la app se enciende sola cuando " +
                    "empiezas a deslizar contenido a pantalla completa."
            } else {
                "Faltan permisos para que la app pueda trabajar sola."
            },
            style = MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Text(
            if (puedeDibujar) "✓  1. Dibujar sobre otras apps" else "1. Dibujar sobre otras apps",
            style = MaterialTheme.typography.titleMedium
        )
        Text("Para mostrarte el tiempo encima de Instagram.", style = MaterialTheme.typography.bodySmall)
        if (!puedeDibujar) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + context.packageName)
                        )
                    )
                }
            ) { Text("Conceder") }
        }

        Text(
            if (servicioActivo) "✓  2. Accesibilidad" else "2. Accesibilidad",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            "Para saber cuándo empiezas y cuándo sales. No lee el contenido de " +
                "la pantalla ni guarda nada fuera de tu teléfono.",
            style = MaterialTheme.typography.bodySmall
        )
        if (!servicioActivo) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            ) { Text("Activar \"Love Yourself\"") }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Text("Cómo se comporta", style = MaterialTheme.typography.titleMedium)
        Text(
            "· Desde el primer deslizamiento: una caja discreta con el tiempo.\n" +
                "· A los ${Config.MIN_PRESENCIA} min: la caja crece.\n" +
                "· A los ${Config.MIN_DECISION} min: pantalla completa, seguir o salir.\n" +
                "Nunca bloquea. Solo devuelve la decisión.",
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Text("Modo investigación", style = MaterialTheme.typography.titleMedium)
        Text(
            "Cuenta cortes de plano leyendo la pantalla. Android exige aceptar un " +
                "diálogo cada vez, así que esto no puede encenderse solo: es para " +
                "medir, no parte de la app.",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val falta = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                if (falta) {
                    lanzadorNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    lanzadorConsentimiento.launch(intentDeConsentimiento(context))
                }
            }
        ) { Text("Medir cortes") }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                context.startService(
                    Intent(context, CapturaContinuaService::class.java)
                        .setAction(CapturaContinuaService.ACCION_DETENER)
                )
                nota = "Medición detenida."
            }
        ) { Text("Detener medición") }
        if (nota.isNotEmpty()) {
            Text(nota, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * No hay API para preguntarle al sistema si un service propio esta activo, asi
 * que se lee el ajuste donde Android guarda la lista.
 */
private fun servicioDeAccesibilidadActivo(context: Context): Boolean {
    val activos = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val propio = "${context.packageName}/${SesionService::class.java.name}"
    return activos.split(':').any { it.equals(propio, ignoreCase = true) }
}

private fun intentDeConsentimiento(context: Context): Intent {
    val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    return manager.createScreenCaptureIntent()
}
