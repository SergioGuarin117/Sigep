import pytest

from app import modelo
from app.esquemas import EntradaTriage


@pytest.fixture(scope="module")
def clf():
    # Modelo pequeno para que la prueba sea rapida; usa los mismos datos y algoritmo.
    return modelo.entrenar(n=8000, guardar=False)[0]


GRAVE = {
    "edad": 40, "saturacion_oxigeno": 85, "conciencia": "dolor", "frecuencia_cardiaca": 120,
    "frecuencia_respiratoria": 24, "presion_sistolica": 100, "presion_diastolica": 60,
    "temperatura": 38.7, "sintomas": ["Fiebre", "Dificultad para respirar"],
}
SANO = {
    "edad": 30, "saturacion_oxigeno": 98, "conciencia": "alerta", "frecuencia_cardiaca": 75,
    "frecuencia_respiratoria": 16, "presion_sistolica": 120, "presion_diastolica": 80, "temperatura": 36.8,
}


def test_caso_grave_es_rojo_y_explica(clf):
    r = modelo.predecir(GRAVE, clf)
    assert r["nivel"] == "rojo"
    assert r["confianza"] in ("alta", "media")
    variables = [f["variable"] for f in r["factores"]]
    assert "saturacion_oxigeno" in variables
    assert all(f["impacto"] > 0 for f in r["factores"])


def test_persona_sana_es_verde_sin_factores(clf):
    r = modelo.predecir(SANO, clf)
    assert r["nivel"] == "verde" and r["factores"] == []
    assert r["puntaje"] < 15


def test_sin_datos_es_verde_con_confianza_baja_y_aviso(clf):
    r = modelo.predecir({}, clf)
    assert r["nivel"] == "verde" and r["confianza"] == "baja"
    assert any("Faltan signos vitales" in a for a in r["avisos"])


def test_sin_signos_vitales_es_negro(clf):
    r = modelo.predecir({"conciencia": "inconsciente", "frecuencia_cardiaca": 0, "frecuencia_respiratoria": 0}, clf)
    assert r["nivel"] == "negro"


def test_probabilidades_suman_uno_y_puntaje_en_rango(clf):
    for caso in (GRAVE, SANO, {}):
        r = modelo.predecir(caso, clf)
        assert sum(r["probabilidades"].values()) == pytest.approx(1.0, abs=0.01)
        assert 0 <= r["puntaje"] <= 100


def test_el_puntaje_crece_con_la_gravedad(clf):
    assert modelo.predecir(GRAVE, clf)["puntaje"] > modelo.predecir(SANO, clf)["puntaje"]


def test_sintomas_desconocidos_se_ignoran(clf):
    r = modelo.predecir({**SANO, "sintomas": ["algo raro"]}, clf)
    assert r["nivel"] == "verde"


def test_siempre_trae_aviso_de_modelo_sintetico(clf):
    assert any("sintéticos" in a for a in modelo.predecir(SANO, clf)["avisos"])


def test_metricas_de_validacion_razonables():
    _, m = modelo.entrenar(n=8000, guardar=False)
    assert m["exactitud"] > 0.85
    assert m["sensibilidad_por_nivel"]["rojo"] > 0.75


# ---- contrato de entrada ----

def test_entrada_vacia_es_valida():
    assert EntradaTriage().sintomas == []


@pytest.mark.parametrize("campo,valor", [
    ("saturacion_oxigeno", 101), ("frecuencia_cardiaca", -1), ("temperatura", 50),
    ("conciencia", "dormido"), ("estado_clinico", "malo"), ("edad", 200),
])
def test_entrada_rechaza_valores_fuera_de_rango(campo, valor):
    with pytest.raises(Exception):
        EntradaTriage(**{campo: valor})


def test_entrada_rechaza_campos_extra_como_nombre_o_documento():
    with pytest.raises(Exception):
        EntradaTriage(nombres="Ana")
    with pytest.raises(Exception):
        EntradaTriage(documento="123")


def test_entrada_rechaza_diastolica_mayor_que_sistolica():
    with pytest.raises(Exception):
        EntradaTriage(presion_sistolica=80, presion_diastolica=100)


# ---- API (solo si FastAPI esta instalado, por ejemplo dentro de Docker o con requirements-dev) ----

def test_api_predecir_y_salud():
    pytest.importorskip("fastapi")
    pytest.importorskip("httpx")
    from fastapi.testclient import TestClient

    from app.main import app

    with TestClient(app) as c:
        assert c.get("/health").json()["estado"] == "ok"
        r = c.post("/triage/predecir", json=GRAVE)
        assert r.status_code == 200 and r.json()["nivel"] == "rojo"
        assert c.post("/triage/predecir", json={"saturacion_oxigeno": 500}).status_code == 422
        assert c.post("/triage/predecir", json={"nombres": "Ana"}).status_code == 422
