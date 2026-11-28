"""Entrena el modelo y lo guarda en app/artefactos/. Uso:  python -m app.entrenar"""
import json

from . import modelo

if __name__ == "__main__":
    _, metricas = modelo.entrenar()
    print(json.dumps(metricas, ensure_ascii=False, indent=2))
