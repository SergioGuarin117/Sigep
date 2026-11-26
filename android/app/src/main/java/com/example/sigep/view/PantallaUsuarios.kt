package com.example.sigep.view

import com.example.sigep.model.*
import com.example.sigep.controller.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** Gestion de usuarios: solo la ve el coordinador municipal. */
@Composable
fun ContenidoUsuarios(miId: Long) {
    var usuarios by remember { mutableStateOf<List<UsuarioAdmin>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var busqueda by remember { mutableStateOf("") }
    var filtroRol by remember { mutableStateOf<Rol?>(null) }
    var editando by remember { mutableStateOf<UsuarioAdmin?>(null) }
    var recarga by remember { mutableStateOf(0) }
    val alcance = rememberCoroutineScope()

    LaunchedEffect(filtroRol, busqueda, recarga) {
        cargando = true
        when (val r = UsuariosController.listar(filtroRol?.valor, busqueda)) {
            is ResultadoListaUsuarios.Exito -> { usuarios = r.usuarios; error = null }
            ResultadoListaUsuarios.SinPermiso -> error = "Tu rol no tiene permiso para ver los usuarios."
            ResultadoListaUsuarios.SinConexion -> error = "Sin conexión con el servidor."
            is ResultadoListaUsuarios.Error -> error = "El servidor respondió ${r.codigo}."
        }
        cargando = false
    }

    Column(Modifier.fillMaxSize()) {
        BarraContexto()
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CampoFormulario(
                etiqueta = "Buscar",
                valor = busqueda,
                onCambio = { busqueda = it },
                placeholder = "Nombre, apellido o correo"
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChipSeleccion("Todos", filtroRol == null, { filtroRol = null })
                Rol.entries.forEach { rol ->
                    ChipSeleccion(rol.etiqueta, filtroRol == rol, { filtroRol = rol })
                }
            }
            aviso?.let { BannerInfo(it) }
            error?.let { Text(it, color = RojoSigep, fontSize = 13.sp) }
            if (cargando) Text("Cargando…", color = TextoSuave, fontSize = 13.sp)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(usuarios, key = { it.id }) { u ->
                Tarjeta(Modifier.fillMaxWidth(), onClick = { editando = u }) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                u.nombreCompleto + if (u.id == miId) " (tú)" else "",
                                color = TextoOscuro,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            if (!u.activo) Pastilla("Inactivo", RojoSigep)
                        }
                        Text(u.correo, color = TextoSuave, fontSize = 12.sp)
                        Pastilla(Rol.desdeValor(u.rol).etiqueta, AzulSigep)
                    }
                }
            }
        }
    }

    editando?.let { usuario ->
        DialogoEditarUsuario(
            usuario = usuario,
            esUnoMismo = usuario.id == miId,
            onCancelar = { editando = null },
            onGuardar = { rol, activo ->
                editando = null
                alcance.launch {
                    aviso = when (val r = UsuariosController.cambiar(usuario.id, rol.valor, activo)) {
                        is ResultadoCambioUsuario.Exito -> "Se actualizó a ${r.usuario.nombreCompleto}."
                        is ResultadoCambioUsuario.Rechazado -> r.mensaje
                        ResultadoCambioUsuario.SinConexion -> "Sin conexión: no se guardó el cambio."
                    }
                    recarga += 1
                }
            }
        )
    }
}

@Composable
private fun DialogoEditarUsuario(
    usuario: UsuarioAdmin,
    esUnoMismo: Boolean,
    onCancelar: () -> Unit,
    onGuardar: (Rol, Boolean) -> Unit
) {
    var rol by remember { mutableStateOf(Rol.desdeValor(usuario.rol)) }
    var activo by remember { mutableStateOf(usuario.activo) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(usuario.nombreCompleto, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(usuario.correo, color = TextoSuave, fontSize = 13.sp)
                if (esUnoMismo) {
                    Text(
                        "No puedes cambiar tu propio rol ni desactivar tu cuenta.",
                        color = TextoSuave,
                        fontSize = 12.sp
                    )
                } else {
                    Selector(
                        etiqueta = "Rol",
                        opciones = Rol.entries.map { it.etiqueta },
                        seleccion = rol.etiqueta,
                        onSeleccion = { eleccion -> rol = Rol.entries.first { it.etiqueta == eleccion } }
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Cuenta activa", color = TextoOscuro, modifier = Modifier.weight(1f))
                        Switch(checked = activo, onCheckedChange = { activo = it })
                    }
                }
            }
        },
        confirmButton = {
            if (!esUnoMismo) {
                TextButton(onClick = { onGuardar(rol, activo) }) { Text("Guardar") }
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text(if (esUnoMismo) "Cerrar" else "Cancelar") } }
    )
}
