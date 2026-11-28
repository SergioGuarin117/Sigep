# Servicio de IA (`ai-service`)

Servicio independiente en Python (FastAPI) que corre en su propio contenedor (`sigep-ai`, puerto 8000).
Por ahora ofrece **triage asistido**; el OCR, la detección de brotes y el recomendador llegan en ramas posteriores.

## Flujo

```
App Android --> API Node --(reglas-v1)--> sugerencia principal
                    |
                    +--> ai-service  POST /triage/predecir --> segunda opinión (modelo)
                    |
                    +--> evaluaciones_triage.explicacion_ia (JSON): motivos de las reglas + ia
```

El médico ve ambas y **confirma** el nivel final. Si el servicio de IA no responde (3 s de límite), la evaluación se guarda
igual con `sugerencia_ia.disponible = false`.

## Endpoints del servicio

| Ruta | Qué hace |
|---|---|
| `GET /health` | Estado, versión del modelo y métricas de validación |
| `POST /triage/predecir` | Recibe signos vitales, síntomas, edad, gestación, conciencia, estado clínico y traslado; devuelve `nivel`, `probabilidades`, `puntaje`, `confianza`, `factores`, `avisos` y `version_modelo`. Rechaza (422) valores fuera de rango y **cualquier campo extra** (por ejemplo `nombres`) |
| `GET /docs` | Documentación interactiva (Swagger) |

## El modelo

- **Algoritmo:** bosque aleatorio (scikit-learn, 200 árboles, clases balanceadas).
- **Entradas (28):** edad, 6 signos vitales, una marca de «no medido» por cada signo, conciencia, gestación, traslado,
  estado clínico, cuántos signos se midieron y 10 síntomas.
- **Salida:** nivel verde, amarillo, rojo o negro con la probabilidad de cada uno. El `puntaje` (0-100) es el riesgo
  esperado: 0 · verde, 30 · amarillo, 75 · rojo y 100 · negro, ponderados por su probabilidad.
- **Confianza:** `alta` con probabilidad ≥ 0,8; `media` ≥ 0,6; `baja` por debajo o con menos de 2 signos medidos.
- **Explicación:** por oclusión. Se normaliza cada variable y se mide cuánto baja la probabilidad del nivel predicho;
  las 4 que más la bajan se devuelven como `factores`.
- **Datos:** **sintéticos** (`app/datos.py`). Se simulan personas con gravedad latente y se etiquetan con una escala de
  puntos inspirada en NEWS2/START, con 5 % de ruido y signos faltantes. Exactitud de validación ≈ 93 % sobre esos
  mismos datos sintéticos.

### Límites (decirlo en la presentación)

- No hay datos clínicos reales en el proyecto: el modelo demuestra el flujo completo, **no está validado clínicamente**.
  Con datos reales de un albergue habría que reentrenarlo (`python -m app.entrenar`) y validarlo con profesionales.
- La exactitud alta se mide contra etiquetas que salen de reglas parecidas: indica que el modelo aprendió el criterio,
  no que acierte con pacientes reales.
- La IA solo sugiere; el sistema nunca asigna un nivel sin confirmación médica.

## Privacidad

Al servicio solo viajan datos clínicos. El backend no envía nombres, documentos, notas libres ni identificadores, y el
esquema del servicio rechaza campos desconocidos.

## Pruebas

- `cd ai-service && pip install -r requirements-dev.txt && pytest` (modelo, esquemas y API).
- `cd backend && npm test` (reglas y cliente de IA con un servidor falso).
