package com.example.sigep.controller

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.sigep.model.Rol

enum class Pestana(val etiqueta: String, val icono: ImageVector) {
    INICIO("Inicio", Icons.Filled.Home),
    ALERTAS("Alertas", Icons.Filled.Warning),
    CASOS("Casos", Icons.Filled.Person),
    EQUIPOS("Equipos", Icons.Filled.AccountBox),
    REPORTES("Reportes", Icons.AutoMirrored.Filled.List),
    USUARIOS("Usuarios", Icons.Filled.Settings);

    companion object {
        /** Pestañas que ve cada rol (maximo 5 para que quepan en la barra inferior). */
        fun paraRol(rol: Rol): List<Pestana> = when (rol) {
            Rol.BRIGADISTA, Rol.MEDICO_TRIAGE -> listOf(INICIO, ALERTAS, CASOS)
            Rol.ADMIN_ALBERGUE -> listOf(INICIO, ALERTAS, CASOS, EQUIPOS, REPORTES)
            Rol.DIRECTOR_IPS -> listOf(INICIO, ALERTAS, EQUIPOS, REPORTES)
            Rol.COORDINADOR_MUNICIPAL -> listOf(INICIO, ALERTAS, CASOS, REPORTES, USUARIOS)
        }
    }
}

/** Sub-pantallas dentro de la pestaña "Casos". */
sealed interface RutaCasos {
    data object Lista : RutaCasos
    data object Registro : RutaCasos
    data class Condicion(val pacienteId: Long, val esNuevo: Boolean) : RutaCasos
    data class Detalle(val pacienteId: Long) : RutaCasos
}
