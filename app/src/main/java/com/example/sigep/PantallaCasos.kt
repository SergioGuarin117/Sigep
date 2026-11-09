package com.example.sigep

import androidx.compose.foundation.background
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
                        onMensaje("Condición de ${paciente.nombreCompleto} guardada.")
                        onRuta(if (ruta.esNuevo) RutaCasos.Lista else RutaCasos.Detalle(paciente.id))
                    },
                    onDespues = {
                        if (ruta.esNuevo) {
                            onMensaje("${paciente.nombreCompleto} quedó registrado. La condición está pendiente.")
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
            id = ruta.pacienteId,
            onEditarCondicion = { onRuta(RutaCasos.Condicion(ruta.pacienteId, esNuevo = false)) },
            onEliminado = { texto ->
                onMensaje(texto)
                onRuta(RutaCasos.Lista)
            }
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
    var filtro by rememberSaveable { mutableIntStateOf(0) } // 0 = todos, 1..3 = P1..P3
    var porEliminar by remember { mutableStateOf<Paciente?>(null) }

    val todos = repo.pacientes
    val triageFiltro = Triage.entries.getOrNull(filtro - 1)
    val texto = normalizar(busqueda.trim())
    val visibles = todos.filter { p ->
        (triageFiltro == null || p.triage == triageFiltro) &&
            (texto.isEmpty() ||
                normalizar(p.nombre).contains(texto) ||
                normalizar(p.apellido).contains(texto) ||
                normalizar(p.nombreCompleto).contains(texto) ||
                normalizar(p.codigo).contains(texto))
    }

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

            Text("Filtrar por prioridad de triage", color = TextoSuave, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipSeleccion("Todos · ${todos.size}", filtro == 0, { filtro = 0 })
                Triage.entries.forEachIndexed { i, t ->
                    ChipSeleccion(
                        "${t.codigo} · ${todos.count { it.triage == t }}",
                        filtro == i + 1,
                        { filtro = i + 1 },
                        t.color
                    )
                }
            }
            Text(
                "P1 Inmediata · P2 Prioritaria · P3 No urgente",
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
                    visibles.forEachIndexed { i, p ->
                        if (i > 0) HorizontalDivider(color = BordeSuave)
                        FilaPaciente(p, onAbrir = { onAbrir(p.id) }, onEliminar = { porEliminar = p })
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

    porEliminar?.let { p ->
        DialogoEliminarPaciente(
            paciente = p,
            onCancelar = { porEliminar = null },
            onConfirmar = {
                repo.eliminarPaciente(p.id)
                onMensaje("Se eliminó el registro de ${p.nombreCompleto}.")
                porEliminar = null
            }
        )
    }
}

@Composable
private fun FilaPaciente(p: Paciente, onAbrir: () -> Unit, onEliminar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAbrir)
            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(p.nombreCompleto, color = TextoOscuro, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${p.codigo} · Sangre ${p.tipoSangre} · ${formatoHora(p.creadoEn)}",
                color = TextoSuave,
                fontSize = 12.sp
            )
            val c = p.condicion
            when {
                p.cerrado -> Text("Dado de alta", color = VerdeSigep, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                c == null -> Text("Condición pendiente", color = Color(0xFFB8741A), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                else -> Text(
                    "${c.sindrome.etiqueta} · ${c.estado.etiqueta}",
                    color = c.estado.color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        EtiquetaTriage(p.triage)
        IconButton(onClick = onEliminar) {
            Icon(Icons.Filled.Delete, contentDescription = "Eliminar a ${p.nombreCompleto}", tint = RojoSigep)
        }
    }
}

@Composable
private fun DialogoEliminarPaciente(paciente: Paciente, onCancelar: () -> Unit, onConfirmar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Color.White,
        title = { Text("Eliminar paciente") },
        text = {
            Text("¿Eliminar a ${paciente.nombreCompleto} (${paciente.codigo})? Se borrará también su condición. Esta acción no se puede deshacer.")
        },
        confirmButton = { TextButton(onClick = onConfirmar) { Text("Eliminar", color = RojoSigep, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
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

    val conCondicion = repo.pacientes.filter { it.condicion != null }
    val sinCondicion = repo.pacientes.size - conCondicion.size
    val texto = normalizar(busqueda.trim())
    val registros = conCondicion.filter { p ->
        texto.isEmpty() ||
            normalizar(p.codigoCaso).contains(texto) ||
            normalizar(p.condicion?.sindrome?.etiqueta ?: "").contains(texto) ||
            normalizar(p.nombreCompleto).contains(texto)
    }

    val conteos = Sindrome.entries.map { s ->
        s to activos.count { it.condicion?.sindrome == s }
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
                        "$sinCondicion ${if (sinCondicion == 1) "paciente" else "pacientes"} sin condición registrada.",
                        color = TextoSuave,
                        fontSize = 11.sp
                    )
                }
            }
        }

        TituloSeccion("Registros recientes · ${registros.size} resultados")

        if (registros.isEmpty()) {
            Text(
                if (conCondicion.isEmpty()) "Aún no hay casos con condición registrada." else "No se encontraron casos con esa búsqueda.",
                color = TextoSuave,
                fontSize = 13.sp
            )
        }

        registros.forEach { p ->
            val c = p.condicion ?: return@forEach
            Tarjeta(Modifier.fillMaxWidth(), onClick = { onAbrir(p.id) }) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            "${p.codigoCaso} · ${c.sindrome.etiqueta}",
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
    id: Long,
    onEditarCondicion: () -> Unit,
    onEliminado: (String) -> Unit
) {
    val p = repo.pacientePorId(id)
    var confirmarEliminar by remember { mutableStateOf(false) }

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
                        EtiquetaTriage(p.triage)
                    }
                    if (p.cerrado) Pastilla("Dado de alta", VerdeSigep)
                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = BordeSuave)
                    FilaDato("Tipo de sangre", p.tipoSangre)
                    FilaDato("Registrado", formatoFechaLarga(p.creadoEn))
                    FilaDato("Observaciones", p.observaciones.ifBlank { "Sin observaciones" })
                }
            }

            Text("Condición", color = TextoOscuro, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            val c = p.condicion
            if (c == null) {
                BannerInfo(
                    "Aún no se registra la condición de este paciente.",
                    fondo = Color(0xFFFDF0DE),
                    color = Color(0xFFB8741A)
                )
                BotonPrimario("Registrar condición", onClick = onEditarCondicion)
            } else {
                Tarjeta(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pastilla(c.estado.etiqueta, c.estado.color)
                            if (c.requiereTraslado) Pastilla("Requiere traslado", RojoSigep)
                        }
                        FilaDato("Cuadro principal", c.sindrome.etiqueta)
                        FilaDato(
                            "Síntomas",
                            if (c.sintomas.isEmpty()) "Ninguno indicado" else c.sintomas.joinToString(", ")
                        )
                        FilaDato("Temperatura", c.temperatura?.let { "$it °C" } ?: "No registrada")
                        FilaDato("Nivel de conciencia", c.conciencia)
                        if (c.gestante) FilaDato("Gestante", "Sí")
                        if (c.condicionesPrevias.isNotBlank()) FilaDato("Condiciones previas / alergias", c.condicionesPrevias)
                        if (c.notas.isNotBlank()) FilaDato("Notas", c.notas)
                        Text(
                            "Actualizado ${formatoFechaHora(c.actualizadoEn)}",
                            color = TextoSuave,
                            fontSize = 11.sp
                        )
                    }
                }
                BotonPrimario("Editar condición", onClick = onEditarCondicion)
            }

            BotonPrimario(
                texto = if (p.cerrado) "Reabrir caso" else "Dar de alta",
                onClick = { repo.alternarAlta(p.id) },
                color = if (p.cerrado) AzulSigep else VerdeSigep
            )
            BotonTextoCentrado("Eliminar paciente", { confirmarEliminar = true }, RojoSigep)
        }
    }

    if (confirmarEliminar) {
        DialogoEliminarPaciente(
            paciente = p,
            onCancelar = { confirmarEliminar = false },
            onConfirmar = {
                confirmarEliminar = false
                repo.eliminarPaciente(p.id)
                onEliminado("Se eliminó el registro de ${p.nombreCompleto}.")
            }
        )
    }
}
