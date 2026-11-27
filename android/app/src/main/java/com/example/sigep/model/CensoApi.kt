package com.example.sigep.model

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Albergue tal como lo entrega GET api/albergues. */
data class AlbergueDto(
    val id: Long,
    val nombre: String,
    val direccion: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val capacidad_personas: Int = 0,
    val personas_alojadas: Int = 0,
    val ocupacion_porcentaje: Int? = null,
    val activo: Boolean = true
)

data class ListaAlberguesRespuesta(
    val albergues: List<AlbergueDto> = emptyList()
)

interface AlberguesApi {
    @GET("api/albergues")
    suspend fun listar(): Response<ListaAlberguesRespuesta>
}

/** Paciente tal como lo entrega la API de censo. El codigo viene como "P-001". */
data class PacienteDto(
    val id: Long,
    val codigo: String? = null,
    val nombres: String,
    val apellidos: String,
    val tipo_sangre: String? = null,
    val observaciones: String? = null,
    val albergue_id: Long? = null,
    val cerrado: Boolean = false,
    val creado_en: String? = null
)

data class ListaPacientesRespuesta(
    val total: Int = 0,
    val pacientes: List<PacienteDto> = emptyList()
)

/** Cuerpo de POST api/pacientes: los campos en null no se envian. */
data class PacienteNuevo(
    val nombres: String,
    val apellidos: String,
    val tipo_sangre: String? = null,
    val observaciones: String? = null,
    val albergue_id: Long? = null
)

data class CambioPaciente(val cerrado: Boolean)

data class PacienteRespuesta(
    val mensaje: String? = null,
    val paciente: PacienteDto? = null,
    val error: String? = null
)

interface PacientesApi {
    @GET("api/pacientes")
    suspend fun listar(
        @Query("limite") limite: Int = 200,
        @Query("desplazamiento") desplazamiento: Int = 0
    ): Response<ListaPacientesRespuesta>

    @POST("api/pacientes")
    suspend fun crear(@Body paciente: PacienteNuevo): Response<PacienteRespuesta>

    @PATCH("api/pacientes/{id}")
    suspend fun actualizar(
        @Path("id") id: Long,
        @Body cambio: CambioPaciente
    ): Response<PacienteRespuesta>
}
