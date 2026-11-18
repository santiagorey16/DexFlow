package com.santiagorey.devflow.projects

import com.google.firebase.firestore.DocumentSnapshot
import org.json.JSONArray
import org.json.JSONObject

// =====================================================
// Firestore (sesión con cuenta real)
// =====================================================

/** Proyecto → mapa clave-valor, que es lo que guarda Firestore. */
internal fun Project.toMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "description" to description,
    "clientId" to clientId,
    "startingPoint" to startingPoint,
    "stack" to stack,
    "deliveryDateMillis" to deliveryDateMillis,
    "budget" to budget,
    "progress" to progress,
    "createdAt" to createdAt,
    "repoFullName" to repoFullName,
    "tasks" to tasks.map { it.toMap() }
)

// Una sola representación de la tarea para Firestore y para el JSON local.
internal fun ProjectTask.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "title" to title,
    "phase" to phaseIndex,
    "done" to done,
    "inProgress" to inProgress,
    "trackedMs" to trackedMs,
    "timerStartedAt" to timerStartedAt,
    "issue" to issueNumber
)

/** Mapa de Firestore o JSON → tarea. Devuelve null si falta el título (dato roto). */
internal fun taskFromMap(map: Map<*, *>): ProjectTask? {
    val title = map["title"] as? String ?: return null
    return ProjectTask(
        id = map["id"] as? String ?: java.util.UUID.randomUUID().toString(),
        title = title,
        phaseIndex = (map["phase"] as? Number)?.toInt()?.coerceIn(0, 3) ?: 0,
        done = map["done"] as? Boolean ?: false,
        inProgress = map["inProgress"] as? Boolean ?: false,
        trackedMs = (map["trackedMs"] as? Number)?.toLong() ?: 0L,
        timerStartedAt = (map["timerStartedAt"] as? Number)?.toLong(),
        issueNumber = (map["issue"] as? Number)?.toInt()
    )
}

/** Lista guardada → tareas, descartando las inválidas. */
private fun Any?.toTasks(): List<ProjectTask> =
    (this as? List<*>)?.mapNotNull { (it as? Map<*, *>)?.let(::taskFromMap) } ?: emptyList()

/** JSONObject → mapa (para reutilizar taskFromMap con datos locales). */
private fun JSONObject.toMap(): Map<String, Any?> =
    keys().asSequence().associateWith { key -> opt(key).takeUnless { it == JSONObject.NULL } }

/**
 * Documento de Firestore → proyecto. null si no tiene nombre.
 * Cada campo trae un valor por defecto, así los documentos viejos se siguen leyendo.
 */
internal fun DocumentSnapshot.toProject(): Project? {
    val name = getString("name") ?: return null
    return Project(
        id = id,
        name = name,
        description = getString("description").orEmpty(),
        clientId = getString("clientId"),
        startingPoint = getLong("startingPoint")?.toInt() ?: 0,
        stack = (get("stack") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
        deliveryDateMillis = getLong("deliveryDateMillis"),
        budget = getString("budget").orEmpty(),
        progress = getLong("progress")?.toInt() ?: 0,
        createdAt = getLong("createdAt") ?: 0L,
        repoFullName = getString("repoFullName"),
        tasks = get("tasks").toTasks()
    )
}

/** Cliente → mapa para Firestore. */
internal fun Client.toMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "phone" to phone,
    "email" to email,
    "company" to company,
    "notes" to notes,
    "createdAt" to createdAt
)

/** Documento de Firestore → cliente (null si no tiene nombre). */
internal fun DocumentSnapshot.toClient(): Client? {
    val name = getString("name") ?: return null
    return Client(
        id = id,
        name = name,
        phone = getString("phone").orEmpty(),
        email = getString("email").orEmpty(),
        company = getString("company").orEmpty(),
        notes = getString("notes").orEmpty(),
        createdAt = getLong("createdAt") ?: 0L
    )
}

/** Entrada de actividad → mapa para Firestore. */
internal fun ActivityEntry.toMap(): Map<String, Any?> = mapOf(
    "type" to type.name,
    "text" to text,
    "timestamp" to timestamp
)

/** Documento de Firestore → entrada de actividad. */
internal fun DocumentSnapshot.toActivity(): ActivityEntry? {
    val text = getString("text") ?: return null
    return ActivityEntry(
        id = id,
        type = activityTypeOf(getString("type")),
        text = text,
        timestamp = getLong("timestamp") ?: 0L
    )
}

// =====================================================
// JSON (datos que versiones anteriores guardaban solo en el teléfono)
// =====================================================

/** Recorre un JSONArray objeto por objeto. */
internal inline fun JSONArray.forEachObject(block: (JSONObject) -> Unit) {
    for (i in 0 until length()) optJSONObject(i)?.let(block)
}

/** Proyecto → JSON (formato antiguo guardado solo en el teléfono). */
internal fun Project.toJson() = JSONObject().apply {
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
    put("repoFullName", repoFullName ?: JSONObject.NULL)
    put("tasks", JSONArray().apply { tasks.forEach { put(JSONObject(it.toMap().filterValues { v -> v != null })) } })
}

/** JSON del formato antiguo → proyecto. */
internal fun JSONObject.toProject() = Project(
    id = getString("id"),
    name = getString("name"),
    description = optString("description"),
    clientId = if (isNull("clientId")) null else optString("clientId"),
    startingPoint = optInt("startingPoint"),
    stack = optJSONArray("stack")?.let { arr -> List(arr.length()) { arr.getString(it) } } ?: emptyList(),
    deliveryDateMillis = if (isNull("deliveryDateMillis")) null else optLong("deliveryDateMillis"),
    budget = optString("budget"),
    progress = optInt("progress"),
    createdAt = optLong("createdAt", System.currentTimeMillis()),
    repoFullName = if (!has("repoFullName") || isNull("repoFullName")) null else optString("repoFullName"),
    tasks = optJSONArray("tasks")?.let { arr ->
        (0 until arr.length()).mapNotNull { i -> arr.optJSONObject(i)?.toMap()?.let(::taskFromMap) }
    } ?: emptyList()
)

/** Cliente → JSON (formato antiguo). */
internal fun Client.toJson() = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("phone", phone)
    put("email", email)
    put("company", company)
    put("notes", notes)
    put("createdAt", createdAt)
}

/** JSON del formato antiguo → cliente. */
internal fun JSONObject.toClient() = Client(
    id = getString("id"),
    name = getString("name"),
    phone = optString("phone"),
    email = optString("email"),
    company = optString("company"),
    notes = optString("notes"),
    createdAt = optLong("createdAt", System.currentTimeMillis())
)

/** Actividad → JSON (formato antiguo). */
internal fun ActivityEntry.toJson() = JSONObject().apply {
    put("id", id)
    put("type", type.name)
    put("text", text)
    put("timestamp", timestamp)
}

/** JSON del formato antiguo → actividad. */
internal fun JSONObject.toActivity() = ActivityEntry(
    id = getString("id"),
    type = activityTypeOf(optString("type")),
    text = optString("text"),
    timestamp = optLong("timestamp")
)

/** Texto guardado → tipo de actividad (PROJECT si no se reconoce). */
private fun activityTypeOf(name: String?): ActivityType =
    ActivityType.entries.firstOrNull { it.name == name } ?: ActivityType.PROJECT
