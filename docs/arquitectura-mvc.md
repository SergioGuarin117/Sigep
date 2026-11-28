# Arquitectura MVC

El proyecto sigue el patron **Modelo - Vista - Controlador** tanto en el
backend como en la app Android.

## Backend (`backend/`)

```
backend/
├── index.js                 # arranca el servidor
└── src/
    ├── app.js               # configura Express y monta las rutas
    ├── config/              # db.js (PostgreSQL), env.js (puerto, JWT)
    ├── routes/              # URL -> controlador (index, auth, health)
    ├── controllers/         # logica de cada peticion
    ├── middlewares/         # verificarToken, permitirRoles, validaciones
    ├── constants/           # roles del sistema
    ├── models/              # consultas SQL (usuarioModel.js)
    └── views/               # forma JSON de las respuestas (usuarioView.js)
```

Flujo: `route -> controller -> model (SQL) -> view (JSON) -> respuesta`.

Al agregar una funcionalidad nueva (p. ej. pacientes) se crean, con el mismo
nombre: `models/pacienteModel.js`, `controllers/pacienteController.js`,
`routes/pacienteRoutes.js` (y se monta en `routes/index.js`).

## App Android (`android/app/src/main/java/com/example/sigep/`)

```
sigep/
├── MainActivity.kt          # punto de entrada (AndroidManifest) y AplicacionSIGEP
├── model/                   # datos y acceso a datos
│   ├── Modelos.kt           #   entidades: Paciente, Alerta, Equipo, Triage...
│   ├── Sesion.kt            #   enum Rol (5 roles) y SesionUsuario
│   ├── AlmacenSesion.kt     #   guarda la sesion en el almacenamiento privado de la app
│   ├── AuthApi.kt           #   contrato Retrofit + DTOs de autenticacion
│   ├── CensoApi.kt          #   albergues y pacientes (DTOs + contrato Retrofit)
│   ├── TriageApi.kt         #   evaluaciones de triage y confirmacion del medico
│   ├── RetrofitClient.kt    #   cliente HTTP (agrega Authorization: Bearer y detecta 401)
│   └── RepositorioSigep.kt  #   datos de la app
├── view/                    # interfaz (Jetpack Compose)
│   ├── Pantalla*.kt         #   pantallas
│   ├── Componentes.kt       #   componentes reutilizables
│   ├── FlujoRegistro.kt     #   formulario de registro de pacientes
│   └── theme/               #   colores, tipografia, tema
└── controller/              # logica que conecta vista y modelo
    ├── AuthController.kt    #   login y registro (llama a la API)
    ├── CensoController.kt   #   albergues y pacientes (sincroniza y registra)
    ├── TriageController.kt  #   evaluar, historial y confirmar el nivel (medico)
    ├── SesionController.kt  #   quien esta conectado y con que rol (StateFlow)
    └── Navegacion.kt        #   pestanas (Pestana) y rutas (RutaCasos)
```

Reglas de dependencia: `model` no conoce a `view` ni a `controller`;
`view` usa `model` y `controller`; `controller` usa `model`.

Pendiente en ramas siguientes: mover la logica de estado que aun vive dentro
de algunas pantallas (casos, alertas, equipos) a controladores propios.

## Triage en la app Android

- El nivel de triage (**Rojo, Amarillo, Verde, Negro**) ya no se elige a mano: lo **sugiere el servidor**
  al guardar una evaluación clínica (signos vitales, síntomas, estado) y lo **confirma un médico de triage**.
- Mientras no se confirma, la app muestra «Nivel sugerido» y «Pendiente de confirmación médica»; el detalle
  del paciente trae los motivos de la sugerencia, el puntaje, los datos que faltaron y el historial.
- La lista de Casos filtra por nivel, «Sin evaluar» y «Por confirmar» (estos últimos de más a menos grave).
- La condición clínica ya no se guarda en el dispositivo: vive en el servidor (`evaluaciones_triage`).

## Sesión en la app Android

- Al iniciar sesión (o al registrarse, que inicia sesión automáticamente) el token y los datos del
  usuario se guardan en `AlmacenSesion`; al reabrir la app la sesión sigue abierta.
- `SesionController.sesion` es un `StateFlow<SesionUsuario?>`: si es `null` se muestra el login; si no,
  la app. Las pantallas leen el rol con `sesion.rol` (enum `Rol`).
- `RetrofitClient` agrega `Authorization: Bearer <token>` a toda petición. Si el servidor responde
  **401** a una petición con token (token vencido o cuenta desactivada), la sesión se cierra y se avisa.
- Al abrir la app se llama a `GET /api/auth/yo` para actualizar rol y nombre (el coordinador puede
  haberlos cambiado). Sin conexión se conserva la sesión guardada.

## Pestañas de la app según el rol

La barra inferior se arma con `Pestana.paraRol(rol)` (`controller/Navegacion.kt`). Cada rol ve como máximo 5 pestañas y la pantalla de inicio oculta los accesos a lo que su rol no ve. La seguridad real sigue estando en el backend (`permitirRoles`); ocultar una pestaña es solo comodidad.

| Rol | Pestañas |
|---|---|
| Brigadista de campo, Médico de triage | Inicio, Alertas, Casos |
| Administrador de albergue | Inicio, Alertas, Casos, Equipos, Reportes |
| Director de IPS | Inicio, Alertas, Equipos, Reportes |
| Coordinador municipal | Inicio, Alertas, Casos, Reportes, Usuarios |

La pestaña **Usuarios** (solo coordinador) lista cuentas con búsqueda y filtro por rol, y permite cambiar el rol o activar/desactivar una cuenta con `GET` y `PATCH /api/usuarios`.
