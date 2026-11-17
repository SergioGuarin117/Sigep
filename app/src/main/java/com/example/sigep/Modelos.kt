package com.example.sigep

import androidx.compose.ui.graphics.Color

val ALBERGUES = listOf("La Rivera", "El Pinar", "Las Flores", "El Puente")
val TIPOS_SANGRE = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-", "Desconocido")
val SINTOMAS = listOf(
    "Fiebre", "Diarrea", "Vómito", "Tos", "Dificultad para respirar",
    "Dolor", "Herida visible", "Deshidratación", "Erupción en piel", "Mareo"
)
val NIVELES_CONCIENCIA = listOf("Alerta", "Responde a la voz", "Responde al dolor", "Inconsciente")

enum class Triage(
    val codigo: String,
    val etiqueta: String,
    val descripcion: String,
    val mensaje: String,
    val color: Color,
    val fondo: Color
) {
    P1(
        "P1", "Inmediata", "Atención o evaluación inmediata",
        "Atención inmediata por el personal sanitario.",
        Color(0xFFC83D48), Color(0xFFFBE9EA)
    ),
    P2(
        "P2", "Prioritaria", "Requiere atención prioritaria",
        "Atención sin esperas prolongadas.",
        Color(0xFFB8741A), Color(0xFFFDF0DE)
    ),
    P3(
        "P3", "No urgente", "Puede esperar según valoración del personal",
        "Puede esperar valoración del personal.",
        Color(0xFF2E7D55), Color(0xFFE5F4EB)
    )
}

enum class Sindrome(val etiqueta: String) {
    DIARREICO("Diarreico agudo"),
    RESPIRATORIO("Respiratorio agudo"),
    FEBRIL("Febril"),
    TRAUMA("Trauma o lesión"),
    OTRO("Otro / sin clasificar")
}

enum class EstadoClinico(val etiqueta: String, val color: Color) {
    ESTABLE("Estable", Color(0xFF2E7D55)),
    OBSERVACION("En observación", Color(0xFF304A78)),
    GRAVE("Grave", Color(0xFFB8741A)),
    CRITICO("Crítico", Color(0xFFC83D48))
}

data class Condicion(
    val sindrome: Sindrome,
    val sintomas: List<String>,
    val temperatura: Double?,
    val conciencia: String,
    val requiereTraslado: Boolean,
    val gestante: Boolean,
    val condicionesPrevias: String,
    val estado: EstadoClinico,
    val notas: String,
    val actualizadoEn: Long
)

data class Paciente(
    val id: Long,
    val numero: Int,
    val nombre: String,
    val apellido: String,
    val tipoSangre: String,
    val centro: String,
    val observaciones: String,
    val triage: Triage,
    val creadoEn: Long,
    val cerrado: Boolean = false,
    val condicion: Condicion? = null
) {
    val codigo: String get() = "P-%03d".format(numero)
    val codigoCaso: String get() = "C-%03d".format(numero)
    val nombreCompleto: String get() = "$nombre $apellido"
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
