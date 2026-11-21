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
│   ├── AuthApi.kt           #   contrato Retrofit + DTOs de autenticacion
│   ├── RetrofitClient.kt    #   cliente HTTP
│   └── RepositorioSigep.kt  #   datos de la app
├── view/                    # interfaz (Jetpack Compose)
│   ├── Pantalla*.kt         #   pantallas
│   ├── Componentes.kt       #   componentes reutilizables
│   ├── FlujoRegistro.kt     #   formulario de registro de pacientes
│   └── theme/               #   colores, tipografia, tema
└── controller/              # logica que conecta vista y modelo
    ├── AuthController.kt    #   login y registro (llama a la API)
    └── Navegacion.kt        #   pestanas (Pestana) y rutas (RutaCasos)
```

Reglas de dependencia: `model` no conoce a `view` ni a `controller`;
`view` usa `model` y `controller`; `controller` usa `model`.

Pendiente en ramas siguientes: mover la logica de estado que aun vive dentro
de algunas pantallas (casos, alertas, equipos) a controladores propios.
