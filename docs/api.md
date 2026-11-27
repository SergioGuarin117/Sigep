# API REST (backend)

Base: `http://localhost:3000`. Cuerpos y respuestas en JSON. Los mensajes de
error vienen en `{ "error": "..." }`.

## Autenticación

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/auth/register` | público | Crea una cuenta con rol `brigadista` |
| POST | `/api/auth/login` | público | Devuelve `token` (JWT, 1 h) y `usuario` |
| GET | `/api/auth/yo` | con token | Datos del usuario autenticado (incluye `albergue_id`, `ips_id`) |

### Registro
Cuerpo: `nombre`, `apellido`, `correo` (formato válido) y `contrasena` (mínimo 6 caracteres).
Respuestas: `201` creada, `400` datos inválidos, `409` correo ya registrado.

### Login
Cuerpo: `correo`, `contrasena`. Respuestas: `200`, `400`, `401` credenciales incorrectas.

### Usar el token
Enviar en cada petición protegida: `Authorization: Bearer <token>`.

| Código | Cuándo |
|---|---|
| 401 | Falta el token, es inválido o expiró |
| 403 | El rol del usuario no tiene permiso para esa ruta |

## Gestión de usuarios (solo `coordinador_municipal`)

Todas las rutas exigen token y rol coordinador (`401` sin sesión, `403` con otro rol).

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/usuarios` | Lista usuarios con filtros y paginación |
| GET | `/api/usuarios/:id` | Un usuario |
| PATCH | `/api/usuarios/:id` | Cambia rol, asignación o estado de la cuenta |

**Filtros de `GET /api/usuarios`** (todos opcionales): `rol`, `activo` (`true`/`false`),
`q` (busca en nombre, apellido y correo), `limite` (1-200, por defecto 50) y
`desplazamiento` (por defecto 0). Respuesta: `{ total, limite, desplazamiento, usuarios: [...] }`.

**Cuerpo de `PATCH`** (cualquier subconjunto, al menos uno):

| Campo | Valor |
|---|---|
| `rol` | `brigadista`, `medico_triage`, `admin_albergue`, `director_ips`, `coordinador_municipal` |
| `albergue_id` | id de un albergue existente, o `null` para quitarlo |
| `ips_id` | id de una IPS existente, o `null` para quitarla |
| `activo` | `true` / `false` (desactivar impide iniciar sesión) |

Respuestas: `200`, `400` (dato inválido o albergue/IPS inexistente), `403` (intentar
cambiar el propio rol o desactivarse a sí mismo), `404`.

Los usuarios nuevos solo se crean por el registro público (rol `brigadista`); el coordinador
después les cambia el rol. **El rol y el estado se leen de la BD en cada petición**, así que un
cambio aplica de inmediato aunque el usuario tenga un token anterior.

## Albergues (`/api/albergues`)

| Método y ruta | Quién | Qué hace |
|---|---|---|
| `GET /api/albergues` | cualquier usuario con sesión | Lista albergues. Filtros: `activo=true\|false`, `q` (nombre o dirección) |
| `GET /api/albergues/:id` | cualquier usuario con sesión | Detalle |
| `POST /api/albergues` | `coordinador_municipal` | Crea un albergue |
| `PATCH /api/albergues/:id` | `coordinador_municipal` | Edita cualquier subconjunto de campos |

Cuerpo de `POST` / `PATCH`: `nombre` (obligatorio al crear, único), `direccion`, `capacidad_personas` (entero ≥ 0),
`latitud` y `longitud` (siempre juntas; ambas `null` para quitar la ubicación), `activo`.

Cada albergue trae `personas_alojadas` (pacientes con caso abierto asignados a él) y
`ocupacion_porcentaje` (`null` si la capacidad es 0). Errores: `400` dato inválido, `403`, `404`,
`409` nombre repetido.

## Censo: grupos familiares y pacientes

Las rutas `/api/grupos-familiares` y `/api/pacientes` las usan `brigadista`, `medico_triage`,
`admin_albergue` y `coordinador_municipal`. El `director_ips` recibe `403`: no ve datos individuales de salud.
Las listas aceptan `limite` (1 a 200, por defecto 50) y `desplazamiento`, y responden `{ total, limite, desplazamiento, ... }`.

### Grupos familiares

| Método y ruta | Qué hace |
|---|---|
| `GET /api/grupos-familiares` | Lista. Filtros: `albergue_id`, `q` (código o zona) |
| `GET /api/grupos-familiares/:id` | Detalle con la lista de `integrantes` |
| `POST /api/grupos-familiares` | Registra un grupo. El `codigo` (`GF-0001`) lo genera el servidor |
| `PATCH /api/grupos-familiares/:id` | Edita |

Campos: `albergue_id`, `zona_afectacion` (para quienes no están en albergue), `latitud` + `longitud`,
`numero_integrantes` (≥ 1, por defecto 1), `observaciones`.

### Pacientes

| Método y ruta | Qué hace |
|---|---|
| `GET /api/pacientes` | Lista. Filtros: `albergue_id`, `grupo_familiar_id`, `cerrado=true\|false`, `q` (nombre, apellido o documento) |
| `GET /api/pacientes/:id` | Detalle con `historia_clinica` |
| `POST /api/pacientes` | Registra un paciente (con historia clínica opcional) |
| `PATCH /api/pacientes/:id` | Edita datos; `cerrado` solo lo cambian médico, administrador de albergue o coordinador |

Campos: `nombres` y `apellidos` (obligatorios), `tipo_documento` + `numero_documento` (juntos; el par es único),
`fecha_nacimiento` (`AAAA-MM-DD`, no futura), `sexo` (`femenino`, `masculino`, `otro`), `tipo_sangre` (`A+` … `O-`),
`telefono`, `gestante`, `condiciones_previas`, `observaciones`, `grupo_familiar_id`, `albergue_id`,
`datos_ocr` (objeto JSON con el texto extraído y su confianza). La respuesta agrega `codigo` (`P-001`),
`nombre_completo` y `edad`. Errores: `400`, `403`, `404`, `409` (documento repetido).

### Historia clínica

Al crear un paciente se pueden enviar las listas `morbilidades`, `medicamentos` y `vacunas` (máximo 30 cada una);
se guardan todas o ninguna. También se agregan una a una:

| Método y ruta | Cuerpo |
|---|---|
| `POST /api/pacientes/:id/morbilidades` | `descripcion` (obligatoria), `codigo_cie10`, `desde` |
| `POST /api/pacientes/:id/medicamentos` | `medicamento` (obligatorio), `dosis`, `frecuencia`, `requiere_suministro` |
| `POST /api/pacientes/:id/vacunas` | `vacuna` (obligatoria), `dosis_numero`, `fecha_aplicacion` |
| `DELETE /api/pacientes/:id/<tipo>/:itemId` | Solo médico, administrador de albergue o coordinador |

## Proteger una ruta nueva (para el equipo)

```js
const { verificarToken } = require("../middlewares/authMiddleware");
const { permitirRoles } = require("../middlewares/rolesMiddleware");
const { ROLES } = require("../constants/roles");

// cualquier usuario con sesión
router.get("/albergues", verificarToken, controlador.listar);

// solo ciertos roles
router.post("/albergues", verificarToken,
  permitirRoles(ROLES.COORDINADOR_MUNICIPAL), controlador.crear);
```
Dentro del controlador, `req.usuario` trae `{ id, correo, rol }`.
