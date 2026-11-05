package com.example.sigep

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val DIA_MS = 24L * 60 * 60 * 1000

private fun inicioDeDia(offsetDias: Int): Long {
    val c = Calendar.getInstance()
    c.add(Calendar.DAY_OF_YEAR, -offsetDias)
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

@Composable
fun ContenidoReportes(repo: RepositorioSigep) {
    val contexto = LocalContext.current
    var verResumen by remember { mutableStateOf(false) }

    val ahora = System.currentTimeMillis()
    val inicios = (6 downTo 0).map { inicioDeDia(it) }
    val conteoPorDia = inicios.map { inicio ->
        repo.pacientes.count { it.creadoEn >= inicio && it.creadoEn < inicio + DIA_MS }
    }
    val totalPeriodo = conteoPorDia.sum()
    val maximo = (conteoPorDia.maxOrNull() ?: 0).coerceAtLeast(1)
    val etiquetasDia = inicios.map {
        SimpleDateFormat("EEE", Locale.forLanguageTag("es-CO")).format(java.util.Date(it)).take(1).uppercase()
    }

    val activos = repo.pacientes.count { !it.cerrado }
    val cerrados = repo.pacientes.count { it.cerrado }
    val nuevas24 = repo.pacientes.count { ahora - it.creadoEn <= DIA_MS }

    Column(Modifier.fillMaxSize()) {
        BarraContexto()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Balance de situación y documentos de coordinación", color = TextoSuave, fontSize = 12.sp)

            Tarjeta(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${formatoFechaCorta(inicios.first())} – ${formatoFechaCorta(ahora)}",
                        color = TextoOscuro,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Pastilla("Últimos 7 días", AzulSigep)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TarjetaResumen(repo.pacientes.size.toString(), "Casos registrados", Modifier.weight(1f))
                TarjetaResumen(activos.toString(), "Activos al corte", Modifier.weight(1f))
                TarjetaResumen(cerrados.toString(), "Cerrados", Modifier.weight(1f))
            }

            Tarjeta(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Casos nuevos por día",
                            color = TextoOscuro,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text("$totalPeriodo en el período", color = TextoSuave, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth().height(140.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        conteoPorDia.forEachIndexed { i, n ->
                            val esHoy = i == conteoPorDia.lastIndex
                            Column(
                                Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Text(n.toString(), color = TextoOscuro, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height((6f + 90f * n / maximo).dp)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(if (esHoy) AzulSigep else Color(0xFFC3CFE8))
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    etiquetasDia[i],
                                    color = TextoSuave,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = BordeSuave)
                    Text(
                        "$nuevas24 en las últimas 24 h · Corte ${formatoFechaHora(ahora)}",
                        color = TextoSuave,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            TituloSeccion("Informes disponibles", "2 documentos")
            Tarjeta(Modifier.fillMaxWidth()) {
                FilaInforme(
                    titulo = "Informe de situación diario",
                    detalle = "Pacientes, síndromes y alertas · texto"
                ) {
                    compartir(contexto, "SIGEP · Informe de situación", resumenSituacion(repo))
                }
                HorizontalDivider(color = BordeSuave)
                FilaInforme(
                    titulo = "Cobertura y despliegue de equipos",
                    detalle = "Equipos por albergue · texto"
                ) {
                    compartir(contexto, "SIGEP · Cobertura de equipos", resumenEquipos(repo))
                }
            }

            BotonPrimario("Ver resumen de situación", onClick = { verResumen = true })
            Text(
                "Los informes no incluyen nombres de pacientes.",
                color = TextoSuave,
                fontSize = 11.sp
            )
        }
    }

    if (verResumen) {
        val texto = remember(repo.pacientes.toList(), repo.alertas.toList(), repo.equipos.toList()) {
            resumenSituacion(repo) + "\n\n" + resumenEquipos(repo)
        }
        AlertDialog(
            onDismissRequest = { verResumen = false },
            containerColor = Color.White,
            title = { Text("Resumen de situación") },
            text = {
                Text(
                    texto,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = { compartir(contexto, "SIGEP · Resumen de situación", texto) }) {
                    Text("Compartir", color = AzulSigep, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { verResumen = false }) { Text("Cerrar") } }
        )
    }
}

@Composable
private fun FilaInforme(titulo: String, detalle: String, onCompartir: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, color = TextoOscuro, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(detalle, color = TextoSuave, fontSize = 11.sp)
        }
        TextButton(onClick = onCompartir) { Text("Compartir", color = AzulSigep, fontSize = 13.sp) }
    }
}

private fun compartir(contexto: Context, asunto: String, texto: String) {
    val intento = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, asunto)
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    contexto.startActivity(Intent.createChooser(intento, "Compartir informe"))
}

private fun resumenSituacion(repo: RepositorioSigep): String {
    val ahora = System.currentTimeMillis()
    val pacientes = repo.pacientes
    val activos = pacientes.filter { !it.cerrado }
    val activas = repo.alertas.filter { it.estado != EstadoAlerta.RESUELTA }
    return buildString {
        appendLine("SIGEP · Resumen de situación")
        appendLine("San Miguel · Inundación")
        appendLine("Corte: ${formatoFechaLarga(ahora)}")
        appendLine()
        appendLine("PACIENTES")
        appendLine("- Registrados: ${pacientes.size} (activos ${activos.size}, dados de alta ${pacientes.size - activos.size})")
        appendLine("- Nuevos en 24 h: ${pacientes.count { ahora - it.creadoEn <= DIA_MS }}")
        appendLine(
            "- Triage (activos): " +
                Triage.entries.joinToString(" · ") { t -> "${t.codigo} ${activos.count { it.triage == t }}" }
        )
        appendLine("- Sin condición registrada: ${pacientes.count { it.condicion == null }}")
        appendLine("- Requieren traslado: ${activos.count { it.condicion?.requiereTraslado == true }}")
        appendLine()
        appendLine("SÍNDROMES (casos activos)")
        Sindrome.entries.forEach { s ->
            appendLine("- ${s.etiqueta}: ${activos.count { it.condicion?.sindrome == s }}")
        }
        appendLine()
        appendLine("ALERTAS")
        appendLine(
            "- Activas: ${activas.size} (" +
                NivelAlerta.entries.joinToString(", ") { n -> "${n.etiqueta} ${activas.count { it.nivel == n }}" } + ")"
        )
        append("- Resueltas: ${repo.alertas.size - activas.size}")
    }
}

private fun resumenEquipos(repo: RepositorioSigep): String {
    val enCampo = repo.equipos.filter { it.estado == EstadoEquipo.EN_CAMPO }
    val cubiertos = ALBERGUES.count { a -> enCampo.any { it.albergue == a } }
    return buildString {
        appendLine("EQUIPOS")
        appendLine("- En campo: ${enCampo.size} de ${repo.equipos.size}")
        appendLine("- Albergues cubiertos: $cubiertos de ${ALBERGUES.size}")
        ALBERGUES.forEach { a ->
            val codigos = enCampo.filter { it.albergue == a }.joinToString(", ") { it.codigo }
            appendLine("  · $a: ${codigos.ifBlank { "sin equipo" }}")
        }
        append("- Reserva: ${repo.equipos.filter { it.estado == EstadoEquipo.DISPONIBLE }.joinToString(", ") { it.codigo }.ifBlank { "ninguno" }}")
    }
}
