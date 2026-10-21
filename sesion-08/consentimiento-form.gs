/**
 * Crea el formulario de consentimiento informado del estudio Love Yourself
 * en Google Forms.
 *
 * Cómo usarlo:
 *   1. Entra a https://script.google.com con tu cuenta UDP → "Nuevo proyecto".
 *   2. Borra lo que aparece, pega este archivo completo y guarda.
 *   3. Arriba, elige la función "crearFormulario" y presiona "Ejecutar".
 *      La primera vez Google pide permisos: acéptalos (es tu propia cuenta).
 *   4. En "Registro de ejecución" aparecen dos enlaces: el de editar y el que
 *      se envía a los participantes. El formulario queda también en tu Drive.
 *
 * Después, en el formulario → Configuración → Respuestas:
 *   - "Recopilar correos electrónicos": "Respuestas de los encuestados"
 *   - "Enviar a los encuestados una copia de sus respuestas": "Siempre"
 *   (Apps Script no permite activar esto último desde el código.)
 */

const CONTACTO = 'santiago.gaete@mail.udp.cl';

function crearFormulario() {
  const form = FormApp.create('Consentimiento informado · Love Yourself (estudio)');
  form.setDescription(
    'Hola. Soy Santiago Gaete, estudiante de 5º año de la Escuela de Diseño de la ' +
    'Universidad Diego Portales. Te invito a participar en el estudio de mi proyecto ' +
    'de título, "Cautiverio Voluntario". Antes de aceptar, lee esta información con calma.'
  );
  form.setCollectEmail(true);
  form.setLimitOneResponsePerUser(false);
  form.setProgressBar(true);
  form.setConfirmationMessage(
    'Gracias por participar. Te escribiré para coordinar la instalación de la app. ' +
    'Si tienes dudas, escríbeme a ' + CONTACTO + '.'
  );

  // ---- 1. Información del estudio ----
  form.addSectionHeaderItem()
    .setTitle('De qué se trata')
    .setHelpText(
      'El estudio busca entender cómo se usa Instagram Reels y probar una app, Love Yourself, ' +
      'que pone pausas breves cuando el contenido llega empujado por el algoritmo (la pestaña ' +
      'Reels, las sugerencias o los anuncios), y no cuando lo eliges tú (un reel que te enviaron ' +
      'o los reels de un perfil).'
    );

  form.addSectionHeaderItem()
    .setTitle('Qué tendrías que hacer')
    .setHelpText(
      '• Instalar la app Love Yourself en tu teléfono Android y darle dos permisos: ' +
      'Accesibilidad y "Mostrar encima de otras apps".\n' +
      '• Usar Instagram como siempre durante 21 días.\n' +
      '   – Días 1 a 7: la app solo registra. No vas a ver nada distinto.\n' +
      '   – Días 8 a 21: en los Reels que no elegiste aparecen una burbuja con tu tiempo, ' +
      'un aviso con un botón para salir, una mascota y, a los 10 minutos, una pantalla para ' +
      'decidir si seguir o salir. Nunca te bloquea: siempre puedes seguir.\n' +
      '• A veces, al salir de Instagram, responder tres preguntas cortas (unos 10 segundos).\n' +
      '• Al final, exportar el registro desde la app y enviármelo.'
    );

  form.addSectionHeaderItem()
    .setTitle('Qué registra la app y qué no')
    .setHelpText(
      'Registra: cuándo usas Instagram, cuánto tiempo, de dónde viene cada reel (pestaña Reels, ' +
      'un mensaje, un perfil), si es de una cuenta que sigues o un anuncio, lo que eliges en el ' +
      'aviso y en la pantalla de los 10 minutos, y tus respuestas a las preguntas. Para comprobar ' +
      'que la app estuvo funcionando, también anota cuándo se enciende o apaga la pantalla, y el ' +
      'modelo del teléfono y la versión de Android.\n\n' +
      'No registra: imágenes ni capturas de pantalla, tus mensajes, los nombres de las cuentas ' +
      'que ves ni nada de otras apps.\n\n' +
      'El registro queda en tu teléfono hasta que tú lo exportes y me lo envíes.'
    );

  form.addSectionHeaderItem()
    .setTitle('Confidencialidad')
    .setHelpText(
      'Tus datos se identifican con un código, no con tu nombre. Solo los veremos yo, el equipo ' +
      'docente y la comisión evaluadora del proyecto. Los resultados se presentarán agrupados, ' +
      'sin que se te pueda identificar. Todos los datos se eliminan al final del semestre.'
    );

  form.addSectionHeaderItem()
    .setTitle('Riesgos, beneficios y compensación')
    .setHelpText(
      'Riesgos: mínimos. Las pausas pueden resultarte molestas. La app no cambia nada de tu ' +
      'cuenta de Instagram.\n\n' +
      'Beneficios: no hay beneficios directos. Puede que te ayude a mirar tu propio uso de otra forma.\n\n' +
      'Compensación: participar no tiene compensación económica ni de otro tipo.'
    );

  form.addSectionHeaderItem()
    .setTitle('Participación voluntaria')
    .setHelpText(
      'Participar es voluntario. Puedes retirarte cuando quieras, sin dar explicaciones y sin ' +
      'ninguna consecuencia: basta con desinstalar la app y avisarme. Si te retiras, borro los ' +
      'datos que me hayas enviado.\n\n' +
      'Contacto: Santiago Gaete · ' + CONTACTO
    );

  // ---- 2. Requisitos y consentimiento ----
  form.addPageBreakItem().setTitle('Consentimiento');

  form.addTextItem()
    .setTitle('Nombre o apodo')
    .setRequired(true);

  form.addTextItem()
    .setTitle('Código de participante')
    .setHelpText('Te lo envío junto con el enlace a este formulario (por ejemplo, P3).')
    .setRequired(true);

  form.addCheckboxItem()
    .setTitle('Requisitos')
    .setHelpText('Marca todas para continuar.')
    .setChoiceValues([
      'Tengo 18 años o más',
      'Uso un teléfono Android',
      'Tengo Instagram configurado en español'
    ])
    .setValidation(FormApp.createCheckboxValidation()
      .setHelpText('Para participar tienen que cumplirse los tres requisitos.')
      .requireSelectAtLeast(3)
      .build())
    .setRequired(true);

  form.addCheckboxItem()
    .setTitle('Declaro que')
    .setChoiceValues([
      'Leí y entendí la información sobre el estudio',
      'Entiendo qué registra la app y qué no registra',
      'Sé que puedo retirarme cuando quiera, sin consecuencias',
      'Acepto que mis datos, identificados con un código, los vean el estudiante, el equipo docente y la comisión evaluadora, y que se eliminen al final del semestre'
    ])
    .setValidation(FormApp.createCheckboxValidation()
      .setHelpText('Para participar tienes que marcar las cuatro.')
      .requireSelectAtLeast(4)
      .build())
    .setRequired(true);

  form.addMultipleChoiceItem()
    .setTitle('¿Aceptas participar en el estudio?')
    .setChoiceValues(['Sí, acepto participar', 'No acepto'])
    .setRequired(true);

  Logger.log('Editar: ' + form.getEditUrl());
  Logger.log('Enviar a participantes: ' + form.getPublishedUrl());
}
