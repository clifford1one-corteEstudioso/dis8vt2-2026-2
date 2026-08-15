# decisión técnica: vía de implementación

| criterio | kotlin(a través de android studio) | expo.dev | ai studio |
| - | - | - | - |
| tipo de dato capturado (código vs pixeles) | código | código (usa kotlin) | código (usa kotlin) |
| permisos requeridos | activación manual servicios de accesibilidad (deterimnado po OS) | activación manual servicios de accesibilidad (deterimnado por OS) | activación manual servicios de accesibilidad (deterimnado por OS) |
| output (APK vs web app) | APK | APK | APK(kotlin) y web app |
| complejidad estimada | media | baja+media(necesita kotlin) | media + prueba y error |
| sporte nativo VLM¹ | sí | necesita kotlin | sí |
| dónde corre | local(mediaPipe/Gemma) | local y/o API | API |
| documentación | <https://kotlinlang.org/docs/home.html> |<https://docs.expo.dev/> | <https://aistudio.google.com/docs> |

¹ [Vision Language Model](https://www.nvidia.com/en-us/glossary/vision-language-models/) is an AI system built by combining a large language model (LLM) with a vision encoder, giving the LLM the ability to “see”. VLMs can process and provide advanced understanding of video, image, and text inputs supplied in the prompt to generate text responses. []

## conclusiones

- las 3 son capaces de generar apks descargables e instalables.
- para capturar los datos en pantalla, tanto ai studio como expo necesitan integración con kotlin
- debido a medidas de seguridad, ai studio puede negarse a generar cosas que necesito. Además los datos son procesados a través de una API lo que puede generar latencia, y el emulador web inestabilidad.

- lectura y entendimiento pantalla:
  - a través de AccessibilityService se leen la pantalla, y se extraen como código(botones, jerarquía, etc.).
  - estos datos son procesados usando VLM de Gemma, que corre local y es capaz de entender qué se está viendo.
