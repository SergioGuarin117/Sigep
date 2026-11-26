package com.example.sigep.model

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

/** Usuario tal como lo ve el coordinador (GET api/usuarios). */
data class UsuarioAdmin(
    val id: Long,
    val nombre: String? = null,
    val apellido: String? = null,
    val correo: String,
    val rol: String,
    val albergue_id: Long? = null,
    val ips_id: Long? = null,
    val activo: Boolean = true
) {
    val nombreCompleto: String
        get() = listOfNotNull(nombre, apellido).joinToString(" ").ifBlank { correo }
}

data class ListaUsuariosRespuesta(
    val total: Int = 0,
    val usuarios: List<UsuarioAdmin> = emptyList()
)

/** Cuerpo de PATCH: los campos en null no se envian. */
data class CambioUsuario(
    val rol: String? = null,
    val activo: Boolean? = null
)

data class UsuarioActualizadoRespuesta(
    val mensaje: String? = null,
    val usuario: UsuarioAdmin? = null,
    val error: String? = null
)

interface UsuariosApi {
    @GET("api/usuarios")
    suspend fun listar(
        @Query("rol") rol: String? = null,
        @Query("q") busqueda: String? = null,
        @Query("limite") limite: Int = 100
    ): Response<ListaUsuariosRespuesta>

    @PATCH("api/usuarios/{id}")
    suspend fun actualizar(
        @Path("id") id: Long,
        @Body cambio: CambioUsuario
    ): Response<UsuarioActualizadoRespuesta>
}
