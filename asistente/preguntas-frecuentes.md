# Preguntas frecuentes del asistente

Respuestas de la empresa de demostración (una PYME genérica de servicios). Son las mismas en los dos motores del chat:

- **Azure AI Language:** se cargan desde `azure/preguntas-frecuentes.tsv` (ver `README.md`).
- **Buscador local** (cuando Azure no está configurado): están en `api/src/main/java/pe/captabot/api/chat/PreguntasLocales.java`.

Si cambian una respuesta, cámbienla en los tres lugares: este archivo, el `.tsv` y `PreguntasLocales.java`.

| N.° | Tema | Frases de ejemplo | Respuesta |
|---|---|---|---|
| 1 | Horario | ¿A qué hora atienden? · ¿Cuál es su horario? · ¿Atienden los sábados? · ¿Hasta qué hora están abiertos? · ¿Trabajan los domingos? | Atendemos de lunes a viernes de 9:00 a. m. a 6:00 p. m. y los sábados de 9:00 a. m. a 1:00 p. m. Por este chat te respondo a cualquier hora. |
| 2 | Ubicación | ¿Dónde están ubicados? · ¿Cuál es su dirección? · ¿Tienen oficina? · ¿Cómo llego? · ¿En qué distrito están? | Nuestra oficina de ejemplo está en Av. Principal 123, Lima. También atendemos de forma virtual a todo el Perú. |
| 3 | Precios | ¿Cuánto cuesta? · ¿Qué precios tienen? · ¿Cuánto cobran por una página web? · Quiero saber el precio · ¿Tienen tarifas? | El precio depende de lo que necesites. Como referencia, una página web informativa parte de S/ 800. Si me das algunos datos, un asesor te envía una cotización exacta. |
| 4 | Servicios | ¿Qué servicios ofrecen? · ¿A qué se dedican? · ¿Qué hacen? · ¿Hacen páginas web? · ¿Ofrecen marketing digital? | Ofrecemos diseño de páginas web, marketing digital en redes sociales y soporte técnico para empresas. |
| 5 | Formas de pago | ¿Cómo puedo pagar? · ¿Aceptan Yape? · ¿Puedo pagar con tarjeta? · ¿Aceptan transferencia? · ¿Puedo pagar en cuotas? | Aceptamos transferencia bancaria, Yape, Plin y tarjetas. En proyectos grandes puedes pagar 50 % al inicio y 50 % a la entrega. |
| 6 | Tiempo de entrega | ¿Cuánto demoran? · ¿En cuánto tiempo lo entregan? · ¿Cuál es el plazo? · ¿Lo tienen para esta semana? · ¿Cuántos días se demora? | Una página web informativa toma entre 7 y 15 días hábiles. Los demás servicios se acuerdan según el alcance del proyecto. |
| 7 | Garantía | ¿Tienen garantía? · ¿Dan soporte después? · ¿Qué pasa si algo falla? · ¿Incluye mantenimiento? · ¿Me ayudan después de la entrega? | Todos nuestros trabajos incluyen 30 días de soporte sin costo después de la entrega. Luego puedes contratar un plan de mantenimiento mensual. |

## Otras respuestas del chat

- **Cotizar o hablar con un asesor** ("quiero cotizar", "que me llame un asesor"): el chat pide consentimiento y los datos del cliente.
- **Cuando no entiende:** «No estoy seguro de haber entendido. Puedo ayudarte con horarios, ubicación, servicios, precios, formas de pago y tiempos de entrega. También puedo pedir que un asesor te contacte.»
- **«menú»** en cualquier momento vuelve al inicio.
