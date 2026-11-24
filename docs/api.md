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
