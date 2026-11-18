# Plan de pruebas – CaptaBot

Equipo ERROR 404 · Proyecto Integrador – IES Cibertec · Tarea en Jira: SCRUM-39
Responsable: Said Oyola (pruebas) con apoyo de todo el equipo.

## 1. Objetivo

Comprobar que CaptaBot cumple los criterios de aceptación de las 12 historias de usuario
(HU01 a HU12) y que cada cambio que se sube al repositorio mantiene el sistema funcionando.

## 2. Alcance

| Componente | Se prueba | No se prueba |
|---|---|---|
| Base de datos PostgreSQL | Creación del esquema, restricciones, vistas y datos de prueba | Rendimiento con grandes volúmenes |
| API Java (Spring Boot) | Reglas de negocio, validaciones, seguridad con clave, respuestas HTTP | Carga y estrés |
| Panel .NET | Inicio de sesión, permisos, pantallas, filtros, exportación a Excel | Compatibilidad con navegadores antiguos |
| Asistente (chat + Azure AI Language) | Preguntas frecuentes, captación de datos, validaciones y registro del prospecto | Calidad de las respuestas de Azure fuera de las frases cargadas |

## 3. Tipos de prueba y herramientas

| Tipo | Herramienta | Qué cubre | Cuándo se ejecuta |
|---|---|---|---|
| Unitarias de la API | JUnit 5, Mockito, AssertJ, MockMvc | Calificación, usuarios, seguimiento, reportes y controladores | En cada push (GitHub Actions) y con `mvn test` |
| Unitarias del panel | xUnit, WebApplicationFactory | Cliente de la API, controladores, exportación a Excel y páginas protegidas | En cada push y con `dotnet test` |
| Base de datos | psql sobre PostgreSQL 16 limpio | Que los tres scripts se ejecuten sin errores y carguen 10 prospectos | En cada push |
| Integración de la API | Postman y newman | 41 solicitudes con 55 verificaciones sobre la API real y la base de datos | En cada push y desde Postman |
| Punta a punta | curl en GitHub Actions | BD + API + panel juntos: conversación del chat, inicio de sesión real, lista, reportes, Excel y usuarios | En cada push |
| Contenedores | Docker Compose | Que el sistema completo levante con un solo comando | Cuando cambian Docker o las dependencias |
| Asistente | JUnit, curl en el CI y navegador | Conversaciones de los casos A01 a A08 | En cada push; A07 y A08 a mano |
| Aceptación | Demo con el Product Owner | Criterios de aceptación de cada historia | Al cierre de cada sprint |

## 4. Entorno de pruebas

- **Automático:** GitHub Actions (Ubuntu) con PostgreSQL 16 como servicio, Java 21 y .NET 10.
  Cada ejecución parte de una base vacía y carga `base-datos/datos-prueba/01_datos_prueba.sql`.
- **Local:** `docker compose up --build` o la instalación manual descrita en el README principal.
- **Usuarios de prueba:** `admin@captabot.pe` (Administrador) y `ventas@captabot.pe` (Vendedor),
  contraseña `Demo2026!`.

## 5. Casos de prueba por historia

Estado: **Auto** = automatizado y en verde en el CI · **Manual** = se prueba a mano.

| Historia | N.° | Caso | Resultado esperado | Estado |
|---|---|---|---|---|
| HU01 | A01 | Preguntar el horario, los precios, el pago y la entrega con distintas frases | Responde el tema correcto y ofrece un asesor | Auto |
| HU01 | A02 | Escribir algo que el asistente no entiende | Muestra el mensaje de respaldo | Auto |
| HU02 | A03 | Abrir `http://localhost:8080` y el chat | La página carga el chat y saluda sin pedir clave | Auto |
| HU03 | P01 | S/ 6000 y urgencia alta | 100 puntos, CALIENTE | Auto |
| HU03 | P02 | S/ 2500 y urgencia media | 65 puntos, TIBIO | Auto |
| HU03 | P03 | S/ 100 y urgencia baja | 15 puntos, FRIO | Auto |
| HU03 | P04 | Montos en el borde de cada tramo (S/ 4999.99, 5000, 499.99, 500) y 70 puntos exactos | Se aplica el tramo correcto; 70 puntos ya es CALIENTE | Auto |
| HU04 | A04 | Completar la conversación de captación | Pide consentimiento, nombre, celular, servicio, presupuesto y urgencia, y registra al prospecto | Auto |
| HU04 | A05 | Rechazar el consentimiento | Se despide sin guardar datos | Auto |
| HU05 | P05 | Registro válido desde la extensión | 201, mensaje con el primer nombre, guardado en etapa NUEVO | Auto |
| HU05 | P06 | Teléfono con menos de 9 dígitos | 400 con el detalle del campo | Auto |
| HU05 | P07 | Urgencia desconocida | 400 «La urgencia debe ser ALTA, MEDIA o BAJA» | Auto |
| HU05 | P08 | Solicitud sin la clave X-API-Key | 401 | Auto |
| HU05 | A06 | Escribir un celular inválido o un monto que no es número | Explica el formato y lo vuelve a pedir | Auto |
| HU01 | A07 | Con Azure configurado, preguntar con otras palabras («¿abren los domingos?») | Responde Azure AI Language; `/api/health` indica `azure-ai-language` | Manual |
| HU01 | A08 | Azure no responde o no está configurado | El chat responde con el buscador local | Auto |
| HU06 | U01 | Inicio de sesión correcto | Entra al panel con su rol | Auto |
| HU06 | U02 | Contraseña incorrecta | Mensaje «Correo o contraseña incorrectos» | Auto |
| HU06 | U03 | Abrir cualquier página sin sesión | Redirige al inicio de sesión | Auto |
| HU06 | U04 | Contraseña guardada | Se guarda cifrada con BCrypt y nunca se devuelve | Auto |
| HU06 | U05 | Formulario sin token antifalsificación | 400 | Auto |
| HU07 | U06 | Crear vendedor | 201, correo en minúsculas, activo | Auto |
| HU07 | U07 | Correo repetido (aunque cambien mayúsculas) | 409 | Auto |
| HU07 | U08 | Contraseña de menos de 8 caracteres | 400 | Auto |
| HU07 | U09 | Editar sin cambiar la contraseña | Se mantiene la contraseña anterior | Auto |
| HU07 | U10 | Desactivar usuario | Ya no puede iniciar sesión | Auto |
| HU07 | U11 | El administrador intenta desactivarse a sí mismo | El panel lo impide | Auto |
| HU07 | U12 | Un vendedor entra a Usuarios | Acceso denegado | Manual |
| HU08 | S01 | Lista sin filtros | Calientes primero, 20 por página | Auto |
| HU08 | S02 | Filtrar por calificación, etapa y fechas | Solo los que cumplen el filtro | Auto |
| HU08 | S03 | Fecha inicial posterior a la final | Aviso y sin consultar | Auto |
| HU09 | S04 | Cambiar de NUEVO a CONTACTADO | Guarda el historial con usuario y fecha | Auto |
| HU09 | S05 | Cambiar a la misma etapa | 400, no se guarda historial | Auto |
| HU10 | S06 | Agregar dos notas | Se muestran de la más reciente a la más antigua, con autor | Auto |
| HU10 | S07 | El autor edita su nota | Se guarda la fecha de edición | Auto |
| HU10 | S08 | Otro usuario edita la nota | 403 «Solo el autor puede editar la nota» | Auto |
| HU10 | S09 | Intentar borrar una nota | No existe la opción | Auto |
| HU11 | R01 | Reporte semanal | 8 semanas, incluidas las vacías, total = calientes + tibios + fríos | Auto |
| HU11 | R02 | Descargar Excel | Archivo .xlsx con tres hojas | Auto |
| HU11 | R03 | Imprimir o guardar PDF | Se imprime sin menús ni botones | Manual |
| HU12 | R04 | Tasa de conversión | Ganados / total con dos decimales | Auto |
| HU12 | R05 | Embudo | Las 5 etapas en orden | Auto |
| HU12 | R06 | Filtrar por fechas | Embudo y tasa solo del periodo | Auto |

## 6. Criterios de aceptación de una entrega

- GitHub Actions en verde: pruebas unitarias, Postman y punta a punta sin fallos.
- Los casos manuales de las historias del sprint probados en la demo con el Product Owner.
- Ningún error abierto de prioridad alta en Jira.

## 7. Registro de errores

Cada error encontrado se registra en Jira como **Error** con: pasos para reproducirlo, resultado
esperado, resultado obtenido y captura. La corrección se sube en un commit con la clave del error
(por ejemplo `SCRUM-60 Corrige el filtro por fechas`), para que quede enlazada en Jira.

## 8. Resultados al 18 de noviembre de 2026

| Prueba | Resultado |
|---|---|
| JUnit (API) | 102 pruebas, 0 fallos |
| xUnit (panel) | 50 pruebas, 0 fallos |
| Postman / newman | 41 solicitudes, 55 verificaciones, 0 fallos |
| Punta a punta (BD + API + panel) | Correcta |
| Docker Compose | Correcto |
| Asistente (A01 a A06, A08) | Automatizados con JUnit y en el CI |
| A07 con Azure AI Language | Pendiente: crear el recurso gratuito en Azure |
