package com.example.sigep

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class AuthRequest(
    val correo: String,
    val contrasena: String
)

data class RegistroRequest(
    val nombre: String,
    val apellido: String,
    val correo: String,
    val contrasena: String
)

data class UsuarioRespuesta(
    val id: Long,
    val correo: String,
    val rol: String,
    val nombre: String? = null,
    val apellido: String? = null
)

data class AuthRespuesta(
    val mensaje: String? = null,
    val token: String? = null,
    val usuario: UsuarioRespuesta? = null,
    val error: String? = null
)

interface AuthApi {
    @POST("api/auth/login")
    suspend fun iniciarSesion(
        @Body solicitud: AuthRequest
    ): Response<AuthRespuesta>

    @POST("api/auth/register")
    suspend fun crearCuenta(
        @Body solicitud: RegistroRequest
    ): Response<AuthRespuesta>
}