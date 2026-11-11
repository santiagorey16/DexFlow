package com.santiagorey.devflow.projects

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray
import org.json.JSONObject

/**
 * Fuente única de proyectos, clientes y actividad reciente.
 *
 * Los datos se guardan en SharedPreferences por usuario (uid), así cada
 * cuenta empieza vacía y conserva su información entre reinicios de la app.
 */
class ProjectsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("devflow_data", Context.MODE_PRIVATE)

    private var userKey: String? = null

    val projects = mutableStateListOf<Project>()
    val clients = mutableStateListOf<Client>()
    val activity = mutableStateListOf<ActivityEntry>()

    var isLoaded by mutableStateOf(false)
        private set

    // =====================================================
    // SESIÓN
    // =====================================================

    /** Carga los datos del usuario indicado (null = sin sesión). */
    fun setUser(uid: String?) {
        if (uid == userKey && isLoaded) return
        userKey = uid
        projects.clear()
        clients.clear()
        activity.clear()
        if (uid != null) load(uid)
        isLoaded = true
    }

    // =====================================================
    // PROYECTOS
    // =====================================================

    fun addProject(project: Project): Project {
        projects.add(0, project)
        log(ActivityType.PROJECT, "Proyecto creado: ${project.name}")
        save()
        return project
    }

    fun getProject(id: String): Project? = projects.firstOrNull { it.id == id }

    fun updateProgress(projectId: String, progress: Int) {
        val index = projects.indexOfFirst { it.id == projectId }
        if (index == -1) return
        val old = projects[index]
        val newProgress = progress.coerceIn(0, 100)
        if (old.progress == newProgress) return
        projects[index] = old.copy(progress = newProgress)
        if (newProgress >= 100 && old.progress < 100) {
            log(ActivityType.COMPLETED, "Proyecto completado: ${old.name}")
        } else {
            log(ActivityType.PROGRESS, "${old.name} avanzó a $newProgress%")
        }
        save()
    }

    fun deleteProject(projectId: String) {
        val project = getProject(projectId) ?: return
        projects.removeAll { it.id == projectId }
        log(ActivityType.PROJECT, "Proyecto eliminado: ${project.name}")
        save()
    }

    fun assignClientToProject(projectId: String, clientId: String?) {
        val index = projects.indexOfFirst { it.id == projectId }
        if (index == -1) return
        projects[index] = projects[index].copy(clientId = clientId)
        val clientName = clientId?.let { getClient(it)?.name }
        if (clientName != null) {
            log(ActivityType.CLIENT, "$clientName asignado a ${projects[index].name}")
        }
        save()
    }

    fun projectsForClient(clientId: String): List<Project> =
        projects.filter { it.clientId == clientId }

    // =====================================================
    // CLIENTES
    // =====================================================

    fun addClient(client: Client): Client {
        clients.add(0, client)
        log(ActivityType.CLIENT, "Cliente agregado: ${client.name}")
        save()
        return client
    }

    fun updateClient(client: Client) {
        val index = clients.indexOfFirst { it.id == client.id }
        if (index == -1) return
        clients[index] = client
        save()
    }

    fun getClient(id: String): Client? = clients.firstOrNull { it.id == id }

    fun deleteClient(clientId: String) {
        val client = getClient(clientId) ?: return
        clients.removeAll { it.id == clientId }
        // Los proyectos se conservan, solo se desvinculan del cliente.
        for (i in projects.indices) {
            if (projects[i].clientId == clientId) {
                projects[i] = projects[i].copy(clientId = null)
            }
        }
        log(ActivityType.CLIENT, "Cliente eliminado: ${client.name}")
        save()
    }

    // =====================================================
    // EXPERIENCIA (XP)
    // =====================================================

    val experience: Int
        get() = projects.size * 100 +
            clients.size * 50 +
            projects.count { it.isCompleted } * 200

    // =====================================================
    // PERSISTENCIA
    // =====================================================

    private fun log(type: ActivityType, text: String) {
        activity.add(0, ActivityEntry(type = type, text = text))
        while (activity.size > MAX_ACTIVITY) activity.removeAt(activity.lastIndex)
    }

    private fun save() {
        val key = userKey ?: return
        val root = JSONObject().apply {
            put("projects", JSONArray().apply { projects.forEach { put(it.toJson()) } })
            put("clients", JSONArray().apply { clients.forEach { put(it.toJson()) } })
            put("activity", JSONArray().apply { activity.forEach { put(it.toJson()) } })
        }
        prefs.edit { putString(prefKey(key), root.toString()) }
    }

    private fun load(uid: String) {
        val raw = prefs.getString(prefKey(uid), null) ?: return
        runCatching {
            val root = JSONObject(raw)
            root.optJSONArray("projects")?.forEachObject { projects.add(it.toProject()) }
            root.optJSONArray("clients")?.forEachObject { clients.add(it.toClient()) }
            root.optJSONArray("activity")?.forEachObject { activity.add(it.toActivity()) }
        }
    }

    private fun prefKey(uid: String) = "data_$uid"

    private companion object {
        const val MAX_ACTIVITY = 20
    }
}

// =====================================================
// JSON helpers
// =====================================================

private inline fun JSONArray.forEachObject(block: (JSONObject) -> Unit) {
    for (i in 0 until length()) optJSONObject(i)?.let(block)
}

private fun Project.toJson() = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("description", description)
    put("clientId", clientId ?: JSONObject.NULL)
    put("startingPoint", startingPoint)
    put("stack", JSONArray(stack))
    put("deliveryDateMillis", deliveryDateMillis ?: JSONObject.NULL)
    put("budget", budget)
    put("progress", progress)
    put("createdAt", createdAt)
}

private fun JSONObject.toProject() = Project(
    id = getString("id"),
    name = getString("name"),
    description = optString("description"),
    clientId = if (isNull("clientId")) null else optString("clientId"),
    startingPoint = optInt("startingPoint"),
    stack = optJSONArray("stack")?.let { arr -> List(arr.length()) { arr.getString(it) } } ?: emptyList(),
    deliveryDateMillis = if (isNull("deliveryDateMillis")) null else optLong("deliveryDateMillis"),
    budget = optString("budget"),
    progress = optInt("progress"),
    createdAt = optLong("createdAt", System.currentTimeMillis())
)

private fun Client.toJson() = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("phone", phone)
    put("email", email)
    put("company", company)
    put("notes", notes)
    put("createdAt", createdAt)
}

private fun JSONObject.toClient() = Client(
    id = getString("id"),
    name = getString("name"),
    phone = optString("phone"),
    email = optString("email"),
    company = optString("company"),
    notes = optString("notes"),
    createdAt = optLong("createdAt", System.currentTimeMillis())
)

private fun ActivityEntry.toJson() = JSONObject().apply {
    put("id", id)
    put("type", type.name)
    put("text", text)
    put("timestamp", timestamp)
}

private fun JSONObject.toActivity() = ActivityEntry(
    id = getString("id"),
    type = runCatching { ActivityType.valueOf(getString("type")) }.getOrDefault(ActivityType.PROJECT),
    text = optString("text"),
    timestamp = optLong("timestamp")
)
