package com.example.sigep.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color

/** Albergues que llegaron de la API (se guardan en el dispositivo para verlos sin conexión). */
object AlberguesCache {
    val lista = mutableStateListOf<AlbergueDto>()

    fun porNombre(nombre: String): AlbergueDto? = lista.firstOrNull { it.nombre == nombre }
    fun porId(id: Long?): AlbergueDto? = lista.firstOrNull { it.id == id }
}

private val ALBERGUES_BASE = listOf("La Rivera", "El Pinar", "Las Flores", "El Puente")

/** Nombres de los albergues activos; mientras no haya datos del servidor se usa la lista base. */
val ALBERGUES: List<String>
    get() = AlberguesCache.lista.filter { it.activo }.map { it.nombre }.ifEmpty { ALBERGUES_BASE }
val TIPOS_SANGRE = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-", "Desconocido")
val SINTOMAS = listOf(
    "Fiebre", "Diarrea", "Vómito", "Tos", "Dificultad para respirar",
    "Dolor", "Herida visible", "Deshidratación", "Erupción en piel", "Mareo"
)
val NIVELES_CONCIENCIA = listOf("Alerta", "Responde a la voz", "Responde al dolor", "Inconsciente")

/** Nivel de triage (escala del servidor). Lo sugiere el sistema y lo confirma un médico. */
enum class Triage(
    val valor: String,
    val etiqueta: String,
    val descripcion: String,
    val mensaje: String,
    val color: Color,
    val fondo: Color
) {
    ROJO(
        "rojo", "Inmediata", "Atención o evaluación inmediata",
        "Atención inmediata por el personal sanitario.",
        Color(0xFFC83D48), Color(0xFFFBE9EA)
    ),
    AMARILLO(
        "amarillo", "Prioritaria", "Requiere atención prioritaria",
        "Atención sin esperas prolongadas.",
        Color(0xFFB8741A), Color(0xFFFDF0DE)
    ),
    VERDE(
        "verde", "No urgente", "Puede esperar según valoración del personal",
        "Puede esperar valoración del personal.",
        Color(0xFF2E7D55), Color(0xFFE5F4EB)
    ),
    NEGRO(
        "negro", "Sin signos vitales", "Sin signos vitales o fallecido",
        "Verificar y atender según protocolo.",
        Color(0xFF2B2B2B), Color(0xFFE6E6E6)
    );

    /** Texto corto para etiquetas: "Rojo · Inmediata". */
    val titulo: String get() = "${valor.replaceFirstChar { it.uppercase() }} · $etiqueta"

    companion object {
        fun desdeValor(valor: String?): Triage? = entries.firstOrNull { it.valor == valor }
    }
}

enum class Sindrome(val valor: String, val etiqueta: String) {
    GASTROINTESTINAL("gastrointestinal", "Diarreico agudo"),
    RESPIRATORIO("respiratorio", "Respiratorio agudo"),
    FEBRIL("febril", "Febril"),
    TRAUMA("trauma", "Trauma o lesión"),
    OTRO("otro", "Otro / sin clasificar");

    companion object {
        fun desdeValor(valor: String?): Sindrome? = entries.firstOrNull { it.valor == valor }
    }
}

enum class EstadoClinico(val valor: String, val etiqueta: String, val color: Color) {
    ESTABLE("estable", "Estable", Color(0xFF2E7D55)),
    OBSERVACION("observacion", "En observación", Color(0xFF304A78)),
    GRAVE("grave", "Grave", Color(0xFFB8741A)),
    CRITICO("critico", "Crítico", Color(0xFFC83D48));

    companion object {
        fun desdeValor(valor: String?): EstadoClinico? = entries.firstOrNull { it.valor == valor }
    }
}

/** Valores que el servidor espera para cada nivel de conciencia (mismo orden que NIVELES_CONCIENCIA). */
val VALORES_CONCIENCIA = listOf("alerta", "voz", "dolor", "inconsciente")

fun etiquetaConciencia(valor: String?): String =
    VALORES_CONCIENCIA.indexOf(valor).let { if (it >= 0) NIVELES_CONCIENCIA[it] else "Sin dato" }

/** Resumen de la última evaluación de triage del paciente (lo calcula y guarda el servidor). */
data class ResumenTriage(
    val evaluacionId: Long,
    val sugerido: Triage,
    val confirmado: Triage?,
    val sindrome: Sindrome?,
    val estado: EstadoClinico?,
    val requiereTraslado: Boolean,
    val evaluadoEn: Long
) {
    /** El nivel que vale: el confirmado por el médico o, mientras tanto, el sugerido. */
    val vigente: Triage get() = confirmado ?: sugerido
}

data class Paciente(
    val id: Long,
    val numero: Int,
    val nombre: String,
    val apellido: String,
    val tipoSangre: String,
    val centro: String,
    val observaciones: String,
    val creadoEn: Long,
    val cerrado: Boolean = false,
    val gestante: Boolean = false,
    val condicionesPrevias: String = "",
    val triage: ResumenTriage? = null
) {
    val codigo: String get() = "P-%03d".format(numero)
    val codigoCaso: String get() = "C-%03d".format(numero)
    val nombreCompleto: String get() = "$nombre $apellido"
    val nivel: Triage? get() = triage?.vigente
    val porConfirmar: Boolean get() = triage != null && triage.confirmado == null
}

enum class NivelAlerta(val etiqueta: String, val orden: Int, val color: Color) {
    CRITICA("Crítica", 0, Color(0xFFC83D48)),
    ALTA("Alta", 1, Color(0xFFE58B36)),
    MODERADA("Moderada", 2, Color(0xFFC9A227))
}

enum class EstadoAlerta(val etiqueta: String) {
    EVALUACION("En evaluación"),
    SEGUIMIENTO("En seguimiento"),
    INTERVENCION("En intervención"),
    RESUELTA("Resuelta")
}

data class Alerta(
    val id: Long,
    val nivel: NivelAlerta,
    val titulo: String,
    val ubicacion: String,
    val creadaEn: Long,
    val estado: EstadoAlerta
)

enum class EstadoEquipo(val etiqueta: String) {
    EN_CAMPO("En campo"),
    DISPONIBLE("Disponible")
}

const val BASE_MUNICIPAL = "Base municipal"

data class Equipo(
    val codigo: String,
    val funcion: String,
    val profesionales: Int,
    val albergue: String,
    val estado: EstadoEquipo
)
