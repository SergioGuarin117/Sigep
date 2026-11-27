package com.example.sigep.controller

import android.util.Log
import com.example.sigep.model.AlberguesCache
import com.example.sigep.model.CambioPaciente
import com.example.sigep.model.Paciente
import com.example.sigep.model.PacienteDto
import com.example.sigep.model.PacienteNuevo
import com.example.sigep.model.RepositorioSigep
import com.example.sigep.model.RetrofitClient
import com.example.sigep.model.Triage
import org.json.JSONObject
import kotlin.coroutines.cancellation.CancellationException

enum class ResultadoSync { EXITO, SIN_CONEXION, SIN_PERMISO, ERROR }

sealed interface ResultadoPaciente {
    data class Exito(val paciente: Paciente) : ResultadoPaciente
    data class Falla(val mensaje: String) : ResultadoPaciente
}

sealed interface ResultadoAlta {
    data object Exito : ResultadoAlta
    data class Falla(val mensaje: String) : ResultadoAlta
}

private const val MAXIMO_PACIENTES = 2000

/** Controlador del censo: habla con la API y deja los datos en el repositorio local. */
object CensoController {

    suspend fun sincronizarAlbergues(repo: RepositorioSigep): ResultadoSync {
        return try {
            val respuesta = RetrofitClient.alberguesApi.listar()
            when {
                respuesta.isSuccessful -> {
                    repo.reemplazarAlbergues(respuesta.body()?.albergues.orEmpty())
                    ResultadoSync.EXITO
                }
                respuesta.code() == 403 -> ResultadoSync.SIN_PERMISO
                else -> ResultadoSync.ERROR
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("SIGEP", "No se pudieron cargar los albergues", e)
            ResultadoSync.SIN_CONEXION
        }
    }

    /** Albergues y, si el rol lo permite, pacientes. Los albergues van primero para poder nombrar el centro. */
    suspend fun sincronizar(repo: RepositorioSigep, conPacientes: Boolean): ResultadoSync {
        val albergues = sincronizarAlbergues(repo)
        if (!conPacientes || albergues != ResultadoSync.EXITO) return albergues
        return sincronizarPacientes(repo)
    }

    private suspend fun sincronizarPacientes(repo: RepositorioSigep): ResultadoSync {
        return try {
            val todos = mutableListOf<PacienteDto>()
            var desplazamiento = 0
            while (true) {
                val respuesta = RetrofitClient.pacientesApi.listar(200, desplazamiento)
                if (!respuesta.isSuccessful) {
                    return if (respuesta.code() == 403) ResultadoSync.SIN_PERMISO else ResultadoSync.ERROR
                }
                val cuerpo = respuesta.body()
                val lote = cuerpo?.pacientes.orEmpty()
                todos += lote
                desplazamiento += lote.size
                if (lote.isEmpty() || desplazamiento >= (cuerpo?.total ?: 0) || desplazamiento >= MAXIMO_PACIENTES) break
            }
            repo.reemplazarPacientes(todos)
            ResultadoSync.EXITO
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("SIGEP", "No se pudieron cargar los pacientes", e)
            ResultadoSync.SIN_CONEXION
        }
    }

    /** Registra al paciente en el servidor. Solo si el servidor acepta, queda en el dispositivo. */
    suspend fun registrarPaciente(
        repo: RepositorioSigep,
        nombre: String,
        apellido: String,
        tipoSangre: String,
        centro: String,
        observaciones: String,
        triage: Triage
    ): ResultadoPaciente {
        val albergue = AlberguesCache.porNombre(centro)
            ?: return ResultadoPaciente.Falla(
                "No se pudo identificar el albergue. Revisa la conexión e inténtalo de nuevo."
            )
        val cuerpo = PacienteNuevo(
            nombres = nombre.trim().take(100),
            apellidos = apellido.trim().take(100),
            tipo_sangre = tipoSangre.takeIf { it != "Desconocido" },
            observaciones = observaciones.trim().take(2000).ifBlank { null },
            albergue_id = albergue.id
        )
        return try {
            val respuesta = RetrofitClient.pacientesApi.crear(cuerpo)
            val dto = respuesta.body()?.paciente
            if (respuesta.isSuccessful && dto != null) {
                val paciente = repo.agregarDesdeServidor(dto, triage)
                sincronizarAlbergues(repo) // la ocupacion del albergue cambio
                ResultadoPaciente.Exito(paciente)
            } else {
                ResultadoPaciente.Falla(mensajeDeError(respuesta.errorBody()?.string(), respuesta.code()))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("SIGEP", "No se pudo registrar al paciente", e)
            ResultadoPaciente.Falla("Sin conexión con el servidor. No se registró al paciente.")
        }
    }

    /** Da de alta o reabre el caso. Lo permite el servidor solo a médico, administrador o coordinador. */
    suspend fun cambiarAlta(repo: RepositorioSigep, paciente: Paciente): ResultadoAlta {
        return try {
            val respuesta = RetrofitClient.pacientesApi.actualizar(paciente.id, CambioPaciente(!paciente.cerrado))
            val dto = respuesta.body()?.paciente
            if (respuesta.isSuccessful && dto != null) {
                repo.fijarCierre(paciente.id, dto.cerrado)
                sincronizarAlbergues(repo)
                ResultadoAlta.Exito
            } else {
                ResultadoAlta.Falla(mensajeDeError(respuesta.errorBody()?.string(), respuesta.code()))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("SIGEP", "No se pudo cambiar el estado del caso", e)
            ResultadoAlta.Falla("Sin conexión con el servidor. No se guardó el cambio.")
        }
    }

    private fun mensajeDeError(cuerpo: String?, codigo: Int): String {
        val texto = try {
            cuerpo?.let { JSONObject(it).optString("error") }
        } catch (e: Exception) {
            null
        }
        return texto?.takeIf { it.isNotBlank() } ?: "El servidor rechazó la solicitud ($codigo)."
    }
}
