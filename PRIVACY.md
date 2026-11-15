# Política de privacidad de DevFlow

DevFlow es una app para gestionar proyectos de software con ayuda de inteligencia artificial.
Esta política explica qué datos usa la app y cómo los protegemos.

## Qué datos guardamos
Tu nombre y correo de inicio de sesión (Firebase Authentication), y los proyectos, clientes,
tareas, actividad e historial del asistente que creas en la app. Se guardan en Google Firebase
(Firestore) asociados a tu cuenta.

## Datos que se quedan en tu teléfono
Las claves de la IA que conectes y el permiso de GitHub se guardan cifrados con el Android
Keystore, solo en tu dispositivo. Nunca se envían a servidores de DevFlow y no se incluyen en
copias de seguridad.

## Inteligencia artificial
Cuando usas el asistente, el texto de tu pregunta y el contexto del proyecto (y del repositorio,
si lo vinculaste) se envían directamente desde tu teléfono al proveedor que elegiste: Google
(Gemini), Anthropic (Claude) u OpenAI (ChatGPT), bajo tu propia cuenta y sus políticas.

## GitHub
Si conectas GitHub, la app lee la información de los repositorios que elijas (commits, issues,
archivos) y puede crear issues cuando tú lo pidas. Puedes desconectarlo en Ajustes cuando quieras.

## Datos de tus clientes
Los datos de contacto de tus clientes los ingresas tú y solo se usan para llamarles o escribirles
desde la app. Eres responsable de tener su autorización.

## Con quién compartimos datos
No vendemos ni compartimos tus datos con terceros con fines comerciales. Solo se procesan en
Firebase y en los servicios que tú conectas (IA y GitHub).

## Eliminar tus datos
En **Ajustes → Privacidad y cuenta → Eliminar mi cuenta** se borran tu cuenta y todos tus datos
de Firestore de forma permanente. Las claves guardadas en el teléfono se eliminan también.

## Contacto
Si tienes dudas sobre tus datos, escríbenos desde la ficha de DevFlow en Google Play.
