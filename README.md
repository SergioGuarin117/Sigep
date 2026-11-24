# SIGEP

Sistema de Informacion y Gestion Epidemiologica Posdesastre.
Proyecto universitario: coordinacion de emergencias medicas, censo
epidemiologico posdesastre y analitica predictiva con IA.

## Estructura del repositorio

| Carpeta | Componente | Tecnologia |
|---|---|---|
| `android/` | Cliente movil (frontend) | Kotlin + Jetpack Compose |
| `backend/` | API REST y autenticacion | Node.js (Express) + JWT |
| `ai-service/` | Triage, OCR y deteccion de brotes | Python (FastAPI) - pendiente |
| `docs/` | Documentacion y alcance | Markdown |
| `docker-compose.yml` | Base de datos y backend | PostgreSQL + Docker |

El detalle de la organizacion MVC (backend y app) esta en
[`docs/arquitectura-mvc.md`](docs/arquitectura-mvc.md).

El modelo de datos (PostgreSQL + PostGIS) esta en [`docs/modelo-er.md`](docs/modelo-er.md).

## Como ejecutar

### Backend y base de datos
1. Crear `backend/.env` con `DB_PASSWORD`, `JWT_SECRET`, `DB_NAME` y `DB_USER`.
2. Desde la raiz: `docker compose up --build`
3. API en `http://localhost:3000`, PostgreSQL con PostGIS en el puerto `5434`.
4. Si ya tenias la base creada, ejecuta antes `docker compose down -v` para que se apliquen los scripts de `backend/db/init/`.

### App Android
1. Abrir la carpeta **`android/`** en Android Studio (no la raiz del repo).
2. Ejecutar en el emulador. La app llama al backend en `http://10.0.2.2:3000/`.

## Usuarios de prueba (solo desarrollo)

Contrasena para todos: `Sigep2026*`

| Correo | Rol |
|---|---|
| brigadista@sigep.test | Brigadista de campo |
| medico@sigep.test | Médico de triage |
| albergue@sigep.test | Administrador de albergue |
| ips@sigep.test | Director de IPS |
| coordinador@sigep.test | Coordinador municipal |

Las cuentas creadas desde la pantalla de registro quedan como Brigadista.

## Flujo de trabajo
Una rama por funcionalidad, con commits pequenos, integrada a `main`
mediante Pull Request. Ver la hoja de ruta en [`docs/alcance.md`](docs/alcance.md).
