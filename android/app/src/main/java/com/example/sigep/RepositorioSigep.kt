package com.example.sigep

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Guarda los datos de la operación en el dispositivo (SharedPreferences + JSON).
 * Las listas son "state lists" de Compose: cualquier cambio redibuja la pantalla.
 */
class RepositorioSigep(contexto: Context) {

    private val prefs = contexto.getSharedPreferences("sigep_datos", Context.MODE_PRIVATE)
    private val gson = Gson()

    val pacientes = mutableStateListOf<Paciente>()
    val alertas = mutableStateListOf<Alerta>()
    val equipos = mutableStateListOf<Equipo>()

    init {
        pacientes.addAll(leer("pacientes", object : TypeToken<List<Paciente>>() {}))
        alertas.addAll(leer("alertas", object : TypeToken<List<Alerta>>() {}))
        equipos.addAll(leer("equipos", object : TypeToken<List<Equipo>>() {}))

        if (!prefs.contains("alertas")) {
            alertas.addAll(alertasIniciales())
            guardarAlertas()
        }
        if (!prefs.contains("equipos")) {
            equipos.addAll(equiposIniciales())
            guardarEquipos()
        }
    }

    // ---------- Pacientes ----------

    fun agregarPaciente(
        nombre: String,
        apellido: String,
        tipoSangre: String,
        centro: String,
        observaciones: String,
        triage: Triage
    ): Paciente {
        val numero = prefs.getInt("contador_pacientes", 0) + 1
        prefs.edit().putInt("contador_pacientes", numero).apply()
        val paciente = Paciente(
            id = System.currentTimeMillis(),
            numero = numero,
            nombre = nombre.trim(),
            apellido = apellido.trim(),
            tipoSangre = tipoSangre,
            centro = centro,
            observaciones = observaciones.trim(),
            triage = triage,
            creadoEn = System.currentTimeMillis()
        )
        pacientes.add(0, paciente)
        guardarPacientes()
        return paciente
    }

    fun pacientePorId(id: Long): Paciente? = pacientes.firstOrNull { it.id == id }

    fun guardarCondicion(id: Long, condicion: Condicion) =
        actualizarPaciente(id) { it.copy(condicion = condicion) }

    fun alternarAlta(id: Long) = actualizarPaciente(id) { it.copy(cerrado = !it.cerrado) }

    fun eliminarPaciente(id: Long) {
        pacientes.removeAll { it.id == id }
        guardarPacientes()
    }

    private fun actualizarPaciente(id: Long, cambio: (Paciente) -> Paciente) {
        val i = pacientes.indexOfFirst { it.id == id }
        if (i >= 0) {
            pacientes[i] = cambio(pacientes[i])
            guardarPacientes()
        }
    }

    // ---------- Alertas ----------

    fun agregarAlerta(nivel: NivelAlerta, titulo: String, ubicacion: String) {
        alertas.add(
            0,
            Alerta(
                id = System.currentTimeMillis(),
                nivel = nivel,
                titulo = titulo.trim(),
                ubicacion = ubicacion,
                creadaEn = System.currentTimeMillis(),
                estado = EstadoAlerta.EVALUACION
            )
        )
        guardarAlertas()
    }

    fun cambiarEstadoAlerta(id: Long, estado: EstadoAlerta) {
        val i = alertas.indexOfFirst { it.id == id }
        if (i >= 0) {
            alertas[i] = alertas[i].copy(estado = estado)
            guardarAlertas()
        }
    }

    fun eliminarAlerta(id: Long) {
        alertas.removeAll { it.id == id }
        guardarAlertas()
    }

    // ---------- Equipos ----------

    fun actualizarEquipo(codigo: String, estado: EstadoEquipo, albergue: String) {
        val i = equipos.indexOfFirst { it.codigo == codigo }
        if (i >= 0) {
            equipos[i] = equipos[i].copy(estado = estado, albergue = albergue)
            guardarEquipos()
        }
    }

    // ---------- Persistencia ----------

    private fun <T> leer(clave: String, tipo: TypeToken<List<T>>): List<T> {
        val json = prefs.getString(clave, null) ?: return emptyList()
        return try {
            gson.fromJson<List<T>>(json, tipo.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun guardarPacientes() =
        prefs.edit().putString("pacientes", gson.toJson(pacientes.toList())).apply()

    private fun guardarAlertas() =
        prefs.edit().putString("alertas", gson.toJson(alertas.toList())).apply()

    private fun guardarEquipos() =
        prefs.edit().putString("equipos", gson.toJson(equipos.toList())).apply()

    // ---------- Datos de arranque (alertas y equipos del operativo) ----------

    private fun alertasIniciales(): List<Alerta> {
        val ahora = System.currentTimeMillis()
        val min = 60_000L
        return listOf(
            Alerta(1, NivelAlerta.CRITICA, "Agua no segura para consumo", "La Rivera", ahora - 20 * min, EstadoAlerta.INTERVENCION),
            Alerta(2, NivelAlerta.ALTA, "Aumento de casos de diarrea", "El Pinar", ahora - 45 * min, EstadoAlerta.EVALUACION),
            Alerta(3, NivelAlerta.ALTA, "Síntomas respiratorios agrupados", "Las Flores", ahora - 60 * min, EstadoAlerta.SEGUIMIENTO),
            Alerta(4, NivelAlerta.MODERADA, "Acumulación de residuos", "El Puente", ahora - 150 * min, EstadoAlerta.EVALUACION),
            Alerta(5, NivelAlerta.MODERADA, "Escasez de insumos de higiene", "La Rivera", ahora - 240 * min, EstadoAlerta.SEGUIMIENTO),
            Alerta(6, NivelAlerta.MODERADA, "Posible foco de mosquitos", "El Pinar", ahora - 300 * min, EstadoAlerta.EVALUACION),
            Alerta(7, NivelAlerta.ALTA, "Corte de energía en albergue", "Las Flores", ahora - 600 * min, EstadoAlerta.RESUELTA),
            Alerta(8, NivelAlerta.MODERADA, "Falta de colchonetas", "El Puente", ahora - 900 * min, EstadoAlerta.RESUELTA)
        )
    }

    private fun equiposIniciales(): List<Equipo> = listOf(
        Equipo("E-01", "Vigilancia epidemiológica", 3, "El Pinar", EstadoEquipo.EN_CAMPO),
        Equipo("E-02", "Vigilancia epidemiológica", 3, "Las Flores", EstadoEquipo.EN_CAMPO),
        Equipo("E-03", "Saneamiento y agua segura", 4, "La Rivera", EstadoEquipo.EN_CAMPO),
        Equipo("E-04", "Saneamiento y agua segura", 3, "El Puente", EstadoEquipo.EN_CAMPO),
        Equipo("E-05", "Atención sanitaria", 4, "La Rivera", EstadoEquipo.EN_CAMPO),
        Equipo("E-06", "Atención sanitaria", 3, "Las Flores", EstadoEquipo.EN_CAMPO),
        Equipo("E-07", "Reserva operativa", 3, BASE_MUNICIPAL, EstadoEquipo.DISPONIBLE),
        Equipo("E-08", "Reserva operativa", 3, BASE_MUNICIPAL, EstadoEquipo.DISPONIBLE)
    )
}
