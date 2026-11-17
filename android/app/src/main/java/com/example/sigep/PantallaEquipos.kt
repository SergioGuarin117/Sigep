package com.example.sigep

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ContenidoEquipos(repo: RepositorioSigep) {
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    var seleccionado by remember { mutableStateOf<String?>(null) }

    val enCampo = repo.equipos.filter { it.estado == EstadoEquipo.EN_CAMPO }
    val disponibles = repo.equipos.filter { it.estado == EstadoEquipo.DISPONIBLE }
    val cubiertos = ALBERGUES.count { a -> enCampo.any { it.albergue == a } }
    val lista = if (pestana == 0) enCampo else disponibles

    Column(Modifier.fillMaxSize()) {
        BarraContexto()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Personal y estructuras de la respuesta en territorio", color = TextoSuave, fontSize = 12.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TarjetaResumen(
                    repo.equipos.size.toString(), "Equipos totales", Modifier.weight(1f),
                    subtitulo = "${repo.equipos.sumOf { it.profesionales }} profesionales"
                )
                TarjetaResumen(
                    enCampo.size.toString(), "En campo", Modifier.weight(1f),
                    subtitulo = "${enCampo.sumOf { it.profesionales }} profesionales"
                )
                TarjetaResumen(
                    disponibles.size.toString(), "Disponibles", Modifier.weight(1f),
                    subtitulo = "${disponibles.sumOf { it.profesionales }} profesionales"
                )
            }

            Segmento(
                opciones = listOf("Desplegados · ${enCampo.size}", "Disponibles · ${disponibles.size}"),
                seleccion = pestana,
                onSeleccion = { pestana = it }
            )

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Cobertura de albergues",
                    color = TextoOscuro,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text("$cubiertos de ${ALBERGUES.size} cubiertos", color = TextoSuave, fontSize = 12.sp)
            }
            LinearProgressIndicator(
                progress = { cubiertos.toFloat() / ALBERGUES.size },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = AzulSigep,
                trackColor = AzulClaro
            )
            val sinCubrir = ALBERGUES.filter { a -> enCampo.none { it.albergue == a } }
            Text(
                if (sinCubrir.isEmpty()) "Todos los albergues tienen al menos un equipo en campo."
                else "Sin equipo en campo: ${sinCubrir.joinToString(", ")}.",
                color = if (sinCubrir.isEmpty()) TextoSuave else RojoSigep,
                fontSize = 12.sp
            )

            if (lista.isEmpty()) {
                Text(
                    if (pestana == 0) "No hay equipos desplegados." else "No hay equipos disponibles.",
                    color = TextoSuave,
                    fontSize = 13.sp
                )
            } else {
                Tarjeta(Modifier.fillMaxWidth()) {
                    lista.forEachIndexed { i, equipo ->
                        if (i > 0) HorizontalDivider(color = BordeSuave)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickableSimple { seleccionado = equipo.codigo }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AzulClaro),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Person, null, tint = AzulSigep)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "${equipo.codigo} · ${equipo.funcion}",
                                    color = TextoOscuro,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${equipo.profesionales} profesionales · ${equipo.albergue}",
                                    color = TextoSuave,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                if (equipo.estado == EstadoEquipo.EN_CAMPO) "Reasignar" else "Desplegar",
                                color = AzulSigep,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Text("Toca un equipo para desplegarlo, reasignarlo o retirarlo a reserva.", color = TextoSuave, fontSize = 11.sp)
        }
    }

    seleccionado?.let { codigo ->
        val equipo = repo.equipos.firstOrNull { it.codigo == codigo }
        if (equipo != null) {
            DialogoEquipo(
                equipo = equipo,
                onCancelar = { seleccionado = null },
                onDesplegar = { albergue ->
                    repo.actualizarEquipo(equipo.codigo, EstadoEquipo.EN_CAMPO, albergue)
                    seleccionado = null
                },
                onRetirar = {
                    repo.actualizarEquipo(equipo.codigo, EstadoEquipo.DISPONIBLE, BASE_MUNICIPAL)
                    seleccionado = null
                }
            )
        } else {
            seleccionado = null
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoEquipo(
    equipo: Equipo,
    onCancelar: () -> Unit,
    onDesplegar: (String) -> Unit,
    onRetirar: () -> Unit
) {
    var albergue by remember {
        mutableStateOf(if (equipo.albergue in ALBERGUES) equipo.albergue else "")
    }
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Color.White,
        title = { Text("${equipo.codigo} · ${equipo.funcion}", fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Albergue asignado", color = TextoOscuro, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ALBERGUES.forEach { a -> ChipSeleccion(a, albergue == a, { albergue = a }) }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = albergue.isNotBlank(),
                onClick = { onDesplegar(albergue) }
            ) {
                Text(
                    if (equipo.estado == EstadoEquipo.EN_CAMPO) "Reasignar" else "Desplegar",
                    color = if (albergue.isNotBlank()) AzulSigep else TextoSuave,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            Row {
                if (equipo.estado == EstadoEquipo.EN_CAMPO) {
                    TextButton(onClick = onRetirar) { Text("Retirar a reserva", color = RojoSigep) }
                }
                TextButton(onClick = onCancelar) { Text("Cancelar") }
            }
        }
    )
}
