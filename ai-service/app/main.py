"""Servicio de IA de SIGEP (FastAPI). Se ejecuta con:  uvicorn app.main:app --port 8000"""
from contextlib import asynccontextmanager

from fastapi import FastAPI

from . import modelo
from .esquemas import EntradaTriage, SalidaTriage


@asynccontextmanager
async def ciclo_de_vida(_app: FastAPI):
    modelo.cargar()  # el modelo se carga una vez al arrancar
    yield


app = FastAPI(title="SIGEP - Servicio de IA", version="0.1.0", lifespan=ciclo_de_vida)


@app.get("/health")
def salud():
    return {"estado": "ok", "modelo": modelo.VERSION, "metricas": modelo.metricas()}


@app.post("/triage/predecir", response_model=SalidaTriage)
def predecir_triage(entrada: EntradaTriage):
    """Sugiere un nivel de triage y explica qué variables pesaron. No reemplaza al médico."""
    return modelo.predecir(entrada.model_dump())
