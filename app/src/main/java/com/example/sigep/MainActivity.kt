package com.example.sigep

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


val AzulSigep = Color(0xFF304A78)
val GrisFondo = Color(0xFFE0E0E0)
val NegroBoton = Color(0xFF292929)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                AplicacionSIGEP()
            }
        }
    }
}

@Composable
fun AplicacionSIGEP() {
    var mostrarRegistro by remember { mutableStateOf(false) }

    if (mostrarRegistro) {
        PantallaRegistro(onVolver = { mostrarRegistro = false })
    } else {
        PantallaLogin(onCrearCuenta = { mostrarRegistro = true })
    }
}

@Composable
fun PantallaLogin(onCrearCuenta: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AzulSigep)
    ) {
        // Encabezado azul con símbolo médico
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚕",
                modifier = Modifier.offset(y = 18.dp),
                color = Color.White,
                fontSize = 58.sp
            )
        }

        // Área principal del formulario
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(GrisFondo)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Correo electrónico",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = AzulSigep,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Contraseña",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = contrasena,
                onValueChange = { contrasena = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = AzulSigep,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    when {
                        correo.isBlank() -> {
                            Toast.makeText(
                                context,
                                "Escribe tu correo electrónico",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        !android.util.Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches() -> {
                            Toast.makeText(
                                context,
                                "Escribe un correo electrónico válido",
                                Toast.LENGTH_SHORT
                            ).show()
                        }


                        contrasena.isBlank() -> {
                            Toast.makeText(
                                context,
                                "Escribe tu contraseña",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        else -> {
                            Toast.makeText(
                                context,
                                "Campos completos. El acceso se conectará más adelante",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NegroBoton
                )
            ) {
                Text("Acceder", color = Color.White)
            }

            Text(
                text = "Olvidé la contraseña",
                color = Color.DarkGray
            )
        }

        // Pie azul con botón y línea decorativa
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
            ) {
                val linea = Path().apply {
                    moveTo(0f, size.height / 2)
                    lineTo(size.width * 0.12f, size.height / 2)
                    lineTo(size.width * 0.15f, size.height * 0.15f)
                    lineTo(size.width * 0.18f, size.height * 0.85f)
                    lineTo(size.width * 0.21f, size.height / 2)
                    lineTo(size.width, size.height / 2)
                }

                drawPath(
                    path = linea,
                    color = Color.LightGray,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Button(
                onClick = onCrearCuenta,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .height(40.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF101A3A)
                )
            ) {
                Text(
                    text = "Crear cuenta",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}