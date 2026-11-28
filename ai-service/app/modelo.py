"""Modelo de triage: entrenamiento, prediccion y explicacion.

Algoritmo: bosque aleatorio (scikit-learn) sobre signos vitales, sintomas, edad y estado clinico.
La explicacion se obtiene por oclusion: se "normaliza" cada variable y se mide cuanto baja la
probabilidad del nivel predicho; las que mas la bajan son las que mas pesaron.
"""
import json
import os
from pathlib import Path

import joblib
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, recall_score
from sklearn.model_selection import train_test_split

from . import datos
from .datos import COLUMNAS, CONCIENCIA, ESTADOS, NIVELES, NORMAL, SIGNOS, SINTOMAS

VERSION = "rf-sintetico-v1"
CARPETA = Path(os.environ.get("MODELO_DIR", Path(__file__).parent / "artefactos"))
PESO_RIESGO = {"verde": 0, "amarillo": 30, "rojo": 75, "negro": 100}

AVISO = ("Sugerencia de un modelo entrenado con datos sintéticos; no es un modelo clínico validado. "
         "Un médico debe confirmar el nivel.")

ETIQUETAS = {
    "edad": "Edad", "frecuencia_cardiaca": "Frecuencia cardíaca", "frecuencia_respiratoria": "Frecuencia respiratoria",
    "presion": "Presión arterial", "temperatura": "Temperatura", "saturacion_oxigeno": "Saturación de oxígeno",
    "conciencia": "Nivel de conciencia", "gestante": "Gestación", "requiere_traslado": "Requiere traslado",
    "estado_clinico": "Estado clínico",
}
UNIDADES = {"frecuencia_cardiaca": "lat/min", "frecuencia_respiratoria": "res/min", "temperatura": "°C",
            "saturacion_oxigeno": "%"}

_modelo = None


def entrenar(n=24000, semilla=7, guardar=True):
    X, y = datos.generar(n, semilla)
    X_ent, X_val, y_ent, y_val = train_test_split(X, y, test_size=0.2, random_state=semilla, stratify=y)
    clf = RandomForestClassifier(
        n_estimators=200, min_samples_leaf=3, class_weight="balanced_subsample",
        random_state=semilla, n_jobs=-1,
    )
    clf.fit(X_ent, y_ent)
    pred = clf.predict(X_val)
    metricas = {
        "version": VERSION,
        "muestras_entrenamiento": int(len(y_ent)),
        "muestras_validacion": int(len(y_val)),
        "exactitud": round(float(accuracy_score(y_val, pred)), 4),
        "sensibilidad_por_nivel": {
            NIVELES[i]: round(float(recall_score(y_val, pred, labels=[i], average=None)[0]), 4)
            for i in range(len(NIVELES)) if (y_val == i).any()
        },
        "nota": "Datos sintéticos: sirve para demostrar el flujo, no para decisiones clínicas.",
    }
    if guardar:
        CARPETA.mkdir(parents=True, exist_ok=True)
        joblib.dump(clf, CARPETA / "modelo.joblib")
        (CARPETA / "metricas.json").write_text(json.dumps(metricas, ensure_ascii=False, indent=2))
    return clf, metricas


def cargar():
    """Carga el modelo guardado; si no existe (desarrollo local) lo entrena en el momento."""
    global _modelo
    if _modelo is None:
        archivo = CARPETA / "modelo.joblib"
        _modelo = joblib.load(archivo) if archivo.exists() else entrenar()[0]
    return _modelo


def metricas():
    archivo = CARPETA / "metricas.json"
    return json.loads(archivo.read_text()) if archivo.exists() else None


def _vector(entrada):
    """Convierte el cuerpo de la peticion en el vector de caracteristicas (mismo orden que COLUMNAS)."""
    fila = {"edad": float(entrada.get("edad") if entrada.get("edad") is not None else 30.0)}
    medidos = 0
    for s in SIGNOS:
        valor = entrada.get(s)
        falta = valor is None
        fila[s] = NORMAL[s] if falta else float(valor)
        fila[f"falta_{s}"] = int(falta)
        medidos += 0 if falta else 1
    conciencia = entrada.get("conciencia")
    fila["conciencia"] = CONCIENCIA.index(conciencia) if conciencia in CONCIENCIA else 0
    fila["gestante"] = int(entrada.get("gestante") is True)
    fila["requiere_traslado"] = int(entrada.get("requiere_traslado") is True)
    estado = entrada.get("estado_clinico")
    fila["estado_clinico"] = ESTADOS.index(estado) if estado in ESTADOS else 0
    fila["n_medidos"] = medidos
    dados = set(entrada.get("sintomas") or [])
    for i, nombre in enumerate(SINTOMAS):
        fila[f"sintoma_{i}"] = int(nombre in dados)
    return np.array([fila[c] for c in COLUMNAS], dtype=float), medidos


def _grupos():
    """Variables que se ocluyen juntas: nombre -> columnas y valor neutro."""
    idx = {c: i for i, c in enumerate(COLUMNAS)}
    g = {
        "edad": ([idx["edad"]], [30.0]),
        "frecuencia_cardiaca": ([idx["frecuencia_cardiaca"]], [NORMAL["frecuencia_cardiaca"]]),
        "frecuencia_respiratoria": ([idx["frecuencia_respiratoria"]], [NORMAL["frecuencia_respiratoria"]]),
        "presion": ([idx["presion_sistolica"], idx["presion_diastolica"]],
                    [NORMAL["presion_sistolica"], NORMAL["presion_diastolica"]]),
        "temperatura": ([idx["temperatura"]], [NORMAL["temperatura"]]),
        "saturacion_oxigeno": ([idx["saturacion_oxigeno"]], [NORMAL["saturacion_oxigeno"]]),
        "conciencia": ([idx["conciencia"]], [0.0]),
        "gestante": ([idx["gestante"]], [0.0]),
        "requiere_traslado": ([idx["requiere_traslado"]], [0.0]),
        "estado_clinico": ([idx["estado_clinico"]], [0.0]),
    }
    for i, nombre in enumerate(SINTOMAS):
        g[f"sintoma:{nombre}"] = ([idx[f"sintoma_{i}"]], [0.0])
    return g


def _describir(variable, entrada, nivel):
    if variable.startswith("sintoma:"):
        return f"Síntoma «{variable[8:]}»"
    nombre = ETIQUETAS[variable]
    if variable == "presion":
        return f"{nombre} {entrada.get('presion_sistolica')}/{entrada.get('presion_diastolica')} mmHg"
    if variable == "conciencia":
        return f"{nombre}: {entrada.get('conciencia')}"
    if variable == "estado_clinico":
        return f"{nombre}: {entrada.get('estado_clinico')}"
    if variable in ("gestante", "requiere_traslado"):
        return nombre
    valor = entrada.get(variable)
    return f"{nombre} {valor} {UNIDADES.get(variable, 'años')}".strip()


def predecir(entrada, modelo=None, factores_max=4):
    """Devuelve nivel, probabilidades, puntaje 0-100, confianza y los factores que mas pesaron."""
    clf = modelo or cargar()
    x, medidos = _vector(entrada)
    prob = clf.predict_proba(x.reshape(1, -1))[0]
    clases = [NIVELES[int(c)] for c in clf.classes_]
    probabilidades = {n: 0.0 for n in NIVELES}
    for n, p in zip(clases, prob):
        probabilidades[n] = round(float(p), 4)
    mejor = max(probabilidades, key=probabilidades.get)
    p_mejor = probabilidades[mejor]

    confianza = "alta" if p_mejor >= 0.8 else "media" if p_mejor >= 0.6 else "baja"
    if medidos < 2:
        confianza = "baja"
    puntaje = round(sum(PESO_RIESGO[n] * p for n, p in probabilidades.items()))

    # Explicacion por oclusion: cuanto baja P(nivel predicho) al normalizar cada variable presente.
    col_clase = list(clf.classes_).index(NIVELES.index(mejor))
    grupos = _grupos()
    variantes, nombres = [], []
    for nombre, (cols, neutro) in grupos.items():
        if np.allclose(x[cols], neutro):
            continue
        v = x.copy()
        v[cols] = neutro
        variantes.append(v)
        nombres.append(nombre)
    factores = []
    if variantes and mejor != "verde":
        bajan = prob[col_clase] - clf.predict_proba(np.array(variantes))[:, col_clase]
        orden = np.argsort(-bajan)
        for i in orden[:factores_max]:
            if bajan[i] >= 0.02:
                factores.append({
                    "variable": nombres[i],
                    "descripcion": _describir(nombres[i], entrada, mejor),
                    "impacto": round(float(bajan[i]), 3),
                })

    avisos = [AVISO]
    if confianza == "baja" and mejor == "verde":
        avisos.append("Faltan signos vitales: el nivel verde no descarta gravedad.")
    return {
        "nivel": mejor,
        "probabilidades": probabilidades,
        "puntaje": puntaje,
        "confianza": confianza,
        "factores": factores,
        "avisos": avisos,
        "version_modelo": VERSION,
    }
