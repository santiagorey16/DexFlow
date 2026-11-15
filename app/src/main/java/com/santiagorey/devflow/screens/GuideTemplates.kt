package com.santiagorey.devflow.screens

/** Plantilla lista para copiar y pegar en un repositorio. */
internal data class GuideTemplate(
    val title: String,
    val fileName: String,
    val guideCode: String,
    val description: String,
    val content: String
)

internal val guideTemplates = listOf(
    GuideTemplate(
        title = "README del proyecto",
        fileName = "README.md",
        guideCode = "DEV-STD-001",
        description = "Estructura mínima para que cualquiera pueda instalar, ejecutar y probar tu proyecto.",
        content = """
            # Nombre del proyecto

            Breve descripción: qué problema resuelve y para quién.

            ## Requisitos
            - Android Studio (versión) / Node (versión) / etc.

            ## Instalación
            1. Clona el repositorio: `git clone https://github.com/usuario/proyecto.git`
            2. Copia `.env.example` a `.env` y completa las variables.
            3. Instala dependencias.

            ## Ejecutar
            Describe el comando o los pasos para ejecutar la app.

            ## Pruebas
            Describe cómo correr las pruebas (por ejemplo `./gradlew test`).

            ## Estructura
            - `app/` código de la aplicación
            - `docs/` documentación adicional

            ## Autor
            Tu nombre · contacto
        """.trimIndent()
    ),
    GuideTemplate(
        title = ".gitignore para Android",
        fileName = ".gitignore",
        guideCode = "SEC-STD-002",
        description = "Evita subir builds, archivos del IDE y claves (local.properties, keystores, .env).",
        content = """
            # Builds
            /build
            /app/build
            .gradle/
            .externalNativeBuild
            .cxx

            # IDE
            .idea/
            *.iml
            .DS_Store

            # Secretos: NUNCA subirlos
            local.properties
            .env
            *.jks
            *.keystore
            *.pem
            google-services.json.bak

            # Otros
            /captures
            *.log
        """.trimIndent()
    ),
    GuideTemplate(
        title = "CI con GitHub Actions (Android)",
        fileName = ".github/workflows/android.yml",
        guideCode = "DPS-STD-004",
        description = "Compila y ejecuta las pruebas automáticamente en cada push y pull request.",
        content = """
            name: Android CI

            on:
              push:
                branches: [ main ]
              pull_request:
                branches: [ main ]

            jobs:
              build:
                runs-on: ubuntu-latest
                steps:
                  - uses: actions/checkout@v4
                  - uses: actions/setup-java@v4
                    with:
                      distribution: temurin
                      java-version: 17
                  - name: Compilar y probar
                    run: ./gradlew assembleDebug testDebugUnitTest
        """.trimIndent()
    ),
    GuideTemplate(
        title = "Guía de Conventional Commits",
        fileName = "CONTRIBUTING.md",
        guideCode = "DPS-STD-004",
        description = "Formato de mensajes de commit para el equipo, con ejemplos.",
        content = """
            # Cómo escribir commits

            Formato: `tipo(alcance): descripción en presente`

            | Tipo | Cuándo usarlo |
            |------|---------------|
            | feat | Nueva funcionalidad |
            | fix | Corrección de un error |
            | docs | Solo documentación |
            | style | Formato, sin cambiar lógica |
            | refactor | Reorganizar código sin cambiar comportamiento |
            | test | Agregar o corregir pruebas |
            | chore | Tareas de mantenimiento (dependencias, configuración) |

            Ejemplos:
            - `feat(login): agregar inicio de sesión con GitHub`
            - `fix(proyectos): corregir cálculo del avance`
            - `docs: explicar cómo ejecutar las pruebas`

            Reglas:
            - Un commit = un cambio lógico.
            - Máximo 72 caracteres en la primera línea.
        """.trimIndent()
    )
)
