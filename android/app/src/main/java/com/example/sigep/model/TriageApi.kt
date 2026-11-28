package com.example.sigep.model

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

data class SignosDto(
    val frecuencia_cardiaca: Int? = null,
    val frecuencia_respiratoria: Int? = null,
    val presion_sistolica: Int? = null,
    val presion_diastolica: Int? = null,
    val temperatura: Double? = null,
    val saturacion_oxigeno: Int? = null,
    val conciencia: String? = null
)

data class MotivoDto(
    val regla: String? = null,
    val nivel: String? = null,
    val descripcion: String = ""
)

data class SugerenciaDto(
    val nivel: String? = null,
    val puntaje: Int? = null,
    val version: String? = null,
    val confianza: String? = null,
    val motivos: List<MotivoDto> = emptyList(),
    val datos_faltantes: List<String> = emptyList(),
    val avisos: List<String> = emptyList()
)

data class DecisionDto(
    val nivel_final: String? = null,
    val confirmado_por_nombre: String? = null,
    val confirmado_en: String? = null
)

/** Evaluacion de triage tal como la entrega la API. */
data class EvaluacionDto(
    val id: Long,
    val paciente_id: Long = 0,
    val evaluado_por_nombre: String? = null,
    val evaluado_en: String? = null,
    val signos_vitales: SignosDto = SignosDto(),
    val sintomas: List<String> = emptyList(),
    val sindrome: String? = null,
    val estado_clinico: String? = null,
    val requiere_traslado: Boolean = false,
    val notas: String? = null,
    val sugerencia: SugerenciaDto = SugerenciaDto(),
    val pendiente_confirmacion: Boolean = true,
    val decision: DecisionDto? = null
)

/** Cuerpo de POST api/pacientes/{id}/triage: los campos en null no se envian. */
data class EvaluacionNueva(
    val frecuencia_cardiaca: Int? = null,
    val frecuencia_respiratoria: Int? = null,
    val presion_sistolica: Int? = null,
    val presion_diastolica: Int? = null,
    val temperatura: Double? = null,
    val saturacion_oxigeno: Int? = null,
    val conciencia: String? = null,
    val sintomas: List<String>? = null,
    val sindrome: String? = null,
    val estado_clinico: String? = null,
    val requiere_traslado: Boolean? = null,
    val notas: String? = null
)

data class ConfirmarTriage(val nivel_final: String)

data class EvaluacionRespuesta(
    val mensaje: String? = null,
    val evaluacion: EvaluacionDto? = null,
    val error: String? = null
)

data class HistorialRespuesta(
    val total: Int = 0,
    val evaluaciones: List<EvaluacionDto> = emptyList()
)

interface TriageApi {
    @POST("api/pacientes/{id}/triage")
    suspend fun evaluar(
        @Path("id") pacienteId: Long,
        @Body evaluacion: EvaluacionNueva
    ): Response<EvaluacionRespuesta>

    @GET("api/pacientes/{id}/triage")
    suspend fun historial(@Path("id") pacienteId: Long): Response<HistorialRespuesta>

    @PATCH("api/triage/{id}/confirmar")
    suspend fun confirmar(
        @Path("id") evaluacionId: Long,
        @Body cuerpo: ConfirmarTriage
    ): Response<EvaluacionRespuesta>
}
