"""Contrato de la API: lo que recibe y devuelve POST /triage/predecir.

Los campos son los mismos que usa la API de Node. No se aceptan campos extra a proposito:
el servicio de IA nunca debe recibir nombres, documentos ni identificadores de personas.
"""
from typing import Literal, Optional

from pydantic import BaseModel, ConfigDict, Field, model_validator

Nivel = Literal["verde", "amarillo", "rojo", "negro"]


class EntradaTriage(BaseModel):
    model_config = ConfigDict(extra="forbid")

    edad: Optional[float] = Field(None, ge=0, le=120)
    gestante: Optional[bool] = None
    frecuencia_cardiaca: Optional[int] = Field(None, ge=0, le=300)
    frecuencia_respiratoria: Optional[int] = Field(None, ge=0, le=100)
    presion_sistolica: Optional[int] = Field(None, ge=0, le=300)
    presion_diastolica: Optional[int] = Field(None, ge=0, le=200)
    temperatura: Optional[float] = Field(None, ge=25, le=45)
    saturacion_oxigeno: Optional[int] = Field(None, ge=0, le=100)
    conciencia: Optional[Literal["alerta", "voz", "dolor", "inconsciente"]] = None
    sintomas: list[str] = Field(default_factory=list, max_length=20)
    estado_clinico: Optional[Literal["estable", "observacion", "grave", "critico"]] = None
    requiere_traslado: Optional[bool] = None

    @model_validator(mode="after")
    def _validar(self):
        if any(len(s) > 60 for s in self.sintomas):
            raise ValueError("cada síntoma admite máximo 60 caracteres")
        if (self.presion_sistolica is not None and self.presion_diastolica is not None
                and self.presion_diastolica > self.presion_sistolica):
            raise ValueError("la presión diastólica no puede superar la sistólica")
        return self


class Factor(BaseModel):
    variable: str
    descripcion: str
    impacto: float


class SalidaTriage(BaseModel):
    nivel: Nivel
    probabilidades: dict[str, float]
    puntaje: int
    confianza: Literal["alta", "media", "baja"]
    factores: list[Factor]
    avisos: list[str]
    version_modelo: str
