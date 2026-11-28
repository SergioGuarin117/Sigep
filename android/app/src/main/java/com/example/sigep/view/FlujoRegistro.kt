package com.example.sigep.view

import com.example.sigep.model.*
import com.example.sigep.controller.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Datos del formulario de registro; viven fuera de los pasos para no perderse al ir y volver. */
class BorradorPaciente {
    var paso by mutableIntStateOf(1)
    var centro by mutableStateOf("")
    var nombre by mutableStateOf("")
    var apellido by mutableStateOf("")
    var tipoSangre by mutableStateOf("")
    var observaciones by mutableStateOf("")
    var confirmado by mutableStateOf(false)

    fun limpiar() {
        paso = 1
        centro = ""
        nombre = ""
        apellido = ""
        tipoSangre = ""
        observaciones = ""
        confirmado = false
    }
}

@Composable
private fun IndicadorPasos(paso: Int) {
    val etiquetas = listOf("1. Datos", "2. Observaciones", "3. Resumen")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        etiquetas.forEachIndexed { i, etiqueta ->
            val actual = i == paso - 1
            Column(Modifier.weight(1f)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i < paso) AzulSigep else BordeSuave)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    etiqueta,
                    fontSize = 11.sp,
                    color = if (actual) AzulSigep else TextoSuave,
                    fontWeight = if (actual) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun VistaRegistro(
    borrador: BorradorPaciente,
    repo: RepositorioSigep,
    onCancelar: () -> Unit,
    onRegistrado: (Paciente) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        BarraContexto()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IndicadorPasos(borrador.paso)
            Text(
                "Paso ${borrador.paso} de 3 · Registro de paciente",
                color = TextoSuave,
                fontSize = 12.sp
            )
            when (borrador.paso) {
                1 -> PasoDatos(borrador, onCancelar)
                2 -> PasoObservaciones(borrador)
                else -> PasoResumen(borrador, repo, onRegistrado)
            }
        }
    }
}

// ---------------------------------------------------------------- Paso 1

@Composable
private fun PasoDatos(borrador: BorradorPaciente, onCancelar: () -> Unit) {
    var intento by remember { mutableStateOf(false) }

    Text("Datos del paciente", color = TextoOscuro, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text(
        "Ingresa los datos de la persona que llega al centro de acogida.",
        color = TextoSuave,
        fontSize = 13.sp
    )

    Tarjeta(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Selector(
                etiqueta = "Centro de acogida",
                opciones = ALBERGUES,
                seleccion = borrador.centro,
                onSeleccion = { borrador.centro = it },
                obligatorio = true,
                error = if (intento && borrador.centro.isBlank()) "Selecciona el centro" else null
            )
            CampoFormulario(
                etiqueta = "Nombre",
                valor = borrador.nombre,
                onCambio = { borrador.nombre = it },
                placeholder = "Ej. Lucía",
                obligatorio = true,
                error = if (intento && borrador.nombre.isBlank()) "Escribe el nombre" else null
            )
            CampoFormulario(
                etiqueta = "Apellido",
                valor = borrador.apellido,
                onCambio = { borrador.apellido = it },
                placeholder = "Ej. Mendoza",
                obligatorio = true,
                error = if (intento && borrador.apellido.isBlank()) "Escribe el apellido" else null
            )
            Selector(
                etiqueta = "Tipo de sangre",
                opciones = TIPOS_SANGRE,
                seleccion = borrador.tipoSangre,
                onSeleccion = { borrador.tipoSangre = it },
                obligatorio = true,
                error = if (intento && borrador.tipoSangre.isBlank()) "Selecciona el tipo de sangre" else null,
                ayuda = "Si no se conoce, elige «Desconocido»."
            )
        }
    }

    BannerInfo(
        "Acceso solo para personal autorizado. Registra únicamente la información necesaria para la atención.",
        icono = Icons.Filled.Lock
    )
    Text("* Campos obligatorios", color = TextoSuave, fontSize = 11.sp)

    BotonPrimario("Continuar a observaciones →", onClick = {
        intento = true
        if (borrador.centro.isNotBlank() && borrador.nombre.isNotBlank() &&
            borrador.apellido.isNotBlank() && borrador.tipoSangre.isNotBlank()
        ) {
            borrador.paso = 2
        }
    })
    BotonTextoCentrado("Cancelar registro", onCancelar)
}

// ---------------------------------------------------------------- Paso 2

@Composable
private fun PasoObservaciones(borrador: BorradorPaciente) {
    Text("Observaciones", color = TextoOscuro, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text(
        "${borrador.nombre} ${borrador.apellido} · ${borrador.tipoSangre} · Centro de acogida ${borrador.centro}",
        color = TextoSuave,
        fontSize = 13.sp
    )

    CampoFormulario(
        etiqueta = "Observaciones",
        valor = borrador.observaciones,
        onCambio = { borrador.observaciones = it },
        placeholder = "Ej. Refiere dolor en el tobillo derecho. Llega acompañada.",
        minLineas = 4,
        ayuda = "Escribe solo información relevante; no escribas diagnósticos."
    )

    BannerInfo("El nivel de triage se calcula al registrar la evaluación clínica, después del registro. Lo sugiere el sistema y lo confirma un médico.")

    BotonPrimario("Revisar registro →", onClick = { borrador.paso = 3 })
    BotonTextoCentrado("Volver a datos", { borrador.paso = 1 })
}

// ---------------------------------------------------------------- Paso 3

@Composable
private fun PasoResumen(
    borrador: BorradorPaciente,
    repo: RepositorioSigep,
    onRegistrado: (Paciente) -> Unit
) {
    val alcance = rememberCoroutineScope()
    var enviando by remember { mutableStateOf(false) }
    var errorEnvio by remember { mutableStateOf<String?>(null) }

    Text("Resumen del registro", color = TextoOscuro, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text("Revisa la información antes de confirmar.", color = TextoSuave, fontSize = 13.sp)

    Tarjeta(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            EncabezadoResumen("Datos del paciente") { borrador.paso = 1 }
            FilaDato("Nombre", borrador.nombre.trim())
            FilaDato("Apellido", borrador.apellido.trim())
            FilaDato("Tipo de sangre", borrador.tipoSangre)
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = BordeSuave)
            EncabezadoResumen("Observaciones") { borrador.paso = 2 }
            Text(
                borrador.observaciones.trim().ifBlank { "Sin observaciones" },
                color = TextoOscuro,
                fontSize = 14.sp
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = BordeSuave)
            FilaDato("Centro de acogida", "${borrador.centro} · San Miguel")
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { borrador.confirmado = !borrador.confirmado },
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = borrador.confirmado,
            onCheckedChange = { borrador.confirmado = it },
            colors = CheckboxDefaults.colors(checkedColor = AzulSigep)
        )
        Text(
            "Confirmo que los datos son correctos y corresponden a la persona atendida.",
            color = TextoOscuro,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
    }

    errorEnvio?.let { Text(it, color = RojoSigep, fontSize = 13.sp) }

    BotonPrimario(
        if (enviando) "Registrando…" else "Confirmar registro ✓",
        enabled = borrador.confirmado && !enviando,
        onClick = {
            run {
                alcance.launch {
                    enviando = true
                    errorEnvio = null
                    val resultado = CensoController.registrarPaciente(
                        repo = repo,
                        nombre = borrador.nombre,
                        apellido = borrador.apellido,
                        tipoSangre = borrador.tipoSangre,
                        centro = borrador.centro,
                        observaciones = borrador.observaciones
                    )
                    when (resultado) {
                        is ResultadoPaciente.Exito -> {
                            borrador.limpiar()
                            onRegistrado(resultado.paciente)
                        }
                        is ResultadoPaciente.Falla -> errorEnvio = resultado.mensaje
                    }
                    enviando = false
                }
            }
        }
    )
    BotonTextoCentrado("Volver a observaciones", { if (!enviando) borrador.paso = 2 })
}

@Composable
private fun EncabezadoResumen(titulo: String, onEditar: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            titulo,
            color = TextoOscuro,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onEditar) { Text("Editar", color = AzulSigep, fontSize = 13.sp) }
    }
}

// ---------------------------------------------------------------- Condición

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormularioCondicion(
    paciente: Paciente,
    esNuevo: Boolean,
    repo: RepositorioSigep,
    onGuardado: () -> Unit,
    onDespues: () -> Unit
) {
    val previa = paciente.triage
    val alcance = rememberCoroutineScope()

    var sindrome by remember { mutableStateOf(previa?.sindrome) }
    var sintomas by remember { mutableStateOf(emptyList<String>()) }
    var temperatura by remember { mutableStateOf("") }
    var frecCardiaca by remember { mutableStateOf("") }
    var frecResp by remember { mutableStateOf("") }
    var presionSis by remember { mutableStateOf("") }
    var presionDia by remember { mutableStateOf("") }
    var saturacion by remember { mutableStateOf("") }
    var conciencia by remember { mutableStateOf(NIVELES_CONCIENCIA.first()) }
    var requiereTraslado by remember { mutableStateOf(previa?.requiereTraslado ?: false) }
    var gestante by remember { mutableStateOf(paciente.gestante) }
    var condicionesPrevias by remember { mutableStateOf(paciente.condicionesPrevias) }
    var estado by remember { mutableStateOf(previa?.estado) }
    var notas by remember { mutableStateOf("") }
    var intento by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }
    var errorEnvio by remember { mutableStateOf<String?>(null) }

    val tempNumero = temperatura.replace(',', '.').toDoubleOrNull()
    val errorTemperatura = when {
        temperatura.isBlank() -> null
        tempNumero == null || tempNumero < 25.0 || tempNumero > 45.0 -> "Ingresa un valor entre 25 y 45 °C"
        else -> null
    }
    val fc = frecCardiaca.toIntOrNull()
    val fr = frecResp.toIntOrNull()
    val pas = presionSis.toIntOrNull()
    val pad = presionDia.toIntOrNull()
    val spo2 = saturacion.toIntOrNull()
    fun errorEntero(texto: String, valor: Int?, max: Int): String? = when {
        texto.isBlank() -> null
        valor == null || valor < 0 || valor > max -> "Ingresa un valor entre 0 y $max"
        else -> null
    }
    val errorFc = errorEntero(frecCardiaca, fc, 300)
    val errorFr = errorEntero(frecResp, fr, 100)
    val errorPas = errorEntero(presionSis, pas, 300)
    val errorPad = errorEntero(presionDia, pad, 200)
        ?: if (pad != null && pas != null && pad > pas) "No puede superar la sistólica" else null
    val errorSpo2 = errorEntero(saturacion, spo2, 100)
    val hayErrores = listOf(errorTemperatura, errorFc, errorFr, errorPas, errorPad, errorSpo2).any { it != null }

    Column(Modifier.fillMaxSize()) {
        BarraContexto()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (esNuevo) {
                BannerInfo(
                    "${paciente.nombreCompleto} (${paciente.codigo}) se registró correctamente.",
                    icono = Icons.Filled.CheckCircle,
                    fondo = VerdeClaro,
                    color = VerdeSigep
                )
            }

            Text("Evaluación clínica", color = TextoOscuro, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                paciente.nivel?.let { EtiquetaTriage(it) }
                Text(
                    "${paciente.nombreCompleto} · ${paciente.centro}",
                    color = TextoSuave,
                    fontSize = 12.sp
                )
            }

            // Motivo principal
            Column {
                EtiquetaCampo("Cuadro principal (síndrome)", true)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Sindrome.entries.forEach {
                        ChipSeleccion(it.etiqueta, sindrome == it, { sindrome = it })
                    }
                }
                if (intento && sindrome == null) {
                    Text("Selecciona el cuadro principal", color = RojoSigep, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }

            // Síntomas
            Column {
                EtiquetaCampo("Síntomas observados", false)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SINTOMAS.forEach { s ->
                        ChipSeleccion(
                            texto = s,
                            seleccionado = s in sintomas,
                            onClick = { sintomas = if (s in sintomas) sintomas - s else sintomas + s }
                        )
                    }
                }
            }

            Text("Signos vitales", color = TextoOscuro, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) {
                    CampoFormulario("Pulso (lat/min)", frecCardiaca, { frecCardiaca = it.filter(Char::isDigit).take(3) },
                        placeholder = "Ej. 80", teclado = KeyboardType.Number, error = errorFc)
                }
                Box(Modifier.weight(1f)) {
                    CampoFormulario("Respiración (res/min)", frecResp, { frecResp = it.filter(Char::isDigit).take(3) },
                        placeholder = "Ej. 16", teclado = KeyboardType.Number, error = errorFr)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) {
                    CampoFormulario("Presión sistólica", presionSis, { presionSis = it.filter(Char::isDigit).take(3) },
                        placeholder = "Ej. 120", teclado = KeyboardType.Number, error = errorPas)
                }
                Box(Modifier.weight(1f)) {
                    CampoFormulario("Presión diastólica", presionDia, { presionDia = it.filter(Char::isDigit).take(3) },
                        placeholder = "Ej. 80", teclado = KeyboardType.Number, error = errorPad)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) {
                    CampoFormulario("Saturación O₂ (%)", saturacion, { saturacion = it.filter(Char::isDigit).take(3) },
                        placeholder = "Ej. 96", teclado = KeyboardType.Number, error = errorSpo2)
                }
                Box(Modifier.weight(1f)) {
                    CampoFormulario("Temperatura (°C)", temperatura, { temperatura = it },
                        placeholder = "Ej. 37.5", teclado = KeyboardType.Decimal, error = errorTemperatura)
                }
            }
            Text(
                "Todos son opcionales; con menos datos la sugerencia es menos confiable.",
                color = TextoSuave, fontSize = 12.sp
            )

            // Conciencia
            Column {
                EtiquetaCampo("Nivel de conciencia", false)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NIVELES_CONCIENCIA.forEach {
                        ChipSeleccion(it, conciencia == it, { conciencia = it })
                    }
                }
            }

            Tarjeta(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    FilaSwitch("Requiere traslado a centro de salud", requiereTraslado) { requiereTraslado = it }
                    HorizontalDivider(color = BordeSuave)
                    FilaSwitch("Persona gestante", gestante) { gestante = it }
                }
            }

            CampoFormulario(
                etiqueta = "Condiciones previas o alergias",
                valor = condicionesPrevias,
                onCambio = { condicionesPrevias = it },
                placeholder = "Ej. Hipertensión, alergia a penicilina",
                minLineas = 2,
                ayuda = "Opcional."
            )

            // Estado clínico
            Column {
                EtiquetaCampo("Estado general", true)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EstadoClinico.entries.forEach {
                        ChipSeleccion(it.etiqueta, estado == it, { estado = it }, it.color)
                    }
                }
                if (intento && estado == null) {
                    Text("Selecciona el estado general", color = RojoSigep, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }

            CampoFormulario(
                etiqueta = "Notas adicionales",
                valor = notas,
                onCambio = { notas = it },
                placeholder = "Información relevante para el equipo",
                minLineas = 3,
                ayuda = "Opcional. No escribas diagnósticos."
            )

            errorEnvio?.let { Text(it, color = RojoSigep, fontSize = 13.sp) }

            BotonPrimario(if (enviando) "Guardando…" else "Guardar evaluación ✓", enabled = !enviando, onClick = {
                intento = true
                val e = estado
                if (sindrome != null && e != null && !hayErrores) {
                    alcance.launch {
                        enviando = true
                        errorEnvio = null
                        val cuerpo = EvaluacionNueva(
                            frecuencia_cardiaca = fc,
                            frecuencia_respiratoria = fr,
                            presion_sistolica = pas,
                            presion_diastolica = pad,
                            temperatura = tempNumero,
                            saturacion_oxigeno = spo2,
                            conciencia = VALORES_CONCIENCIA[NIVELES_CONCIENCIA.indexOf(conciencia)],
                            sintomas = sintomas,
                            sindrome = sindrome?.valor,
                            estado_clinico = e.valor,
                            requiere_traslado = requiereTraslado,
                            notas = notas.trim().ifBlank { null }
                        )
                        when (val r = TriageController.evaluar(repo, paciente, cuerpo, gestante, condicionesPrevias)) {
                            is ResultadoTriage.Exito -> onGuardado()
                            is ResultadoTriage.Falla -> errorEnvio = r.mensaje
                        }
                        enviando = false
                    }
                }
            })
            BotonTextoCentrado(if (esNuevo) "Evaluar después" else "Cancelar", { if (!enviando) onDespues() })
        }
    }
}

@Composable
private fun FilaSwitch(texto: String, valor: Boolean, onCambio: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(texto, color = TextoOscuro, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(checked = valor, onCheckedChange = onCambio)
    }
}
