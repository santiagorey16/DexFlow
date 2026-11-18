package com.santiagorey.devflow

import android.net.Uri
import androidx.navigation.NavHostController

/**
 * Rutas de navegación: la "dirección" de cada pantalla, como las URLs de una web.
 *
 * - Las constantes con `{llaves}` son plantillas que usa el NavHost, p. ej. "project/{projectId}".
 * - Las funciones arman la ruta real con el dato, p. ej. projectDetail("abc") = "project/abc".
 * - Lo que va después de `?` es opcional (puede no venir).
 */
internal object Routes {
    // Pantallas sin parámetros
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val DASHBOARD = "dashboard"
    const val PROJECTS = "projects"
    const val GUIDES = "guides"
    const val CUSTOMERS = "customers"
    const val SETTINGS = "settings"

    // Nuevo proyecto; clientId es opcional (viene si se crea desde la ficha de un cliente).
    const val PROJECT_NEW = "project_new?clientId={clientId}"
    /** Ruta real de "nuevo proyecto"; con clientId el formulario abre con ese cliente ya elegido. */
    fun projectNew(clientId: String? = null) =
        if (clientId == null) "project_new" else "project_new?clientId=$clientId"

    const val PROJECT_DETAIL = "project/{projectId}"
    /** Ruta real del detalle de un proyecto, p. ej. "project/abc123". */
    fun projectDetail(projectId: String) = "project/$projectId"

    const val PROJECT_EDIT = "project_edit/{projectId}"
    /** Ruta real para editar un proyecto existente. */
    fun projectEdit(projectId: String) = "project_edit/$projectId"

    // fromProject = true: al guardar, el cliente vuelve elegido al formulario de nuevo proyecto.
    const val CLIENT_NEW = "client_new?fromProject={fromProject}"
    /** Ruta real de "nuevo cliente". fromProject = true si se abrió desde el formulario de proyecto. */
    fun clientNew(fromProject: Boolean = false) = "client_new?fromProject=$fromProject"

    const val CLIENT_DETAIL = "client_detail/{clientId}"
    /** Ruta real de la ficha de un cliente. */
    fun clientDetail(clientId: String) = "client_detail/$clientId"

    const val AI_SETUP = "ai_setup"
    const val NOTIFICATIONS = "notifications"
    const val PRIVACY = "privacy"

    // Asistente: los dos parámetros son opcionales. El prompt se codifica (Uri.encode)
    // porque puede traer espacios, tildes o signos que romperían la ruta.
    const val ASSISTANT = "assistant?projectId={projectId}&prompt={prompt}"
    /** Arma la ruta del asistente agregando solo los parámetros que vienen (los dos son opcionales). */
    fun assistant(projectId: String? = null, prompt: String? = null): String {
        val params = listOfNotNull(
            projectId?.let { "projectId=$it" },
            prompt?.let { "prompt=${Uri.encode(it)}" }
        )
        return if (params.isEmpty()) "assistant" else "assistant?" + params.joinToString("&")
    }

    /** Pantallas de "fuera de sesión": al iniciar sesión estando en una de ellas, se salta al Dashboard. */
    val authRoutes = setOf(LOGIN, REGISTER, FORGOT_PASSWORD)

    /** Clave para devolver al asistente de proyecto el cliente recién creado. */
    const val RESULT_NEW_CLIENT_ID = "new_client_id"
}

/** Navega a [route] dejándola como única pantalla en la pila (no se puede volver atrás). */
internal fun NavHostController.resetTo(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

