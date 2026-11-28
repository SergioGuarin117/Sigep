# ai-service

Servicio de Inteligencia Artificial (Python, FastAPI) de SIGEP.

## Triage asistido

`POST /triage/predecir` recibe signos vitales, síntomas, edad y estado clínico (sin nombres ni
identificadores) y devuelve un **nivel sugerido** (verde, amarillo, rojo o negro), las probabilidades de
cada nivel, un puntaje 0-100, la confianza y los **factores que más pesaron**. `GET /health` informa la
versión del modelo y sus métricas.

- Modelo: bosque aleatorio (scikit-learn). La explicación se calcula por oclusión: se normaliza cada
  variable y se mide cuánto baja la probabilidad del nivel predicho.
- Datos: **sintéticos** (`app/datos.py`), etiquetados con una escala de puntos inspirada en NEWS2/START y 5 % de
  ruido. No hay datos clínicos reales en el proyecto, así que el modelo es una demostración del flujo y
  **no está validado clínicamente**. El médico siempre confirma el nivel.
- El modelo se entrena al construir la imagen de Docker (`python -m app.entrenar`, unos segundos).

## Ejecutar

Con Docker (lo normal): `docker compose up -d --build` en la raíz del repositorio; el servicio queda en
`http://localhost:8000` (documentación interactiva en `/docs`).

Sin Docker:

```
cd ai-service
python -m venv .venv && .venv\Scripts\activate        # en Linux/macOS: source .venv/bin/activate
pip install -r requirements-dev.txt
python -m app.entrenar
uvicorn app.main:app --port 8000
pytest
```

## Después

Lectura OCR de documentos, detección espacio-temporal de brotes y recomendador de recursos llegan en ramas posteriores.
