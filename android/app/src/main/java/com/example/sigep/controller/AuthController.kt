package com.example.sigep.controller

import com.example.sigep.model.AuthRequest
import com.example.sigep.model.RegistroRequest
import com.example.sigep.model.RetrofitClient

/** Resultado de intentar iniciar sesion. La vista decide como mostrarlo. */
sealed interface ResultadoLogin {
    data class Exito(val nombre: String) : ResultadoLogin
    data object CredencialesInvalidas : ResultadoLogin
    data class ErrorServidor(val codigo: Int) : ResultadoLogin
    data object SinConexion : ResultadoLogin
}

/** Resultado de intentar crear una cuenta. */
sealed interface ResultadoRegistro {
    data class Exito(val mensaje: String?) : ResultadoRegistro
    data object Rechazado : ResultadoRegistro
    data class Falla(val detalle: String) : ResultadoRegistro
}

/** Controlador de autenticacion: habla con el modelo (API) y devuelve resultados. */
object AuthController {

    suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoLogin {
        return try {
            val respuesta = RetrofitClient.authApi.iniciarSesion(
                AuthRequest(correo = correo.trim(), contrasena = contrasena)
            )
            when {
                respuesta.isSuccessful -> {
                    val nombreApi = respuesta.body()?.usuario?.nombre
                    ResultadoLogin.Exito(
                        nombreApi?.takeIf { it.isNotBlank() }
                            ?: correo.trim().substringBefore("@")
                    )
                }
                respuesta.code() == 401 -> ResultadoLogin.CredencialesInvalidas
                else -> ResultadoLogin.ErrorServidor(respuesta.code())
            }
        } catch (e: Exception) {
            ResultadoLogin.SinConexion
        }
    }

    suspend fun crearCuenta(
        nombre: String,
        apellido: String,
        correo: String,
        contrasena: String
    ): ResultadoRegistro {
        return try {
            val respuesta = RetrofitClient.authApi.crearCuenta(
                RegistroRequest(
                    nombre = nombre.trim(),
                    apellido = apellido.trim(),
                    correo = correo.trim(),
                    contrasena = contrasena
                )
            )
            if (respuesta.isSuccessful) {
                ResultadoRegistro.Exito(respuesta.body()?.mensaje)
            } else {
                ResultadoRegistro.Rechazado
            }
        } catch (e: Exception) {
            ResultadoRegistro.Falla(e.toString())
        }
    }
}
