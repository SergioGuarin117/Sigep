package com.example.sigep.view

import com.example.sigep.model.*
import com.example.sigep.controller.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
fun PantallaCasos(
    repo: RepositorioSigep,
    rol: Rol,
    ruta: RutaCasos,
    onRuta: (RutaCasos) -> Unit,
    borrador: BorradorPaciente,
    vista: Int,
    onVista: (Int) -> Unit,
    mensaje: String?,
    onMensaje: (String?) -> Unit
) {
    when (ruta) {
        is RutaCasos.Lista -> VistaListaCasos(
            repo = repo,
            vista = vista,
            onVista = onVista,
            mensaje = mensaje,
            onMensaje = onMensaje,
            onRegistrar = { onRuta(RutaCasos.Registro) },
            onAbrir = { onRuta(RutaCasos.Detalle(it)) }
        )

        is RutaCasos.Registro -> VistaRegistro(
            borrador = borrador,
            repo = repo,
            onCancelar = {
                borrador.limpiar()
                onRuta(RutaCasos.Lista)
            },
            onRegistrado = { nuevo -> onRuta(RutaCasos.Condicion(nuevo.id, esNuevo = true)) }
        )

        is RutaCasos.Condicion -> {
            val paciente = repo.pacientePorId(ruta.pacienteId)
            if (paciente == null) {
                LaunchedEffect(Unit) { onRuta(RutaCasos.Lista) }
            } else {
                FormularioCondicion(
                    paciente = paciente,
                    esNuevo = ruta.esNuevo,
                    repo = repo,
                    onGuardado = {
                        onMensaje("Evaluación de ${paciente.nombreCompleto} guardada.")
                        onRuta(if (ruta.esNuevo) RutaCasos.Lista else RutaCasos.Detalle(paciente.id))
                    },
                    onDespues = {
                        if (ruta.esNuevo) {
                            onMensaje("${paciente.nombreCompleto} quedó registrado. Falta su evaluación clínica.")
                            onRuta(RutaCasos.Lista)
                        } else {
                            onRuta(RutaCasos.Detalle(paciente.id))
                        }
                    }
                )
            }
        }

        is RutaCasos.Detalle -> VistaDetalle(
            repo = repo,
            rol = rol,
            id = ruta.pacienteId,
            onEditarCondicion = { onRuta(RutaCasos.Condicion(ruta.pacienteId, esNuevo = false)) }
        )
    }
}

// ---------------------------------------------------------------- Lista (Pacientes / Vigilancia)

@Composable
private fun VistaListaCasos(
    repo: RepositorioSigep,
    vista: Int,
    onVista: (Int) -> Unit,
    mensaje: String?,
    onMensaje: (String?) -> Unit,
    onRegistrar: () -> Unit,
    onAbrir: (Long) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        BarraContexto()
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            Segmento(
                opciones = listOf("Pacientes · ${repo.pacientes.size}", "Vigilancia"),
                seleccion = vista,
                onSeleccion = onVista
            )
        }
        if (vista == 0) {
            VistaPacientes(repo, mensaje, onMensaje, onRegistrar, onAbrir)
        } else {
            VistaVigilancia(repo, onAbrir)
        }
    }
}

@Composable
private fun VistaPacientes(
    repo: RepositorioSigep,
    mensaje: String?,
    onMensaje: (String?) -> Unit,
    onRegistrar: () -> Unit,
    onAbrir: (Long) -> Unit
) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    // 0 = todos, 1..4 = rojo/amarillo/verde/negro (orden de Triage.entries), 5 = sin evaluar, 6 = por confirmar
    var filtro by rememberSaveable { mutableIntStateOf(0) }

    val todos = repo.pacientes
    val triageFiltro = Triage.entries.getOrNull(filtro - 1)
    val sinEvaluar = todos.count { it.triage == null }
    val porConfirmar = todos.count { it.porConfirmar }
    val texto = normalizar(busqueda.trim())
    val visibles = todos.filter { p ->
        (when (filtro) {
            5 -> p.triage == null
            6 -> p.porConfirmar
            else -> triageFiltro == null || p.nivel == triageFiltro
        }) &&
            (texto.isEmpty() ||
                normalizar(p.nombre).contains(texto) ||
                normalizar(p.apellido).contains(texto) ||
                normalizar(p.nombreCompleto).contains(texto) ||
                normalizar(p.codigo).contains(texto))
    }

    val ordenados = if (filtro == 6) visibles.sortedBy { it.nivel?.ordinal ?: 99 } else visibles

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Todos los centros · ${todos.size} ${if (todos.size == 1) "paciente" else "pacientes"}",
                color = TextoSuave,
                fontSize = 12.sp
            )

            if (mensaje != null) {
                BannerInfo(mensaje, Icons.Filled.CheckCircle, VerdeClaro, VerdeSigep)
            }

            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Buscar por nombre, apellido o código", color = TextoSuave) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextoSuave) },
                trailingIcon = {
                    if (busqueda.isNotEmpty()) {
                        IconButton(onClick = { busqueda = "" }) {
                            Icon(Icons.Filled.Close, "Borrar búsqueda", tint = TextoSuave)
                        }
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = AzulSigep,
                    unfocusedBorderColor = BordeSuave
                )
            )

            Text("Filtrar por nivel de triage", color = TextoSuave, fontSize = 12.sp)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChipSeleccion("Todos · ${todos.size}", filtro == 0, { filtro = 0 })
                Triage.entries.forEachIndexed { i, t ->
                    ChipSeleccion(
                        "${t.valor.replaceFirstChar { it.uppercase() }} · ${todos.count { it.nivel == t }}",
                        filtro == i + 1,
                        { filtro = i + 1 },
                        t.color
                    )
                }
                ChipSeleccion("Sin evaluar · $sinEvaluar", filtro == 5, { filtro = 5 })
                ChipSeleccion("Por confirmar · $porConfirmar", filtro == 6, { filtro = 6 })
            }
            Text(
                "Rojo inmediata · Amarillo prioritaria · Verde no urgente · Negro sin signos vitales",
                color = TextoSuave,
                fontSize = 11.sp
            )

            TituloSeccion("Pacientes registrados")

            if (visibles.isEmpty()) {
                Tarjeta(Modifier.fillMaxWidth()) {
                    Text(
                        text = if (todos.isEmpty()) {
                            "Aún no hay pacientes registrados. Toca «Registrar paciente» para agregar el primero."
                        } else {
                            "No se encontraron pacientes con esa búsqueda o filtro."
                        },
                        color = TextoSuave,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Tarjeta(Modifier.fillMaxWidth()) {
                    ordenados.forEachIndexed { i, p ->
                        if (i > 0) HorizontalDivider(color = BordeSuave)
                        FilaPaciente(p, onAbrir = { onAbrir(p.id) })
                    }
                }
            }

            Text(
                "Mostrando ${visibles.size} de ${todos.size}. Datos de uso exclusivo del personal autorizado.",
                color = TextoSuave,
                fontSize = 11.sp
            )
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            BotonPrimario("Registrar paciente  +", onClick = onRegistrar)
        }
    }

}

@Composable
private fun FilaPaciente(p: Paciente, onAbrir: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAbrir)
            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(p.nombreCompleto, color = TextoOscuro, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${p.codigo} · Sangre ${p.tipoSangre} · ${formatoHora(p.creadoEn)}",
                color = TextoSuave,
                fontSize = 12.sp
            )
            val t = p.triage
            when {
                p.cerrado -> Text("Dado de alta", color = VerdeSigep, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                t == null -> Text("Sin evaluar", color = Color(0xFFB8741A), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                else -> Text(
                    listOfNotNull(t.sindrome?.etiqueta, t.estado?.etiqueta).joinToString(" · ")
                        .ifBlank { "Evaluado" },
                    color = t.estado?.color ?: TextoSuave,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            p.nivel?.let { EtiquetaTriage(it) }
            if (p.porConfirmar) Text("Por confirmar", color = Color(0xFFB8741A), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ---------------------------------------------------------------- Vigilancia epidemiológica

@Composable
private fun VistaVigilancia(repo: RepositorioSigep, onAbrir: (Long) -> Unit) {
    var busqueda by rememberSaveable { mutableStateOf("") }

    val ahora = System.currentTimeMillis()
    val dia = 24L * 60 * 60 * 1000
    val activos = repo.pacientes.filter { !it.cerrado }
    val nuevos = repo.pacientes.count { ahora - it.creadoEn <= dia }
    val cerrados = repo.pacientes.count { it.cerrado }

    val conCondicion = repo.pacientes.filter { it.triage != null }
    val sinCondicion = repo.pacientes.size - conCondicion.size
    val texto = normalizar(busqueda.trim())
    val registros = conCondicion.filter { p ->
        texto.isEmpty() ||
            normalizar(p.codigoCaso).contains(texto) ||
            normalizar(p.triage?.sindrome?.etiqueta ?: "").contains(texto) ||
            normalizar(p.nombreCompleto).contains(texto)
    }

    val conteos = Sindrome.entries.map { s ->
        s to activos.count { it.triage?.sindrome == s }
    }
    val maximo = (conteos.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Vigilancia epidemiológica · Registros anonimizados", color = TextoSuave, fontSize = 12.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TarjetaResumen(activos.size.toString(), "Activos", Modifier.weight(1f))
            TarjetaResumen(nuevos.toString(), "Nuevos · 24 h", Modifier.weight(1f))
            TarjetaResumen(cerrados.toString(), "Cerrados", Modifier.weight(1f))
        }

        OutlinedTextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Buscar por código o síndrome", color = TextoSuave) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextoSuave) },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = AzulSigep,
                unfocusedBorderColor = BordeSuave
            )
        )

        Tarjeta(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Síndromes en vigilancia", color = TextoOscuro, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                conteos.forEach { (sindrome, n) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(sindrome.etiqueta, color = TextoOscuro, fontSize = 13.sp, modifier = Modifier.width(130.dp))
                        Box(
                            Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(AzulClaro)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(n.toFloat() / maximo)
                                    .height(8.dp)
                                    .background(AzulSigep, RoundedCornerShape(4.dp))
                            )
                        }
                        Text(
                            n.toString(),
                            color = TextoOscuro,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(28.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                }
                if (sinCondicion > 0) {
                    Text(
                        "$sinCondicion ${if (sinCondicion == 1) "paciente" else "pacientes"} sin evaluación clínica.",
                        color = TextoSuave,
                        fontSize = 11.sp
                    )
                }
            }
        }

        TituloSeccion("Registros recientes · ${registros.size} resultados")

        if (registros.isEmpty()) {
            Text(
                if (conCondicion.isEmpty()) "Aún no hay casos con evaluación clínica." else "No se encontraron casos con esa búsqueda.",
                color = TextoSuave,
                fontSize = 13.sp
            )
        }

        registros.forEach { p ->
            val c = p.triage ?: return@forEach
            Tarjeta(Modifier.fillMaxWidth(), onClick = { onAbrir(p.id) }) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            "${p.codigoCaso} · ${c.sindrome?.etiqueta ?: "Sin clasificar"}",
                            color = TextoOscuro,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("${p.centro} · ${formatoFechaHora(p.creadoEn)}", color = TextoSuave, fontSize = 12.sp)
                    }
                    val esNuevo = System.currentTimeMillis() - p.creadoEn <= dia
                    when {
                        p.cerrado -> Pastilla("Cerrado", VerdeSigep)
                        esNuevo -> Pastilla("Nuevo", AzulSigep)
                        else -> Pastilla("Seguimiento", Color(0xFFB8741A))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Detalle

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VistaDetalle(
    repo: RepositorioSigep,
    rol: Rol,
    id: Long,
    onEditarCondicion: () -> Unit
) {
    val p = repo.pacientePorId(id)
    val alcance = rememberCoroutineScope()
    var guardandoAlta by remember { mutableStateOf(false) }
    var errorAlta by remember { mutableStateOf<String?>(null) }

    if (p == null) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("Este paciente ya no existe.", color = TextoSuave)
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        BarraContexto()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Tarjeta(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.nombreCompleto, color = TextoOscuro, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("${p.codigo} · ${p.centro}", color = TextoSuave, fontSize = 12.sp)
                        }
                        p.nivel?.let { EtiquetaTriage(it) }
                    }
                    if (p.cerrado) Pastilla("Dado de alta", VerdeSigep)
                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = BordeSuave)
                    FilaDato("Tipo de sangre", p.tipoSangre)
                    FilaDato("Registrado", formatoFechaLarga(p.creadoEn))
                    FilaDato("Observaciones", p.observaciones.ifBlank { "Sin observaciones" })
                }
            }

            if (p.gestante) FilaDato("Gestante", "Sí")
            if (p.condicionesPrevias.isNotBlank()) FilaDato("Condiciones previas / alergias", p.condicionesPrevias)

            TarjetaTriage(repo, rol, p, onEditarCondicion)

            BotonPrimario(
                texto = if (p.cerrado) "Reabrir caso" else "Dar de alta",
                enabled = !guardandoAlta,
                onClick = {
                    alcance.launch {
                        guardandoAlta = true
                        errorAlta = null
                        val resultado = CensoController.cambiarAlta(repo, p)
                        if (resultado is ResultadoAlta.Falla) errorAlta = resultado.mensaje
                        guardandoAlta = false
                    }
                },
                color = if (p.cerrado) AzulSigep else VerdeSigep
            )
            errorAlta?.let { Text(it, color = RojoSigep, fontSize = 13.sp) }
        }
    }
}

// ---------------------------------------------------------------- Triage en el detalle

private fun nivelDeValor(valor: String?): Triage? = Triage.desdeValor(valor)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaTriage(
    repo: RepositorioSigep,
    rol: Rol,
    p: Paciente,
    onEvaluar: () -> Unit
) {
    val alcance = rememberCoroutineScope()
    var historial by remember(p.id, p.triage?.evaluacionId) { mutableStateOf<List<EvaluacionDto>?>(null) }
    var errorHistorial by remember(p.id) { mutableStateOf<String?>(null) }
    var confirmando by remember { mutableStateOf(false) }
    var errorConfirmar by remember { mutableStateOf<String?>(null) }
    var nivelElegido by remember(p.triage?.evaluacionId) { mutableStateOf<Triage?>(null) }

    LaunchedEffect(p.id, p.triage?.evaluacionId) {
        errorHistorial = null
        when (val r = TriageController.historial(p.id)) {
            is ResultadoHistorial.Exito -> historial = r.evaluaciones
            is ResultadoHistorial.Falla -> errorHistorial = r.mensaje
        }
    }

    Text("Triage", color = TextoOscuro, fontSize = 17.sp, fontWeight = FontWeight.Bold)

    val resumen = p.triage
    if (resumen == null) {
        BannerInfo(
            "Este paciente aún no tiene evaluación clínica. Registra los signos vitales para obtener el nivel sugerido.",
            fondo = Color(0xFFFDF0DE),
            color = Color(0xFFB8741A)
        )
        if (!p.cerrado) BotonPrimario("Registrar evaluación", onClick = onEvaluar)
        return
    }

    val ultima = historial?.firstOrNull { it.id == resumen.evaluacionId }

    Tarjeta(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val vigente = resumen.vigente
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(vigente.fondo)
                    .padding(12.dp)
            ) {
                Text(
                    if (resumen.confirmado != null) "Nivel confirmado: ${vigente.titulo}" else "Nivel sugerido: ${vigente.titulo}",
                    color = vigente.color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(vigente.mensaje, color = vigente.color, fontSize = 12.sp)
            }
            if (resumen.confirmado == null) {
                Text(
                    "Pendiente de confirmación médica. La sugerencia es una ayuda y no reemplaza el criterio del médico.",
                    color = Color(0xFFB8741A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                val d = ultima?.decision
                Text(
                    "Confirmado${d?.confirmado_por_nombre?.let { " por $it" } ?: ""}" +
                        (d?.confirmado_en?.let { " el ${formatoIso(it)}" } ?: ""),
                    color = TextoSuave,
                    fontSize = 12.sp
                )
                if (resumen.confirmado != resumen.sugerido) {
                    Text("El sistema había sugerido ${resumen.sugerido.titulo}.", color = TextoSuave, fontSize = 12.sp)
                }
            }

            if (ultima != null) {
                val s = ultima.sugerencia
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    s.puntaje?.let { Pastilla("Puntaje $it/100", AzulSigep) }
                    s.confianza?.let { Pastilla("Confianza $it", AzulSigep) }
                    if (resumen.requiereTraslado) Pastilla("Requiere traslado", RojoSigep)
                }
                if (s.motivos.isNotEmpty()) {
                    Text("Motivos", color = TextoOscuro, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    s.motivos.forEach { m ->
                        val nivel = nivelDeValor(m.nivel)
                        Row(verticalAlignment = Alignment.Top) {
                            Text("• ", color = nivel?.color ?: TextoOscuro, fontSize = 13.sp)
                            Text(m.descripcion, color = TextoOscuro, fontSize = 13.sp)
                        }
                    }
                }
                if (s.datos_faltantes.isNotEmpty()) {
                    Text(
                        "No se midió: ${s.datos_faltantes.joinToString(", ")}.",
                        color = TextoSuave,
                        fontSize = 12.sp
                    )
                }
                s.avisos.forEach { Text("⚠ $it", color = Color(0xFFB8741A), fontSize = 12.sp) }
                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = BordeSuave)
                DatosEvaluacion(ultima)
            } else if (errorHistorial != null) {
                Text(errorHistorial.orEmpty(), color = RojoSigep, fontSize = 12.sp)
            } else {
                Text("Cargando detalle…", color = TextoSuave, fontSize = 12.sp)
            }
        }
    }

    if (rol == Rol.MEDICO_TRIAGE && resumen.confirmado == null && !p.cerrado) {
        Tarjeta(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Confirmar nivel de triage", color = TextoOscuro, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Elige el nivel final. Solo puede confirmarse una vez; para corregirlo registra una evaluación nueva.",
                    color = TextoSuave,
                    fontSize = 12.sp
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Triage.entries.forEach { t ->
                        ChipSeleccion(
                            "${t.valor.replaceFirstChar { it.uppercase() }} · ${t.etiqueta}",
                            (nivelElegido ?: resumen.sugerido) == t,
                            { nivelElegido = t },
                            t.color
                        )
                    }
                }
                errorConfirmar?.let { Text(it, color = RojoSigep, fontSize = 12.sp) }
                BotonPrimario(
                    if (confirmando) "Confirmando…" else "Confirmar nivel",
                    enabled = !confirmando,
                    onClick = {
                        alcance.launch {
                            confirmando = true
                            errorConfirmar = null
                            val nivel = nivelElegido ?: resumen.sugerido
                            when (val r = TriageController.confirmar(repo, p.id, resumen.evaluacionId, nivel)) {
                                is ResultadoTriage.Exito -> Unit
                                is ResultadoTriage.Falla -> errorConfirmar = r.mensaje
                            }
                            confirmando = false
                        }
                    }
                )
            }
        }
    }

    if (!p.cerrado) BotonPrimario("Registrar nueva evaluación", onClick = onEvaluar)

    val anteriores = historial.orEmpty().filter { it.id != resumen.evaluacionId }
    if (anteriores.isNotEmpty()) {
        Text("Evaluaciones anteriores", color = TextoOscuro, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Tarjeta(Modifier.fillMaxWidth()) {
            anteriores.forEachIndexed { i, e ->
                if (i > 0) HorizontalDivider(color = BordeSuave)
                val sugerido = nivelDeValor(e.sugerencia.nivel)
                val final = nivelDeValor(e.decision?.nivel_final)
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(formatoIso(e.evaluado_en), color = TextoOscuro, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(
                            e.evaluado_por_nombre?.let { "Por $it" } ?: "",
                            color = TextoSuave,
                            fontSize = 11.sp
                        )
                    }
                    (final ?: sugerido)?.let { EtiquetaTriage(it) }
                }
            }
        }
    }
}

@Composable
private fun DatosEvaluacion(e: EvaluacionDto) {
    val v = e.signos_vitales
    FilaDato("Cuadro principal", Sindrome.desdeValor(e.sindrome)?.etiqueta ?: "Sin clasificar")
    FilaDato("Estado general", EstadoClinico.desdeValor(e.estado_clinico)?.etiqueta ?: "Sin dato")
    FilaDato("Síntomas", if (e.sintomas.isEmpty()) "Ninguno indicado" else e.sintomas.joinToString(", "))
    FilaDato("Nivel de conciencia", etiquetaConciencia(v.conciencia))
    FilaDato("Pulso", v.frecuencia_cardiaca?.let { "$it lat/min" } ?: "No registrado")
    FilaDato("Respiración", v.frecuencia_respiratoria?.let { "$it res/min" } ?: "No registrada")
    FilaDato(
        "Presión arterial",
        if (v.presion_sistolica != null && v.presion_diastolica != null) "${v.presion_sistolica}/${v.presion_diastolica} mmHg"
        else "No registrada"
    )
    FilaDato("Saturación de oxígeno", v.saturacion_oxigeno?.let { "$it %" } ?: "No registrada")
    FilaDato("Temperatura", v.temperatura?.let { "$it °C" } ?: "No registrada")
    if (!e.notas.isNullOrBlank()) FilaDato("Notas", e.notas)
    Text(
        "Evaluado ${formatoIso(e.evaluado_en)}${e.evaluado_por_nombre?.let { " por $it" } ?: ""}",
        color = TextoSuave,
        fontSize = 11.sp
    )
}
