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
