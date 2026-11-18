# Preguntas frecuentes del asistente

Contenido base para las acciones de preguntas frecuentes en watsonx Assistant.
Corresponde a una PYME genérica de servicios; cambien los datos entre corchetes
por los de la empresa con la que hagan la demo.

Cada pregunta se crea como una **acción**. Las frases de ejemplo se copian en
"Customer starts with" (mínimo 5 por acción) y la respuesta va en el paso 1.
Al final de cada respuesta conviene ofrecer la acción **Registrar interés**.

| N.° | Acción | Frases de ejemplo | Respuesta |
|---|---|---|---|
| 1 | Horario de atención | ¿A qué hora atienden? · ¿Cuál es su horario? · ¿Atienden los sábados? · ¿Hasta qué hora están abiertos? · ¿Trabajan los domingos? | Atendemos de lunes a viernes de 9:00 a. m. a 6:00 p. m. y los sábados de 9:00 a. m. a 1:00 p. m. Por este chat te respondemos a cualquier hora. |
| 2 | Ubicación | ¿Dónde están ubicados? · ¿Cuál es su dirección? · ¿Tienen oficina? · ¿Cómo llego? · ¿En qué distrito están? | Estamos en [dirección, distrito]. También atendemos de forma virtual a todo el Perú. |
| 3 | Servicios | ¿Qué servicios ofrecen? · ¿A qué se dedican? · ¿Qué hacen? · ¿Hacen páginas web? · ¿Ofrecen marketing digital? | Ofrecemos diseño de páginas web, marketing digital en redes sociales y soporte técnico para empresas. ¿Te gustaría que un asesor te prepare una cotización? |
| 4 | Precios | ¿Cuánto cuesta? · ¿Qué precios tienen? · ¿Cuánto cobran por una página web? · Quiero saber el precio · ¿Tienen tarifas? | El precio depende de lo que necesites. Como referencia, una página web informativa parte de S/ [monto]. Si me das algunos datos, un asesor te envía una cotización exacta. |
| 5 | Formas de pago | ¿Cómo puedo pagar? · ¿Aceptan Yape? · ¿Puedo pagar con tarjeta? · ¿Aceptan transferencia? · ¿Puedo pagar en cuotas? | Aceptamos transferencia bancaria, Yape, Plin y tarjetas. Para proyectos grandes se puede pagar en dos partes: 50 % al inicio y 50 % a la entrega. |
| 6 | Tiempo de entrega | ¿Cuánto demoran? · ¿En cuánto tiempo lo entregan? · ¿Cuál es el plazo? · ¿Lo tienen para esta semana? · ¿Cuántos días se demora? | Una página web informativa toma entre 7 y 15 días hábiles. Los demás servicios se acuerdan según el alcance del proyecto. |
| 7 | Garantía y soporte | ¿Tienen garantía? · ¿Dan soporte después? · ¿Qué pasa si algo falla? · ¿Incluye mantenimiento? · ¿Me ayudan después de la entrega? | Todos nuestros trabajos incluyen 30 días de soporte sin costo después de la entrega. Luego puedes contratar un plan de mantenimiento mensual. |
| 8 | Hablar con un asesor | Quiero hablar con una persona · ¿Me puede llamar un asesor? · Necesito hablar con alguien · Pásame con un vendedor · Quiero que me contacten | Claro. Para que un asesor te contacte necesito algunos datos. *(Ir a la acción Registrar interés.)* |

## Respuesta cuando el asistente no entiende

Configurar en **Fallback** (acción del sistema "No action matches"):

> No estoy seguro de haber entendido. Puedo ayudarte con horarios, ubicación,
> servicios, precios, formas de pago y tiempos de entrega. Si prefieres, déjame
> tus datos y un asesor te contactará.
