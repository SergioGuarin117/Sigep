package com.example.sigep

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContenidoAlertas(repo: RepositorioSigep) {
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    var expandida by remember { mutableStateOf<Long?>(null) }
    var creando by remember { mutableStateOf(false) }
    var porEliminar by remember { mutableStateOf<Alerta?>(null) }

    val orden = compareBy<Alerta> { it.nivel.orden }.thenByDescending { it.creadaEn }
    val activas = repo.alertas.filter { it.estado != EstadoAlerta.RESUELTA }.sortedWith(orden)
    val resueltas = repo.alertas.filter { it.estado == EstadoAlerta.RESUELTA }.sortedByDescending { it.creadaEn }
    val lista = if (pestana == 0) activas else resueltas

    Column(Modifier.fillMaxSize()) {
        BarraContexto()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Riesgos detectados y seguimiento de la respuesta", color = TextoSuave, fontSize = 12.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TarjetaResumen(
                    activas.count { it.nivel == NivelAlerta.CRITICA }.toString(), "Crítica",
                    Modifier.weight(1f)
                )
                TarjetaResumen(
                    activas.count { it.nivel == NivelAlerta.ALTA }.toString(), "Altas",
                    Modifier.weight(1f)
                )
                TarjetaResumen(
                    activas.count { it.nivel == NivelAlerta.MODERADA }.toString(), "Moderadas",
                    Modifier.weight(1f)
                )
            }

            Segmento(
                opciones = listOf("Activas · ${activas.size}", "Resueltas · ${resueltas.size}"),
                seleccion = pestana,
                onSeleccion = { pestana = it; expandida = null }
            )

            if (lista.isEmpty()) {
                Text(
                    if (pestana == 0) "No hay alertas activas." else "Aún no hay alertas resueltas.",
                    color = TextoSuave,
                    fontSize = 13.sp
                )
            }

            lista.forEach { alerta ->
                val abierta = expandida == alerta.id
                Tarjeta(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { expandida = if (abierta) null else alerta.id }
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .width(4.dp)
                                    .background(alerta.nivel.color, RoundedCornerShape(4.dp))
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    alerta.nivel.etiqueta,
                                    color = alerta.nivel.color,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    alerta.titulo,
                                    color = TextoOscuro,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${alerta.ubicacion} · ${hace(alerta.creadaEn)}",
                                    color = TextoSuave,
                                    fontSize = 12.sp
                                )
                                Text(
                                    alerta.estado.etiqueta,
                                    color = AzulSigep,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (abierta) {
                            if (alerta.estado != EstadoAlerta.RESUELTA) {
                                Text("Cambiar estado", color = TextoSuave, fontSize = 11.sp)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        EstadoAlerta.EVALUACION,
                                        EstadoAlerta.SEGUIMIENTO,
                                        EstadoAlerta.INTERVENCION
                                    ).forEach { estado ->
                                        ChipSeleccion(
                                            texto = estado.etiqueta,
                                            seleccionado = alerta.estado == estado,
                                            onClick = { repo.cambiarEstadoAlerta(alerta.id, estado) }
                                        )
                                    }
                                }
                                Row {
                                    TextButton(onClick = {
                                        repo.cambiarEstadoAlerta(alerta.id, EstadoAlerta.RESUELTA)
                                        expandida = null
                                    }) { Text("Marcar como resuelta", color = VerdeSigep) }
                                    TextButton(onClick = { porEliminar = alerta }) {
                                        Text("Eliminar", color = RojoSigep)
                                    }
                                }
                            } else {
                                Row {
                                    TextButton(onClick = {
                                        repo.cambiarEstadoAlerta(alerta.id, EstadoAlerta.EVALUACION)
                                        expandida = null
                                    }) { Text("Reabrir alerta", color = AzulSigep) }
                                    TextButton(onClick = { porEliminar = alerta }) {
                                        Text("Eliminar", color = RojoSigep)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            BotonPrimario("Nueva alerta", onClick = { creando = true })
        }
    }

    if (creando) {
        DialogoNuevaAlerta(
            onCancelar = { creando = false },
            onCrear = { nivel, titulo, ubicacion ->
                repo.agregarAlerta(nivel, titulo, ubicacion)
                pestana = 0
                creando = false
            }
        )
    }

    porEliminar?.let { alerta ->
        AlertDialog(
            onDismissRequest = { porEliminar = null },
            title = { Text("Eliminar alerta") },
            text = { Text("¿Eliminar «${alerta.titulo}»? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    repo.eliminarAlerta(alerta.id)
                    porEliminar = null
                    expandida = null
                }) { Text("Eliminar", color = RojoSigep) }
            },
            dismissButton = { TextButton(onClick = { porEliminar = null }) { Text("Cancelar") } },
            containerColor = Color.White
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoNuevaAlerta(
    onCancelar: () -> Unit,
    onCrear: (NivelAlerta, String, String) -> Unit
) {
    var nivel by remember { mutableStateOf(NivelAlerta.ALTA) }
    var titulo by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("") }
    var intento by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Color.White,
        title = { Text("Nueva alerta") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Nivel", color = TextoOscuro, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NivelAlerta.entries.forEach {
                        ChipSeleccion(it.etiqueta, nivel == it, { nivel = it }, it.color)
                    }
                }
                CampoFormulario(
                    etiqueta = "Descripción del riesgo",
                    valor = titulo,
                    onCambio = { titulo = it },
                    placeholder = "Ej. Agua no segura para consumo",
                    obligatorio = true,
                    error = if (intento && titulo.isBlank()) "Escribe una descripción" else null
                )
                Selector(
                    etiqueta = "Ubicación",
                    opciones = ALBERGUES,
                    seleccion = ubicacion,
                    onSeleccion = { ubicacion = it },
                    obligatorio = true,
                    error = if (intento && ubicacion.isBlank()) "Selecciona el albergue" else null
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                intento = true
                if (titulo.isNotBlank() && ubicacion.isNotBlank()) onCrear(nivel, titulo, ubicacion)
            }) { Text("Crear", color = AzulSigep, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
