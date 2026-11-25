package com.example.sigep.model

import android.content.Context
import android.content.SharedPreferences

/**
 * Guarda la sesion en el almacenamiento privado de la app para que siga abierta
 * al cerrar y volver a abrir. Solo se puede leer desde esta aplicacion.
 */
object AlmacenSesion {
    private const val ARCHIVO = "sigep_sesion"
    private const val TOKEN = "token"
    private const val ID = "id"
    private const val CORREO = "correo"
    private const val NOMBRE = "nombre"
    private const val APELLIDO = "apellido"
    private const val ROL = "rol"

    private var preferencias: SharedPreferences? = null

    fun iniciar(contexto: Context) {
        if (preferencias == null) {
            preferencias = contexto.applicationContext
                .getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
        }
    }

    fun guardar(sesion: SesionUsuario) {
        preferencias?.edit()
            ?.putString(TOKEN, sesion.token)
            ?.putLong(ID, sesion.id)
            ?.putString(CORREO, sesion.correo)
            ?.putString(NOMBRE, sesion.nombre)
            ?.putString(APELLIDO, sesion.apellido)
            ?.putString(ROL, sesion.rol.valor)
            ?.apply()
    }

    fun leer(): SesionUsuario? {
        val datos = preferencias ?: return null
        val token = datos.getString(TOKEN, null) ?: return null
        return SesionUsuario(
            token = token,
            id = datos.getLong(ID, 0L),
            correo = datos.getString(CORREO, "") ?: "",
            nombre = datos.getString(NOMBRE, "") ?: "",
            apellido = datos.getString(APELLIDO, "") ?: "",
            rol = Rol.desdeValor(datos.getString(ROL, null))
        )
    }

    fun borrar() {
        preferencias?.edit()?.clear()?.apply()
    }
}
