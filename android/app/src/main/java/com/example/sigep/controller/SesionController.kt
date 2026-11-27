package com.example.sigep.controller

import android.content.Context
import com.example.sigep.model.AlmacenSesion
import com.example.sigep.model.RetrofitClient
import com.example.sigep.model.RepositorioSigep
import com.example.sigep.model.Rol
import com.example.sigep.model.SesionUsuario
import com.example.sigep.model.UsuarioRespuesta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Controlador de la sesion: sabe quien esta conectado y con que rol.
 * Las vistas observan [sesion]: si es null no hay sesion abierta.
 */
object SesionController {
    private val _sesion = MutableStateFlow<SesionUsuario?>(null)
    val sesion: StateFlow<SesionUsuario?> = _sesion.asStateFlow()

    private val _sesionExpirada = MutableStateFlow(false)

    /** true cuando la sesion se cerro porque el servidor rechazo el token. */
    val sesionExpirada: StateFlow<Boolean> = _sesionExpirada.asStateFlow()

    private var contextoApp: Context? = null

    /** Se llama una vez al abrir la app: recupera la sesion guardada. */
    fun iniciar(contexto: Context) {
        contextoApp = contexto.applicationContext
        AlmacenSesion.iniciar(contexto)
        _sesion.value = AlmacenSesion.leer()
        RetrofitClient.alSesionExpirada = { cerrarSesion(expirada = true) }
    }

    fun abrirSesion(token: String, usuario: UsuarioRespuesta, nombreRespaldo: String) {
        val nueva = SesionUsuario(
            token = token,
            id = usuario.id,
            correo = usuario.correo,
            nombre = usuario.nombre?.takeIf { it.isNotBlank() } ?: nombreRespaldo,
            apellido = usuario.apellido ?: "",
            rol = Rol.desdeValor(usuario.rol)
        )
        AlmacenSesion.guardar(nueva)
        _sesion.value = nueva
    }

    fun cerrarSesion(expirada: Boolean = false) {
        AlmacenSesion.borrar()
        // Los datos de salud no deben quedar en el telefono para la siguiente persona que inicie sesion.
        RepositorioSigep.borrarDatosPersonales(contextoApp)
        _sesion.value = null
        if (expirada) {
            _sesionExpirada.value = true
        }
    }

    /** La vista ya aviso al usuario que la sesion expiro. */
    fun avisoExpiradaMostrado() {
        _sesionExpirada.value = false
    }

    /**
     * Pregunta al servidor por el perfil para tener el rol y el nombre al dia
     * (el coordinador puede haberlos cambiado). Si no hay conexion, se conserva
     * la sesion guardada; si el servidor responde 401 el cliente HTTP la cierra.
     */
    suspend fun refrescarPerfil() {
        if (_sesion.value == null) return
        try {
            val respuesta = RetrofitClient.authApi.perfil()
            if (!respuesta.isSuccessful) return
            val usuario = respuesta.body()?.usuario ?: return
            val actual = _sesion.value ?: return
            val actualizada = actual.copy(
                nombre = usuario.nombre?.takeIf { it.isNotBlank() } ?: actual.nombre,
                apellido = usuario.apellido ?: actual.apellido,
                rol = Rol.desdeValor(usuario.rol)
            )
            AlmacenSesion.guardar(actualizada)
            _sesion.value = actualizada
        } catch (e: Exception) {
            // Sin conexion: se mantiene la sesion guardada.
        }
    }
}
