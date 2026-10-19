package com.example.sigep

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun PantallaRegistro(onVolver: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permisoRedLocal = "android.permission.ACCESS_LOCAL_NETWORK"

    val pedirPermisoRedLocal = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        val mensaje = if (concedido) {
            "Permiso concedido. Toca Crear cuenta nuevamente."
        } else {
            "Se necesita permiso de red local para conectar con SIGEP."
        }

        Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
    }
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var correoRegistro by remember { mutableStateOf("") }
    var contrasenaRegistro by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AzulSigep)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("⚕", color = Color.White, fontSize = 48.sp)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(GrisFondo)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Crear cuenta",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = apellido,
                   onValueChange = { apellido = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Apellido") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = correoRegistro,

                    onValueChange = { correoRegistro = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Correo electrónico") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    )
                )

                OutlinedTextField(
                    value = contrasenaRegistro,

                    onValueChange = { contrasenaRegistro = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (
                            nombre.isBlank() ||
                            apellido.isBlank() ||
                            correoRegistro.isBlank() ||
                            contrasenaRegistro.isBlank()
                        ) {
                            Toast.makeText(
                                context,
                                "Completa todos los campos",
                                Toast.LENGTH_SHORT
                            ).show()
                        }  else if (
                            Build.VERSION.SDK_INT >= 37 &&
                            ContextCompat.checkSelfPermission(context, permisoRedLocal) !=
                            PackageManager.PERMISSION_GRANTED)
                        {
                            pedirPermisoRedLocal.launch(permisoRedLocal)
                        } else {
                            scope.launch {
                                try {
                                    val respuesta = RetrofitClient.authApi.crearCuenta(
                                        RegistroRequest(
                                            nombre = nombre.trim(),
                                            apellido = apellido.trim(),
                                            correo = correoRegistro.trim(),
                                            contrasena = contrasenaRegistro
                                        )
                                    )

                                    if (respuesta.isSuccessful) {
                                        Toast.makeText(
                                            context,
                                            respuesta.body()?.mensaje ?: "Cuenta creada correctamente",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onVolver()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "No se pudo crear la cuenta. Revisa si el correo ya está registrado.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        e.toString(),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }, modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NegroBoton
                    )
                ) {
                    Text("Crear cuenta", color = Color.White)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onVolver,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF101A3A)
                    )
                ) {
                    Text("Volver al inicio de sesión", color = Color.White)
                }
            }
        }
}
