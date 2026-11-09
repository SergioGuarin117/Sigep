package com.example.sigep

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

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

@Composable
fun PantallaPrincipal(nombreUsuario: String, onCerrarSesion: () -> Unit) {
    val contexto = LocalContext.current
    val repo = remember { RepositorioSigep(contexto.applicationContext) }
    val borrador = remember { BorradorPaciente() }

    var pestana by rememberSaveable { mutableStateOf(Pestana.INICIO) }
    var ruta by remember { mutableStateOf<RutaCasos>(RutaCasos.Lista) }
    var vistaCasos by rememberSaveable { mutableIntStateOf(0) }
    var mensaje by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mensaje) {
        if (mensaje != null) {
            delay(4000)
            mensaje = null
        }
    }

    fun irARegistro() {
        pestana = Pestana.CASOS
        vistaCasos = 0
        ruta = RutaCasos.Registro
    }

    fun abrirPaciente(id: Long) {
        pestana = Pestana.CASOS
        ruta = RutaCasos.Detalle(id)
    }

    fun volverCasos() {
        when (val actual = ruta) {
            is RutaCasos.Registro -> {
                if (borrador.paso > 1) {
                    borrador.paso -= 1
                } else {
                    borrador.limpiar()
                    ruta = RutaCasos.Lista
                }
            }
            is RutaCasos.Condicion -> {
                if (actual.esNuevo) {
                    repo.pacientePorId(actual.pacienteId)?.let {
                        mensaje = "${it.nombreCompleto} quedó registrado. La condición está pendiente."
                    }
                    ruta = RutaCasos.Lista
                } else {
                    ruta = RutaCasos.Detalle(actual.pacienteId)
                }
            }
            is RutaCasos.Detalle -> ruta = RutaCasos.Lista
            is RutaCasos.Lista -> Unit
        }
    }

    BackHandler(enabled = pestana == Pestana.CASOS && ruta !is RutaCasos.Lista) { volverCasos() }

    val enSubPantalla = pestana == Pestana.CASOS && ruta !is RutaCasos.Lista
    val titulo = if (pestana == Pestana.CASOS) {
        when (ruta) {
            is RutaCasos.Lista -> "Casos"
            is RutaCasos.Registro -> "Registro"
            is RutaCasos.Condicion -> "Condición"
            is RutaCasos.Detalle -> "Paciente"
        }
    } else {
        pestana.etiqueta
    }

    val tecladoVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val alertasActivas = repo.alertas.count { it.estado != EstadoAlerta.RESUELTA }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = FondoApp,
        topBar = {
            Encabezado(
                titulo = titulo,
                alertasActivas = alertasActivas,
                onAtras = if (enSubPantalla) ({ volverCasos() }) else null,
                onCampana = { pestana = Pestana.ALERTAS },
                onCerrarSesion = onCerrarSesion
            )
        },
        bottomBar = {
            if (!tecladoVisible) {
                NavigationBar(containerColor = Color.White) {
                    Pestana.entries.forEach { p ->
                        NavigationBarItem(
                            selected = pestana == p,
                            onClick = {
                                if (pestana == p && p == Pestana.CASOS) ruta = RutaCasos.Lista
                                pestana = p
                            },
                            icon = { Icon(p.icono, contentDescription = p.etiqueta) },
                            label = { Text(p.etiqueta, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AzulSigep,
                                selectedTextColor = AzulSigep,
                                indicatorColor = AzulClaro,
                                unselectedIconColor = TextoSuave,
                                unselectedTextColor = TextoSuave
                            )
                        )
                    }
                }
            }
        }
    ) { espacio ->
        Box(Modifier.padding(espacio).fillMaxSize()) {
            when (pestana) {
                Pestana.INICIO -> ContenidoInicio(
                    repo = repo,
                    nombreUsuario = nombreUsuario,
                    onIrA = { destino ->
                        if (destino == Pestana.CASOS) ruta = RutaCasos.Lista
                        pestana = destino
                    },
                    onRegistrar = { irARegistro() },
                    onAbrirPaciente = { abrirPaciente(it) }
                )

                Pestana.ALERTAS -> ContenidoAlertas(repo)

                Pestana.CASOS -> PantallaCasos(
                    repo = repo,
                    ruta = ruta,
                    onRuta = { ruta = it },
                    borrador = borrador,
                    vista = vistaCasos,
                    onVista = { vistaCasos = it },
                    mensaje = mensaje,
                    onMensaje = { mensaje = it }
                )

                Pestana.EQUIPOS -> ContenidoEquipos(repo)

                Pestana.REPORTES -> ContenidoReportes(repo)
            }
        }
    }
}
