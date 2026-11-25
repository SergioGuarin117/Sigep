package com.example.sigep.model

/** Roles del sistema. `valor` es el texto exacto que envia el backend. */
enum class Rol(val valor: String, val etiqueta: String) {
    BRIGADISTA("brigadista", "Brigadista de campo"),
    MEDICO_TRIAGE("medico_triage", "Médico de triage"),
    ADMIN_ALBERGUE("admin_albergue", "Administrador de albergue"),
    DIRECTOR_IPS("director_ips", "Director de IPS"),
    COORDINADOR_MUNICIPAL("coordinador_municipal", "Coordinador municipal");

    companion object {
        /** Un valor desconocido cae en el rol de menor privilegio. */
        fun desdeValor(valor: String?): Rol =
            entries.firstOrNull { it.valor == valor } ?: BRIGADISTA
    }
}

/** Datos de la persona que tiene la sesion abierta. */
data class SesionUsuario(
    val token: String,
    val id: Long,
    val correo: String,
    val nombre: String,
    val apellido: String,
    val rol: Rol
)
