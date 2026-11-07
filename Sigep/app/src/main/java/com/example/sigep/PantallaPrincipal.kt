package com.example.sigep

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField

private val pestañas = listOf(
    "Inicio" to "⌂",
    "Alertas" to "⚠",
    "Casos" to "＋",
    "Equipos" to "♙",
    "Reportes" to "▥"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal() {
    var pestañaSeleccionada by remember { mutableStateOf("Inicio") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = pestañaSeleccionada,
                        color = Color.White
                    )
                },
                actions = {
                    Text("🔔", modifier = Modifier.padding(end = 16.dp))
                    Text("⚙", modifier = Modifier.padding(end = 16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulSigep
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                pestañas.forEach { (nombre, símbolo) ->
                    NavigationBarItem(
                        selected = pestañaSeleccionada == nombre,
                        onClick = { pestañaSeleccionada = nombre },
                        icon = { Text(text = símbolo, fontSize = 20.sp) },
                        label = { Text(text = nombre) }
                    )
                }
            }
        },
        containerColor = Color(0xFFF3F5F9)
    ) { espacioDisponible ->
        when (pestañaSeleccionada) {
            "Inicio" -> ContenidoInicio(
                modifier = Modifier.padding(espacioDisponible)
            )
            "Alertas" -> ContenidoAlertas(
            modifier = Modifier.padding(espacioDisponible)
            )
            "Casos" -> ContenidoCasos(
                modifier = Modifier.padding(espacioDisponible)
            )
           // "Equipos" -> ContenidoEquipos(
            //    modifier = Modifier.padding(espacioDisponible)
            //)

            else -> ContenidoPendiente(
                nombre = pestañaSeleccionada,
                modifier = Modifier.padding(espacioDisponible)
            )
        }
    }
}

@Composable
private fun ContenidoInicio(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "📍  San Miguel · Inundación",
            color = AzulSigep,
            style = MaterialTheme.typography.titleSmall
        )

        Text(
            text = "Centro de coordinación · Resumen de las últimas 24 h",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = "Resumen de situación",
            style = MaterialTheme.typography.titleMedium
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TarjetaResumen(
                valor = "38",
                título = "Casos activos",
                modifier = Modifier.weight(1f)
            )
            TarjetaResumen(
                valor = "6",
                título = "Alertas activas",
                modifier = Modifier.weight(1f)
            )
            TarjetaResumen(
                valor = "6/8",
                título = "Equipos en campo",
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "Atención prioritaria",
            style = MaterialTheme.typography.titleMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🔴  Crítica · Agua no segura para consumo",
                    color = Color(0xFF9F2D35),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Albergue La Rivera · Hace 20 min",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Text(
            text = "Gestión de la respuesta",
            style = MaterialTheme.typography.titleMedium
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TarjetaResumen(
                valor = "Casos",
                título = "Seguimiento",
                modifier = Modifier.weight(1f)
            )
            TarjetaResumen(
                valor = "Equipos",
                título = "Despliegue",
                modifier = Modifier.weight(1f)
            )
            TarjetaResumen(
                valor = "Reportes",
                título = "Balance diario",
                modifier = Modifier.weight(1f)
            )
        }
    }
}
@Composable
private fun ContenidoAlertas(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "📍  San Miguel · Inundación",
            color = AzulSigep,
            style = MaterialTheme.typography.titleSmall
        )

        Text(
            text = "Riesgos detectados y seguimiento de la respuesta",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TarjetaResumen("1", "Crítica", Modifier.weight(1f))
            TarjetaResumen("3", "Altas", Modifier.weight(1f))
            TarjetaResumen("3", "Moderadas", Modifier.weight(1f))
        }

        Text(
            text = "Activas · 6                       Resueltas · 4",
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .background(AzulSigep, RoundedCornerShape(8.dp))
                .padding(12.dp)
        )

        TarjetaAlerta(
            nivel = "Crítica",
            título = "Agua no segura para consumo",
            detalle = "Albergue La Rivera · Hace 20 min",
            estado = "En intervención",
            colorNivel = Color(0xFFC83D48)
        )

        TarjetaAlerta(
            nivel = "Alta",
            título = "Aumento de casos de diarrea",
            detalle = "El Pinar · Hace 45 min",
            estado = "En evaluación",
            colorNivel = Color(0xFFE58B36)
        )

        TarjetaAlerta(
            nivel = "Alta",
            título = "Síntomas respiratorios agrupados",
            detalle = "Las Flores · Hace 1 h",
            estado = "En seguimiento",
            colorNivel = Color(0xFFE58B36)
        )
    }
}

@Composable
private fun TarjetaAlerta(
    nivel: String,
    título: String,
    detalle: String,
    estado: String,
    colorNivel: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(4.dp)
                    .height(64.dp)
                    .background(colorNivel, RoundedCornerShape(4.dp))
            ) {}

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = nivel,
                    color = colorNivel,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = título,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = detalle,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = estado,
                    color = AzulSigep,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
private data class CasoMuestra(
    val codigo: String,
    val sindrome: String,
    val ubicacion: String,
    val estado: String
)

@Composable
private fun ContenidoCasos(modifier: Modifier = Modifier) {
    var filtro by remember { mutableStateOf("") }

    val casos = listOf(
        CasoMuestra(
            "C-054",
            "Enfermedad diarreica aguda",
            "El Pinar · 24 Jul, 08:20",
            "Nuevo"
        ),
        CasoMuestra(
            "C-063",
            "Infección respiratoria",
            "Las Flores · 24 Jul, 09:15",
            "Seguimiento"
        ),
        CasoMuestra(
            "C-071",
            "Síndrome febril",
            "La Rivera · 24 Jul, 10:30",
            "Nuevo"
        )
    )

    val casosVisibles = casos.filter { caso ->
        caso.codigo.contains(filtro, ignoreCase = true) ||
                caso.sindrome.contains(filtro, ignoreCase = true) ||
                caso.ubicacion.contains(filtro, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "📍  San Miguel · Inundación",
            color = AzulSigep,
            style = MaterialTheme.typography.titleSmall
        )

        Text(
            text = "Vigilancia epidemiológica · Registros anonimizados",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TarjetaResumen("38", "Activos", Modifier.weight(1f))
            TarjetaResumen("22", "Nuevos · 24 h", Modifier.weight(1f))
            TarjetaResumen("16", "Cerrados", Modifier.weight(1f))
        }

        OutlinedTextField(
            value = filtro,
            onValueChange = { filtro = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Buscar por código o síndrome") }
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Síndromes en vigilancia",
                    style = MaterialTheme.typography.titleSmall
                )
                Text("Diarreico agudo                                      18")
                Text("Respiratorio agudo                                12")
                Text("Febril                                                    8")
            }
        }

        Text(
            text = "Registros recientes · ${casosVisibles.size} resultados",
            style = MaterialTheme.typography.titleMedium
        )

        if (casosVisibles.isEmpty()) {
            Text(
                text = "No se encontraron casos con esa búsqueda.",
                color = Color.Gray
            )
        } else {
            casosVisibles.forEach { caso ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${caso.codigo} · ${caso.sindrome}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = caso.ubicacion,
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = caso.estado,
                            color = AzulSigep,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaResumen(
    valor: String,
    título: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = valor,
                color = AzulSigep,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = título,
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ContenidoPendiente(
    nombre: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF3F5F9))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = nombre,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Esta sección se desarrollará en el siguiente paso.",
            color = Color.Gray
        )
    }
}