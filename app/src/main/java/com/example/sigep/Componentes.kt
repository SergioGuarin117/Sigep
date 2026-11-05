package com.example.sigep

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------- Paleta ----------
val FondoApp = Color(0xFFF3F5F9)
val TextoOscuro = Color(0xFF1B2540)
val TextoSuave = Color(0xFF6B7487)
val BordeSuave = Color(0xFFE2E6EF)
val AzulClaro = Color(0xFFE8EEF8)
val RojoSigep = Color(0xFFC83D48)
val VerdeSigep = Color(0xFF2E7D55)
val VerdeClaro = Color(0xFFE5F4EB)

// ---------- Utilidades de texto y fecha ----------
private val LOCALE_ES: Locale = Locale.forLanguageTag("es-CO")

fun formatoFechaHora(ms: Long): String = SimpleDateFormat("d MMM · HH:mm", LOCALE_ES).format(Date(ms))
fun formatoHora(ms: Long): String = SimpleDateFormat("HH:mm", LOCALE_ES).format(Date(ms))
fun formatoFechaCorta(ms: Long): String = SimpleDateFormat("d MMM", LOCALE_ES).format(Date(ms))
fun formatoFechaLarga(ms: Long): String = SimpleDateFormat("d MMM yyyy · HH:mm", LOCALE_ES).format(Date(ms))

fun hace(ms: Long): String {
    val minutos = ((System.currentTimeMillis() - ms) / 60_000L).coerceAtLeast(0)
    return when {
        minutos < 1 -> "Hace un momento"
        minutos < 60 -> "Hace $minutos min"
        minutos < 60 * 24 -> "Hace ${minutos / 60} h"
        else -> "Hace ${minutos / (60 * 24)} d"
    }
}

/** Quita tildes y pasa a minúsculas para que "lucia" encuentre "Lucía". */
fun normalizar(texto: String): String =
    Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()

// ---------- Encabezado y barra de contexto ----------

@Composable
fun Encabezado(
    titulo: String,
    alertasActivas: Int,
    onAtras: (() -> Unit)?,
    onCampana: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    var menuAbierto by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AzulSigep)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onAtras != null) {
            BotonIcono(Icons.AutoMirrored.Filled.ArrowBack, "Volver", onAtras)
            Spacer(Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SIGEP · DESASTRES",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                letterSpacing = 1.sp
            )
            Text(
                text = titulo,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box {
            BotonIcono(Icons.Filled.Notifications, "Alertas", onCampana)
            if (alertasActivas > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(RojoSigep),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (alertasActivas > 9) "9+" else alertasActivas.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Box {
            BotonIcono(Icons.Filled.Settings, "Ajustes") { menuAbierto = true }
            DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                DropdownMenuItem(
                    text = { Text("Cerrar sesión") },
                    onClick = {
                        menuAbierto = false
                        onCerrarSesion()
                    }
                )
            }
        }
    }
}

@Composable
private fun BotonIcono(icono: ImageVector, descripcion: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = descripcion, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun BarraContexto() {
    val ahora = remember { System.currentTimeMillis() }
    Column(Modifier.background(Color.White)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.LocationOn, null, tint = AzulSigep, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "San Miguel · Inundación",
                color = AzulSigep,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(formatoFechaHora(ahora), color = TextoSuave, fontSize = 12.sp)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(BordeSuave))
    }
}

// ---------- Tarjetas ----------

@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val forma = RoundedCornerShape(12.dp)
    val colores = CardDefaults.cardColors(containerColor = Color.White)
    val borde = BorderStroke(1.dp, BordeSuave)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = forma, colors = colores, border = borde, content = contenido)
    } else {
        Card(modifier = modifier, shape = forma, colors = colores, border = borde, content = contenido)
    }
}

@Composable
fun TarjetaResumen(
    valor: String,
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    onClick: (() -> Unit)? = null
) {
    Tarjeta(modifier = modifier, onClick = onClick) {
        Column(Modifier.padding(12.dp)) {
            Text(valor, color = AzulSigep, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(titulo, color = TextoOscuro, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            if (subtitulo != null) {
                Text(subtitulo, color = TextoSuave, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun TituloSeccion(texto: String, accion: String? = null, onAccion: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            texto,
            color = TextoOscuro,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (accion != null) {
            Text(
                accion,
                color = AzulSigep,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onAccion)
            )
        }
    }
}

@Composable
fun BannerInfo(
    texto: String,
    icono: ImageVector = Icons.Filled.Info,
    fondo: Color = AzulClaro,
    color: Color = AzulSigep
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(fondo)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(texto, color = color, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
fun EtiquetaTriage(triage: Triage) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(triage.fondo)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            "${triage.codigo} · ${triage.etiqueta}",
            color = triage.color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun Pastilla(texto: String, color: Color, fondo: Color = color.copy(alpha = 0.12f)) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(fondo)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(texto, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun FilaDato(etiqueta: String, valor: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(etiqueta, color = TextoSuave, fontSize = 11.sp)
        Text(valor, color = TextoOscuro, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// ---------- Botones ----------

@Composable
fun BotonPrimario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = AzulSigep
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
            disabledContainerColor = color.copy(alpha = 0.35f),
            disabledContentColor = Color.White
        )
    ) {
        Text(texto, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
fun BotonTextoCentrado(texto: String, onClick: () -> Unit, color: Color = TextoOscuro) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(texto, color = color, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

// ---------- Campos de formulario ----------

@Composable
fun EtiquetaCampo(texto: String, obligatorio: Boolean) {
    Text(
        text = if (obligatorio) "$texto *" else texto,
        color = TextoOscuro,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun CampoFormulario(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    obligatorio: Boolean = false,
    error: String? = null,
    ayuda: String? = null,
    minLineas: Int = 1,
    teclado: KeyboardType = KeyboardType.Text
) {
    val apoyo = error ?: ayuda
    Column(modifier) {
        EtiquetaCampo(etiqueta, obligatorio)
        OutlinedTextField(
            value = valor,
            onValueChange = onCambio,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = TextoSuave) },
            isError = error != null,
            singleLine = minLineas == 1,
            minLines = minLineas,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(keyboardType = teclado),
            supportingText = if (apoyo != null) {
                { Text(apoyo, fontSize = 12.sp) }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                errorContainerColor = Color.White,
                focusedBorderColor = AzulSigep,
                unfocusedBorderColor = BordeSuave
            )
        )
    }
}

@Composable
fun Selector(
    etiqueta: String,
    opciones: List<String>,
    seleccion: String,
    onSeleccion: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Selecciona una opción",
    obligatorio: Boolean = false,
    error: String? = null,
    ayuda: String? = null
) {
    var abierto by remember { mutableStateOf(false) }
    val forma = RoundedCornerShape(10.dp)
    val apoyo = error ?: ayuda
    Column(modifier) {
        EtiquetaCampo(etiqueta, obligatorio)
        Box(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(forma)
                    .background(Color.White)
                    .border(1.dp, if (error != null) RojoSigep else BordeSuave, forma)
                    .clickable { abierto = true }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (seleccion.isBlank()) placeholder else seleccion,
                    color = if (seleccion.isBlank()) TextoSuave else TextoOscuro,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Filled.KeyboardArrowDown, null, tint = TextoSuave)
            }
            DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                opciones.forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(opcion) },
                        onClick = {
                            onSeleccion(opcion)
                            abierto = false
                        }
                    )
                }
            }
        }
        if (apoyo != null) {
            Text(
                apoyo,
                color = if (error != null) RojoSigep else TextoSuave,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun ChipSeleccion(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    colorSeleccion: Color = AzulSigep
) {
    val forma = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .clip(forma)
            .background(if (seleccionado) colorSeleccion else Color.White)
            .border(1.dp, if (seleccionado) colorSeleccion else BordeSuave, forma)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            texto,
            color = if (seleccionado) Color.White else TextoOscuro,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Selector segmentado tipo pestañas (Activas / Resueltas, En campo / Disponibles…). */
@Composable
fun Segmento(opciones: List<String>, seleccion: Int, onSeleccion: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, BordeSuave, RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        opciones.forEachIndexed { i, texto ->
            val activo = i == seleccion
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (activo) AzulSigep else Color.Transparent)
                    .clickable { onSeleccion(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    texto,
                    color = if (activo) Color.White else TextoOscuro,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

fun Modifier.clickableSimple(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)
