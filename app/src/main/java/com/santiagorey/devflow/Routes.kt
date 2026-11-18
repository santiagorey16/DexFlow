package com.santiagorey.devflow

import android.net.Uri
import androidx.navigation.NavHostController

// Rutas de navegación de la app.

internal object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val DASHBOARD = "dashboard"
    const val PROJECTS = "projects"
    const val GUIDES = "guides"
    const val CUSTOMERS = "customers"
    const val SETTINGS = "settings"

    const val PROJECT_NEW = "project_new?clientId={clientId}"
    fun projectNew(clientId: String? = null) =
        if (clientId == null) "project_new" else "project_new?clientId=$clientId"

    const val PROJECT_DETAIL = "project/{projectId}"
    fun projectDetail(projectId: String) = "project/$projectId"

    const val PROJECT_EDIT = "project_edit/{projectId}"
    fun projectEdit(projectId: String) = "project_edit/$projectId"

    const val CLIENT_NEW = "client_new?fromProject={fromProject}"
    fun clientNew(fromProject: Boolean = false) = "client_new?fromProject=$fromProject"

    const val CLIENT_DETAIL = "client_detail/{clientId}"
    fun clientDetail(clientId: String) = "client_detail/$clientId"

    const val AI_SETUP = "ai_setup"
    const val NOTIFICATIONS = "notifications"
    const val PRIVACY = "privacy"

    const val ASSISTANT = "assistant?projectId={projectId}&prompt={prompt}"
    fun assistant(projectId: String? = null, prompt: String? = null): String {
        val params = listOfNotNull(
            projectId?.let { "projectId=$it" },
            prompt?.let { "prompt=${Uri.encode(it)}" }
        )
        return if (params.isEmpty()) "assistant" else "assistant?" + params.joinToString("&")
    }

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

