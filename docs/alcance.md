# Alcance del proyecto (resumen del documento guia)

**Plataforma de Coordinacion de Emergencias Medicas, Censo Epidemiologico
Posdesastre y Analitica Predictiva con IA.**

Decision del equipo: el cliente se desarrolla como **aplicacion Android
(dispositivo movil)** y se mantiene el diseno actual de la app.

## Roles (5)
Brigadista de Campo, Medico de Triage, Administrador de Albergue,
Director de IPS/Hospital, Coordinador Municipal de Salud.

## Funcionalidades
1. Autenticacion y control de acceso por roles (JWT).
2. Censo epidemiologico con OCR: grupos familiares, morbilidades,
   medicamentos cronicos, vacunacion.
3. Triage asistido por IA (Verde, Amarillo, Rojo, Negro) y factores de riesgo
   sanitario en albergues (agua, hacinamiento, desechos).
4. Motor predictivo de brotes por albergue.
5. Capacidad hospitalaria: camas (general, UCI, urgencias), medicamentos,
   insumos, personal.
6. Dashboard con matriz demanda vs. capacidad, recomendador de recursos y
   exportacion CSV, PDF, JSON.

## No funcionales
Seguridad (Habeas Data, JWT, roles), inferencia < 2 s, actualizacion en
tiempo real, carga rapida con poco ancho de banda, solo espanol.

## Exclusiones
Integracion con historia clinica electronica, IA autonoma sin supervision
medica, voz a texto, multilenguaje, despliegue multi-region.

## Hoja de ruta por ramas
1. `chore/reestructurar-repo` (esta)
2. `feature/modelo-datos-postgis`
3. `feature/auth-5-roles`
4. `feature/censo-familias`
5. `feature/triage-4-niveles`
6. `feature/ai-service-triage`
7. `feature/ocr-cedulas`
8. `feature/factores-riesgo-albergues`
9. `feature/capacidad-hospitalaria`
10. `feature/alertas-brotes`
11. `feature/dashboard-bi`
12. `feature/pruebas-y-docs`
