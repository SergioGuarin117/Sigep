package com.example.sigep.controller

import com.example.sigep.model.CambioUsuario
import com.example.sigep.model.RetrofitClient
import com.example.sigep.model.UsuarioAdmin
import org.json.JSONObject

sealed interface ResultadoListaUsuarios {
    data class Exito(val usuarios: List<UsuarioAdmin>) : ResultadoListaUsuarios
    data object SinPermiso : ResultadoListaUsuarios
    data object SinConexion : ResultadoListaUsuarios
    data class Error(val codigo: Int) : ResultadoListaUsuarios
}

sealed interface ResultadoCambioUsuario {
    data class Exito(val usuario: UsuarioAdmin) : ResultadoCambioUsuario
    data class Rechazado(val mensaje: String) : ResultadoCambioUsuario
    data object SinConexion : ResultadoCambioUsuario
}

/** Controlador de la gestion de usuarios (solo coordinador municipal). */
object UsuariosController {

    suspend fun listar(rol: String?, busqueda: String): ResultadoListaUsuarios {
        return try {
            val respuesta = RetrofitClient.usuariosApi.listar(
                rol = rol,
                busqueda = busqueda.trim().ifBlank { null }
            )
            when {
                respuesta.isSuccessful ->
                    ResultadoListaUsuarios.Exito(respuesta.body()?.usuarios.orEmpty())
                respuesta.code() == 403 -> ResultadoListaUsuarios.SinPermiso
                else -> ResultadoListaUsuarios.Error(respuesta.code())
            }
        } catch (e: Exception) {
            ResultadoListaUsuarios.SinConexion
        }
    }

    suspend fun cambiar(id: Long, rol: String?, activo: Boolean?): ResultadoCambioUsuario {
        return try {
            val respuesta = RetrofitClient.usuariosApi.actualizar(id, CambioUsuario(rol, activo))
            val usuario = respuesta.body()?.usuario
            if (respuesta.isSuccessful && usuario != null) {
                ResultadoCambioUsuario.Exito(usuario)
            } else {
                ResultadoCambioUsuario.Rechazado(mensajeDeError(respuesta.errorBody()?.string(), respuesta.code()))
            }
        } catch (e: Exception) {
            ResultadoCambioUsuario.SinConexion
        }
    }

    private fun mensajeDeError(cuerpo: String?, codigo: Int): String {
        val texto = try {
            cuerpo?.let { JSONObject(it).optString("error") }
        } catch (e: Exception) {
            null
        }
        return texto?.takeIf { it.isNotBlank() } ?: "El servidor rechazó el cambio ($codigo)."
    }
}
