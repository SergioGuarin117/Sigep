package com.example.sigep

import com.example.sigep.view.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.ComponentActivity
import android.os.Bundle
import android.widget.Toast.makeText
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.offset



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = AzulSigep,
                    onPrimary = Color.White,
                    secondary = AzulSigep,
                    tertiary = AzulSigep
                )
            ) {
                AplicacionSIGEP()
            }
        }
    }
}

@Composable
fun AplicacionSIGEP() {
    var mostrarRegistro by rememberSaveable { mutableStateOf(false) }
    var sesionIniciada by rememberSaveable { mutableStateOf(false) }
    var nombreUsuario by rememberSaveable { mutableStateOf("") }

    when {
        sesionIniciada -> PantallaPrincipal(
            nombreUsuario = nombreUsuario,
            onCerrarSesion = {
                sesionIniciada = false
                mostrarRegistro = false
                nombreUsuario = ""
            }
        )

        mostrarRegistro -> PantallaRegistro(
            onVolver = { mostrarRegistro = false },
            onRegistroExitoso = { nombre ->
                nombreUsuario = nombre
                mostrarRegistro = false
                sesionIniciada = true
            }
        )

        else -> PantallaLogin(
            onCrearCuenta = { mostrarRegistro = true },
            onLoginExitoso = { nombre ->
                nombreUsuario = nombre
                sesionIniciada = true
            }
        )
    }
}
