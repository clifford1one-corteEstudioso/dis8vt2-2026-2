// package es un conjunto de datos. Sirve para ordenar el codigo
// y poder usar funciones con el mismo nombre en distintos packages
package com.love.yourself.app

// librerias
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.love.yourself.app.ui.theme.LoveyourselfTheme


// pantalla principal
class MainActivity : ComponentActivity() {
    // funcion que se ejecuta al abirse la app
    // override es cmo sobreescribir
    // fun se usa para llamar funciones
    // OnCreate es el nombre de la funcion
    // savedInstanceState = parametro
    // Bundle = tipo de paramertro
    // ? = se usa para admitir un null
    override fun onCreate(savedInstanceState: Bundle?) {
        // super se usa para hacer referencia a la clase madre
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoveyourselfTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "weeeeeeeeeeeeeeeena po $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LoveyourselfTheme {
        Greeting("march")
    }
}