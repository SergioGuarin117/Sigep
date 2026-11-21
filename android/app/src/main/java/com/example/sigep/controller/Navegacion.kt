package com.example.sigep.controller

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

enum class Pestana(val etiqueta: String, val icono: ImageVector) {
    INICIO("Inicio", Icons.Filled.Home),
    ALERTAS("Alertas", Icons.Filled.Warning),
    CASOS("Casos", Icons.Filled.Person),
    EQUIPOS("Equipos", Icons.Filled.AccountBox),
    REPORTES("Reportes", Icons.AutoMirrored.Filled.List)
}

/** Sub-pantallas dentro de la pestaña "Casos". */
sealed interface RutaCasos {
    data object Lista : RutaCasos
    data object Registro : RutaCasos
    data class Condicion(val pacienteId: Long, val esNuevo: Boolean) : RutaCasos
    data class Detalle(val pacienteId: Long) : RutaCasos
}
