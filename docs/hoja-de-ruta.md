# Hoja de ruta por ramas

Cada rama agrupa un bloque coherente de trabajo (entre 2 y 5 commits). Se integra a `main` con un Pull Request propio. La columna *Depende de* indica qué debe estar integrado antes (`—` = se puede hacer en paralelo).

**Regla de trabajo:** toda rama se crea **desde `main` actualizado** (`git checkout main && git pull && git checkout -b <rama>`). Si una rama depende de otra, se espera a que la anterior esté integrada. Al integrar usar **Create a merge commit** (no *Squash*): una rama que depende de otra aún no integrada choca si la anterior se aplasta en un solo commit.

**Convención de nombres:** `feature/`, `chore/`, `fix/`, `test/`, `docs/` + descripción en minúsculas con guiones. **Commits:** `tipo(alcance): resumen` (por ejemplo `feat(api): crear paciente`).


## Hecho (8 ramas)

| # | Rama | Qué incluye | Capa | Estado |
|---|---|---|---|---|
| 0 | `chore/limpiar-main` | Repositorio limpio y `docker-compose.yml` correcto | Repo | ✅ en `main` |
| 1 | `chore/reestructurar-repo` | Carpetas `android/` y `backend/` con arquitectura MVC | Repo | ✅ en `main` |
| 2 | `feature/modelo-datos-postgis` | 17 tablas con PostGIS, vistas y datos de ejemplo | BD | ✅ en `main` |
| 3 | `feature/auth-rol-por-defecto` | Roles del sistema, registro como brigadista y usuarios de prueba | Backend + BD | ✅ en `main` |
| 4 | `feature/auth-jwt-y-roles-backend` | `verificarToken`, `permitirRoles`, validación y `GET /api/auth/yo` | Backend | ✅ en `main` |
| 5 | `feature/auth-gestion-usuarios-api` | El coordinador lista usuarios, cambia rol/asignación y activa o desactiva cuentas | Backend | ✅ en `main` |
| 6 | `feature/android-sesion-y-roles` | Sesión con rol, token, interceptor `Authorization` y manejo de 401 | Android | ✅ en `main` |
| 7 | `feature/android-navegacion-por-rol` | Pestañas según el rol + pantalla de gestión de usuarios del coordinador | Android | ✅ en `main` |

## Por hacer (17 ramas)

| # | Rama | Qué incluye | Capa | Depende de |
|---|---|---|---|---|
| 8 | `feature/api-albergues-y-censo` | Albergues, grupos familiares, pacientes e historia clínica (morbilidades, medicamentos, vacunas) | Backend | 4 |
| 9 | `feature/android-albergues-y-censo` | Pantallas Albergues y Casos con datos reales y registro del censo enviado a la API | Android | 7, 8 |
| 10 | `feature/triage-reglas-y-api` | Motor de reglas Verde/Amarillo/Rojo/Negro, evaluación y confirmación del médico | Backend | 8 |
| 11 | `feature/android-triage` | 4 niveles, signos vitales, confirmación del médico e historial | Android | 9, 10 |
| 12 | `feature/ai-triage` | Servicio FastAPI en Docker, modelo de triage, `/triage/predecir` y llamada desde el backend | IA + Backend | 10 |
| 13 | `feature/android-sugerencia-ia` | Sugerencia de la IA y su explicación al médico | Android | 11, 12 |
| 14 | `feature/android-ocr` | Captura con CameraX, texto con ML Kit y autocompletar el registro | Android | 9 |
| 15 | `feature/api-capacidad-y-riesgo` | IPS (camas, inventario, personal) y factores de riesgo del albergue | Backend | 8 |
| 16 | `feature/android-capacidad-y-riesgo` | Pantallas de capacidad hospitalaria y semáforo de riesgo | Android | 9, 15 |
| 17 | `feature/ai-brotes-y-alertas` | Detección de brotes (estadística + PostGIS) y API de alertas con tarea periódica | IA + Backend | 8, 12 |
| 18 | `feature/android-alertas-api` | Pantalla Alertas con datos reales y cambio de estado | Android | 9, 17 |
| 19 | `feature/api-matriz-y-recomendador` | Matriz demanda vs. capacidad y recomendador de recursos | Backend + IA | 15, 8 |
| 20 | `feature/android-dashboard-y-matriz` | Indicadores del coordinador, mapa, matriz y aprobar/rechazar recomendaciones | Android | 16, 19 |
| 21 | `feature/exportar-reportes` | Exportar a CSV y JSON (API) y a PDF (Android) | Backend + Android | 19 |
| 22 | `feature/android-offline-cache` | Caché local y sincronización diferida con poca conectividad | Android | 9 |
| 23 | `test/seguridad-y-pruebas` | Auditoría de accesos a datos de salud, pruebas de API/IA/Android y prueba de carga | Backend + Pruebas | 12, 17 |
| 24 | `docs/api-y-manuales` | OpenAPI, manuales de los 5 roles e informe final | Docs | — |

**Total: 25 ramas** (8 hechas, 17 por hacer).

## Orden sugerido

Se sigue el orden numérico: cada rama Android viene justo después de la API que consume. Las ramas 12 a 14 y 17 casi no dependen de las demás, así que se pueden repartir entre integrantes del equipo.
