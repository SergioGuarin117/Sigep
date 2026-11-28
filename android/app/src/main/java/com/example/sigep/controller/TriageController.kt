package com.example.sigep.controller

import android.util.Log
import com.example.sigep.model.CambioPaciente
import com.example.sigep.model.ConfirmarTriage
import com.example.sigep.model.EvaluacionDto
import com.example.sigep.model.EvaluacionNueva
import com.example.sigep.model.Paciente
import com.example.sigep.model.RepositorioSigep
import com.example.sigep.model.RetrofitClient
import com.example.sigep.model.Triage
import kotlin.coroutines.cancellation.CancellationException

sealed interface ResultadoTriage {
    data class Exito(val evaluacion: EvaluacionDto) : ResultadoTriage
    data class Falla(val mensaje: String) : ResultadoTriage
}

sealed interface ResultadoHistorial {
    data class Exito(val evaluaciones: List<EvaluacionDto>) : ResultadoHistorial
    data class Falla(val mensaje: String) : ResultadoHistorial
}

/** Controlador del triage: registra evaluaciones, consulta el historial y confirma el nivel (médico). */
object TriageController {

    /**
     * Guarda la evaluación en el servidor, que calcula el nivel sugerido.
     * Si cambiaron los datos del registro que usan las reglas (gestante, condiciones previas) se actualizan antes.
     */
    suspend fun evaluar(
        repo: RepositorioSigep,
        paciente: Paciente,
        evaluacion: EvaluacionNueva,
        gestante: Boolean,
        condicionesPrevias: String
    ): ResultadoTriage {
        return try {
            val previas = condicionesPrevias.trim().take(2000)
            val cambioRegistro = gestante != paciente.gestante || previas != paciente.condicionesPrevias
            if (cambioRegistro) {
                val r = RetrofitClient.pacientesApi.actualizar(
                    paciente.id,
                    CambioPaciente(gestante = gestante, condiciones_previas = previas)
                )
                if (!r.isSuccessful) {
                    return ResultadoTriage.Falla(CensoController.mensajeDeError(r.errorBody()?.string(), r.code()))
                }
            }
            val respuesta = RetrofitClient.triageApi.evaluar(paciente.id, evaluacion)
            val dto = respuesta.body()?.evaluacion
            if (respuesta.isSuccessful && dto != null) {
                repo.fijarEvaluacion(paciente.id, dto, gestante, previas)
                ResultadoTriage.Exito(dto)
            } else {
                ResultadoTriage.Falla(CensoController.mensajeDeError(respuesta.errorBody()?.string(), respuesta.code()))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("SIGEP", "No se pudo registrar la evaluación", e)
            ResultadoTriage.Falla("Sin conexión con el servidor. No se guardó la evaluación.")
        }
    }

    suspend fun historial(pacienteId: Long): ResultadoHistorial {
        return try {
            val respuesta = RetrofitClient.triageApi.historial(pacienteId)
            val cuerpo = respuesta.body()
            if (respuesta.isSuccessful && cuerpo != null) {
                ResultadoHistorial.Exito(cuerpo.evaluaciones)
            } else {
                ResultadoHistorial.Falla(CensoController.mensajeDeError(respuesta.errorBody()?.string(), respuesta.code()))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("SIGEP", "No se pudo cargar el historial de triage", e)
            ResultadoHistorial.Falla("Sin conexión con el servidor.")
        }
    }

    /** Solo el médico de triage puede confirmar; el servidor lo valida. */
    suspend fun confirmar(
        repo: RepositorioSigep,
        pacienteId: Long,
        evaluacionId: Long,
        nivel: Triage
    ): ResultadoTriage {
        return try {
            val respuesta = RetrofitClient.triageApi.confirmar(evaluacionId, ConfirmarTriage(nivel.valor))
            val dto = respuesta.body()?.evaluacion
            if (respuesta.isSuccessful && dto != null) {
                repo.fijarEvaluacion(pacienteId, dto)
                ResultadoTriage.Exito(dto)
            } else {
                ResultadoTriage.Falla(CensoController.mensajeDeError(respuesta.errorBody()?.string(), respuesta.code()))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("SIGEP", "No se pudo confirmar el nivel", e)
            ResultadoTriage.Falla("Sin conexión con el servidor. No se confirmó el nivel.")
        }
    }
}
