"""Datos de entrenamiento SINTETICOS para el modelo de triage.

No existen datos clinicos reales en el proyecto. Se simulan personas con distintos grados de gravedad y
se etiquetan con una escala de puntos inspirada en NEWS2 (alerta temprana) y en criterios START, con un
5 % de ruido para que el modelo no memorice umbrales exactos. El resultado sirve para demostrar el flujo
completo (datos -> modelo -> API -> explicacion), NO es un modelo clinico validado.
"""
import numpy as np

NIVELES = ["verde", "amarillo", "rojo", "negro"]
CONCIENCIA = ["alerta", "voz", "dolor", "inconsciente"]
ESTADOS = ["estable", "observacion", "grave", "critico"]
SINTOMAS = [
    "Fiebre", "Diarrea", "Vómito", "Tos", "Dificultad para respirar",
    "Dolor", "Herida visible", "Deshidratación", "Erupción en piel", "Mareo",
]
SIGNOS = ["frecuencia_cardiaca", "frecuencia_respiratoria", "presion_sistolica",
          "presion_diastolica", "temperatura", "saturacion_oxigeno"]
# Valor que se usa cuando un signo no se midio (el modelo ademas recibe una marca de "faltante").
NORMAL = {
    "frecuencia_cardiaca": 80.0, "frecuencia_respiratoria": 16.0, "presion_sistolica": 120.0,
    "presion_diastolica": 80.0, "temperatura": 36.8, "saturacion_oxigeno": 98.0,
}

COLUMNAS = (
    ["edad"] + SIGNOS + [f"falta_{s}" for s in SIGNOS]
    + ["conciencia", "gestante", "requiere_traslado", "estado_clinico", "n_medidos"]
    + [f"sintoma_{i}" for i in range(len(SINTOMAS))]
)


def puntos_fr(fr, nino):
    lo, hi = (20, 30) if nino else (12, 20)
    if fr <= 8: return 3
    if fr < lo: return 1
    if fr <= hi: return 0
    if fr <= hi + 4: return 2
    return 3


def puntos_fc(fc, nino):
    lo, hi = (70, 120) if nino else (51, 90)
    if fc <= 40: return 3
    if fc < lo: return 1
    if fc <= hi: return 0
    if fc <= hi + 20: return 1
    if fc <= hi + 40: return 2
    return 3


def etiquetar(f):
    """Nivel verdadero (indice en NIVELES) de una fila de caracteristicas ya imputadas."""
    nino = f["edad"] < 12
    p = {
        "fr": puntos_fr(f["frecuencia_respiratoria"], nino) if not f["falta_frecuencia_respiratoria"] else 0,
        "fc": puntos_fc(f["frecuencia_cardiaca"], nino) if not f["falta_frecuencia_cardiaca"] else 0,
    }
    s = f["saturacion_oxigeno"]
    p["spo2"] = 0 if f["falta_saturacion_oxigeno"] else (3 if s <= 91 else 2 if s <= 93 else 1 if s <= 95 else 0)
    pas = f["presion_sistolica"]
    p["pas"] = 0 if f["falta_presion_sistolica"] else (3 if pas <= 90 or pas >= 220 else 2 if pas <= 100 else 1 if pas <= 110 else 0)
    t = f["temperatura"]
    p["temp"] = 0 if f["falta_temperatura"] else (3 if t <= 35 else 1 if t <= 36 else 0 if t <= 38 else 1 if t <= 39 else 2)
    p["con"] = 0 if f["conciencia"] == 0 else 3

    extra = 0
    if f["sintoma_4"]: extra += 2                       # dificultad para respirar
    if f["sintoma_7"]: extra += 1                       # deshidratacion
    if f["sintoma_6"]: extra += 1                       # herida visible
    if f["sintoma_1"] and f["sintoma_2"]: extra += 1    # diarrea con vomito
    if f["requiere_traslado"]: extra += 1
    if f["estado_clinico"] == 2: extra += 3
    if f["estado_clinico"] == 3: extra += 7
    if f["gestante"] and not f["falta_presion_sistolica"] and f["presion_sistolica"] >= 140: extra += 2

    total = sum(p.values()) + extra
    if f["conciencia"] == 3 and ((not f["falta_frecuencia_cardiaca"] and f["frecuencia_cardiaca"] == 0)
                                  or (not f["falta_frecuencia_respiratoria"] and f["frecuencia_respiratoria"] == 0)):
        return 3
    if total >= 7 or f["estado_clinico"] == 3:
        return 2
    if total >= 3 or max(p.values()) == 3:
        return 1
    return 0


def _persona(rng):
    """Una persona simulada: gravedad latente 0..4 que mueve los signos vitales."""
    g = rng.choice(5, p=[0.52, 0.26, 0.14, 0.07, 0.01])
    edad = float(np.clip(rng.gamma(2.2, 14.0), 0, 95))
    nino = edad < 12
    fr0, fc0 = (24, 95) if nino else (16, 76)
    v = {
        "frecuencia_cardiaca": rng.normal(fc0 + 14 * g, 9 + 3 * g),
        "frecuencia_respiratoria": rng.normal(fr0 + 3.5 * g, 2.5 + g),
        "presion_sistolica": rng.normal(122 - 9 * g, 11 + 2 * g),
        "temperatura": rng.normal(36.8 + 0.5 * g, 0.5),
        "saturacion_oxigeno": rng.normal(97.5 - 2.6 * g, 1.3 + 0.6 * g),
    }
    v["presion_diastolica"] = v["presion_sistolica"] * rng.uniform(0.55, 0.72)
    if g == 4:  # casos sin signos vitales
        v["frecuencia_cardiaca"] = float(rng.choice([0, 0, 35]))
        v["frecuencia_respiratoria"] = float(rng.choice([0, 4]))
        v["saturacion_oxigeno"] = float(rng.integers(40, 80))
    v["frecuencia_cardiaca"] = float(np.clip(round(v["frecuencia_cardiaca"]), 0, 220))
    v["frecuencia_respiratoria"] = float(np.clip(round(v["frecuencia_respiratoria"]), 0, 60))
    v["presion_sistolica"] = float(np.clip(round(v["presion_sistolica"]), 50, 230))
    v["presion_diastolica"] = float(np.clip(round(v["presion_diastolica"]), 30, min(130, v["presion_sistolica"])))
    v["temperatura"] = float(np.clip(round(v["temperatura"], 1), 33, 42))
    v["saturacion_oxigeno"] = float(np.clip(round(v["saturacion_oxigeno"]), 40, 100))

    if g == 0: con = rng.choice(4, p=[0.99, 0.01, 0, 0])
    elif g == 1: con = rng.choice(4, p=[0.93, 0.06, 0.01, 0])
    elif g == 2: con = rng.choice(4, p=[0.7, 0.2, 0.09, 0.01])
    elif g == 3: con = rng.choice(4, p=[0.3, 0.3, 0.3, 0.1])
    else: con = 3
    estado_p = [[0.8, 0.18, 0.02, 0], [0.5, 0.35, 0.14, 0.01], [0.15, 0.35, 0.4, 0.1],
                [0.02, 0.1, 0.4, 0.48], [0, 0, 0.1, 0.9]][g]
    sint_p = 0.05 + 0.12 * g
    sintomas = (rng.random(len(SINTOMAS)) < sint_p).astype(int)
    if g >= 2 and rng.random() < 0.5: sintomas[4] = 1
    return {
        "edad": edad, **v, "conciencia": int(con),
        "gestante": int(edad >= 14 and edad <= 48 and rng.random() < 0.06),
        "requiere_traslado": int(rng.random() < 0.05 + 0.2 * g),
        "estado_clinico": int(rng.choice(4, p=estado_p)),
        "sintomas": sintomas,
    }


def _fila(persona, rng, tasa_faltante):
    f = {"edad": persona["edad"]}
    n_medidos = 0
    for s in SIGNOS:
        falta = int(rng.random() < tasa_faltante)
        f[s] = NORMAL[s] if falta else persona[s]
        f[f"falta_{s}"] = falta
        n_medidos += 1 - falta
    f["conciencia"] = persona["conciencia"]
    f["gestante"] = persona["gestante"]
    f["requiere_traslado"] = persona["requiere_traslado"]
    f["estado_clinico"] = persona["estado_clinico"]
    f["n_medidos"] = n_medidos
    for i, valor in enumerate(persona["sintomas"]):
        f[f"sintoma_{i}"] = int(valor)
    return f


def generar(n=24000, semilla=7):
    """Devuelve (X, y): matriz de caracteristicas en el orden de COLUMNAS y niveles verdaderos."""
    rng = np.random.default_rng(semilla)
    X = np.zeros((n, len(COLUMNAS)))
    y = np.zeros(n, dtype=int)
    for i in range(n):
        fila = _fila(_persona(rng), rng, tasa_faltante=rng.choice([0.0, 0.15, 0.4]))
        nivel = etiquetar(fila)
        if nivel in (1, 2) and rng.random() < 0.05:   # ruido: a veces el criterio humano difiere
            nivel = int(np.clip(nivel + rng.choice([-1, 1]), 0, 2))
        X[i] = [fila[c] for c in COLUMNAS]
        y[i] = nivel
    return X, y
