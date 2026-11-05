package com.example.sigep

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@Composable
fun ContenidoInicio(
    repo: RepositorioSigep,
    nombreUsuario: String,
    onIrA: (Pestana) -> Unit,
    onRegistrar: () -> Unit,
    onAbrirPaciente: (Long) -> Unit
) {
    val activas = repo.alertas
        .filter { it.estado != EstadoAlerta.RESUELTA }
        .sortedWith(compareBy<Alerta> { it.nivel.orden }.thenByDescending { it.creadaEn })
    val pacientesActivos = repo.pacientes.filter { !it.cerrado }
    val inmediatos = pacientesActivos.filter { it.triage == Triage.P1 }
    val enCampo = repo.equipos.count { it.estado == EstadoEquipo.EN_CAMPO }

    val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val saludo = when {
        hora < 12 -> "Buen día"
        hora < 19 -> "Buenas tardes"
        else -> "Buenas noches"
    }
    val nombre = nombreUsuario.ifBlank { "equipo" }

    Column(Modifier.fillMaxSize()) {
        BarraContexto()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Centro de coordinación · Resumen de las últimas 24 h",
                color = TextoSuave,
                fontSize = 12.sp
            )

            Tarjeta(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("$saludo, $nombre", color = TextoOscuro, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    val resumen = buildString {
                        append("Hay ${activas.size} alertas activas y ${pacientesActivos.size} pacientes en atención")
                        if (inmediatos.isNotEmpty()) append(", ${inmediatos.size} con triage inmediato")
                        append(".")
                    }
                    Text(resumen, color = TextoSuave, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }

            TituloSeccion("Resumen de situación")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TarjetaResumen(
                    valor = pacientesActivos.size.toString(),
                    titulo = "Casos activos",
                    modifier = Modifier.weight(1f),
                    onClick = { onIrA(Pestana.CASOS) }
                )
                TarjetaResumen(
                    valor = activas.size.toString(),
                    titulo = "Alertas activas",
                    modifier = Modifier.weight(1f),
                    onClick = { onIrA(Pestana.ALERTAS) }
                )
                TarjetaResumen(
                    valor = "$enCampo/${repo.equipos.size}",
                    titulo = "Equipos en campo",
                    modifier = Modifier.weight(1f),
                    onClick = { onIrA(Pestana.EQUIPOS) }
                )
            }

            TituloSeccion("Atención prioritaria", "Ver todo") { onIrA(Pestana.ALERTAS) }

            if (activas.isEmpty() && inmediatos.isEmpty()) {
                Text("Sin pendientes prioritarios.", color = TextoSuave, fontSize = 13.sp)
            }

            activas.take(2).forEach { alerta ->
                Tarjeta(Modifier.fillMaxWidth(), onClick = { onIrA(Pestana.ALERTAS) }) {
                    Row(
                        Modifier.padding(12.dp).height(IntrinsicSize.Min),
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
                            Text(alerta.titulo, color = TextoOscuro, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${alerta.ubicacion} · ${hace(alerta.creadaEn)}",
                                color = TextoSuave,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            inmediatos.take(2).forEach { paciente ->
                Tarjeta(Modifier.fillMaxWidth(), onClick = { onAbrirPaciente(paciente.id) }) {
                    Row(
                        Modifier.padding(12.dp).height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .width(4.dp)
                                .background(Triage.P1.color, RoundedCornerShape(4.dp))
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            EtiquetaTriage(Triage.P1)
                            Text(
                                paciente.nombreCompleto,
                                color = TextoOscuro,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${paciente.codigo} · ${paciente.centro} · ${hace(paciente.creadoEn)}",
                                color = TextoSuave,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            TituloSeccion("Gestión de la respuesta")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TarjetaResumen("Casos", "Seguimiento", Modifier.weight(1f), onClick = { onIrA(Pestana.CASOS) })
                TarjetaResumen("Equipos", "Despliegue", Modifier.weight(1f), onClick = { onIrA(Pestana.EQUIPOS) })
                TarjetaResumen("Reportes", "Balance diario", Modifier.weight(1f), onClick = { onIrA(Pestana.REPORTES) })
            }

            Spacer(Modifier.height(4.dp))
            BotonPrimario("Registrar afectado", onClick = onRegistrar)
            Text(
                "Datos de uso exclusivo del personal autorizado.",
                color = TextoSuave,
                fontSize = 11.sp
            )
        }
    }
}
