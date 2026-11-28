package com.example.sigep.model

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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
        AlberguesCache.lista.clear()
        AlberguesCache.lista.addAll(leer("albergues", object : TypeToken<List<AlbergueDto>>() {}))
        pacientes.addAll(leer("pacientes_v2", object : TypeToken<List<Paciente>>() {}))
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

    companion object {
        /** Quita del dispositivo los datos de pacientes (se usa al cerrar sesión). */
        fun borrarDatosPersonales(contexto: Context?) {
            contexto?.getSharedPreferences("sigep_datos", Context.MODE_PRIVATE)
                ?.edit()?.remove("pacientes_v2")?.remove("pacientes")?.apply()
        }
    }

    // ---------- Albergues (vienen de la API) ----------

    fun reemplazarAlbergues(nuevos: List<AlbergueDto>) {
        AlberguesCache.lista.clear()
        AlberguesCache.lista.addAll(nuevos)
        prefs.edit().putString("albergues", gson.toJson(nuevos)).apply()
    }

    // ---------- Pacientes ----------

    /** Reemplaza la lista por la del servidor (que incluye el resumen de triage de cada paciente). */
    fun reemplazarPacientes(remotos: List<PacienteDto>) {
        val previos = pacientes.associateBy { it.id }
        val nuevos = remotos.map { aPaciente(it, previos[it.id]) }
        pacientes.clear()
        pacientes.addAll(nuevos)
        guardarPacientes()
    }

    /** Agrega un paciente recien registrado en el servidor (aun sin evaluacion de triage). */
    fun agregarDesdeServidor(dto: PacienteDto): Paciente {
        val paciente = aPaciente(dto, null)
        pacientes.removeAll { it.id == paciente.id }
        pacientes.add(0, paciente)
        guardarPacientes()
        return paciente
    }

    fun fijarCierre(id: Long, cerrado: Boolean) = actualizarPaciente(id) { it.copy(cerrado = cerrado) }

    private fun aPaciente(dto: PacienteDto, previo: Paciente?): Paciente = Paciente(
        id = dto.id,
        numero = dto.codigo?.substringAfter("P-")?.toIntOrNull() ?: previo?.numero ?: 0,
        nombre = dto.nombres,
        apellido = dto.apellidos,
        tipoSangre = dto.tipo_sangre ?: "Desconocido",
        centro = AlberguesCache.porId(dto.albergue_id)?.nombre ?: "Sin albergue",
        observaciones = dto.observaciones.orEmpty(),
        creadoEn = milisDesdeIso(dto.creado_en) ?: previo?.creadoEn ?: System.currentTimeMillis(),
        cerrado = dto.cerrado,
        gestante = dto.gestante,
        condicionesPrevias = dto.condiciones_previas.orEmpty(),
        triage = dto.triage?.let { aResumen(it) }
    )

    private fun aResumen(t: TriageResumenDto): ResumenTriage? {
        val sugerido = Triage.desdeValor(t.nivel_sugerido) ?: return null
        return ResumenTriage(
            evaluacionId = t.evaluacion_id,
            sugerido = sugerido,
            confirmado = Triage.desdeValor(t.nivel_final),
            sindrome = Sindrome.desdeValor(t.sindrome),
            estado = EstadoClinico.desdeValor(t.estado_clinico),
            requiereTraslado = t.requiere_traslado,
            evaluadoEn = milisDesdeIso(t.evaluado_en) ?: System.currentTimeMillis()
        )
    }

    /** Guarda el resultado de una evaluacion o confirmacion hecha en el servidor. */
    fun fijarEvaluacion(id: Long, e: EvaluacionDto, gestante: Boolean? = null, previas: String? = null) =
        actualizarPaciente(id) { p ->
            val sugerido = Triage.desdeValor(e.sugerencia.nivel)
            p.copy(
                gestante = gestante ?: p.gestante,
                condicionesPrevias = previas ?: p.condicionesPrevias,
                triage = if (sugerido == null) p.triage else ResumenTriage(
                    evaluacionId = e.id,
                    sugerido = sugerido,
                    confirmado = Triage.desdeValor(e.decision?.nivel_final),
                    sindrome = Sindrome.desdeValor(e.sindrome),
                    estado = EstadoClinico.desdeValor(e.estado_clinico),
                    requiereTraslado = e.requiere_traslado,
                    evaluadoEn = milisDesdeIso(e.evaluado_en) ?: System.currentTimeMillis()
                )
            )
        }

    fun pacientePorId(id: Long): Paciente? = pacientes.firstOrNull { it.id == id }

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
        prefs.edit().putString("pacientes_v2", gson.toJson(pacientes.toList())).apply()

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

/** Convierte una fecha ISO del servidor (UTC) a milisegundos; null si no se puede leer. */
fun milisDesdeIso(texto: String?): Long? {
    if (texto == null || texto.length < 19) return null
    return try {
        val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        formato.timeZone = TimeZone.getTimeZone("UTC")
        formato.parse(texto.substring(0, 19))?.time
    } catch (e: Exception) {
        null
    }
}
