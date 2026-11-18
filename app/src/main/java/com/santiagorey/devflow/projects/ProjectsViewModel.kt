package com.santiagorey.devflow.projects

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import org.json.JSONArray
import org.json.JSONObject

/**
 * Fuente única de proyectos, clientes y actividad reciente.
 *
 * - Cuenta real (Firebase): los datos viven en Firestore en `users/{uid}/...`, así se
 *   conservan al cambiar de teléfono. Firestore guarda una copia offline, por lo que la
 *   app sigue funcionando sin conexión y sincroniza cuando vuelve la red.
 *
 * La primera vez que una cuenta real abre la app, los datos que tenía guardados localmente
 * (versiones anteriores) se suben a Firestore automáticamente.
 */
class ProjectsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("devflow_data", Context.MODE_PRIVATE)
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /** Clave de la cuenta activa (uid de Firebase). */
    private var userKey: String? = null

    /** uid cuando los datos se sincronizan con Firestore; null en modo local. */
    private var remoteUid: String? = null

    private val listeners = mutableListOf<ListenerRegistration>()

    val projects = mutableStateListOf<Project>()
    val clients = mutableStateListOf<Client>()
    val activity = mutableStateListOf<ActivityEntry>()

    var isLoaded by mutableStateOf(false)
        private set

    /** Último error de sincronización con la nube (para mostrarlo una vez). */
    var syncError by mutableStateOf<String?>(null)
        private set

    /** La pantalla ya mostró el error: se borra para no repetirlo. */
    fun clearSyncError() {
        syncError = null
    }

    // =====================================================
    // SESIÓN
    // =====================================================

    /**
     * Carga los datos de la cuenta indicada.
     *
     * @param key uid de Firebase o null (sin sesión).
     * @param remote true si [key] es una cuenta real que debe sincronizarse con Firestore.
     */
    fun setUser(key: String?, remote: Boolean) {
        if (key == userKey && isLoaded) return
        stopSync()
        userKey = key
        remoteUid = if (remote) key else null
        projects.clear()
        clients.clear()
        activity.clear()

        if (key != null) {
            if (remote) {
                migrateLocalDataIfNeeded(key)
                startSync(key)
            } else {
                loadLocal(key)
            }
        }
        isLoaded = true
    }

    /** El ViewModel se destruye: se quitan los listeners de Firestore. */
    override fun onCleared() {
        super.onCleared()
        stopSync()
    }

    // =====================================================
    // PROYECTOS
    // =====================================================

    /**
     * Crea un proyecto: aparece al instante en la lista y se envía a Firestore
     * (actualización optimista). También registra la actividad.
     */
    fun addProject(project: Project): Project {
        projects.add(0, project)
        writeProject(project)
        log(ActivityType.PROJECT, "Proyecto creado: ${project.name}")
        persistLocal()
        return project
    }

    /** Busca un proyecto por su id (null si no existe o se borró). */
    fun getProject(id: String): Project? = projects.firstOrNull { it.id == id }

    /** Guarda los cambios hechos desde "Editar proyecto" (conserva avance y fecha de creación). */
    fun updateProject(project: Project) {
        val index = projects.indexOfFirst { it.id == project.id }
        if (index == -1) return
        projects[index] = project
        writeProject(project)
        log(ActivityType.PROJECT, "Proyecto actualizado: ${project.name}")
        persistLocal()
    }

    /** Cambia el avance manual (0–100). Registra "completado" si llega a 100. */
    fun updateProgress(projectId: String, progress: Int) {
        val index = projects.indexOfFirst { it.id == projectId }
        if (index == -1) return
        val old = projects[index]
        val newProgress = progress.coerceIn(0, 100)
        if (old.progress == newProgress) return
        val updated = old.copy(progress = newProgress)
        projects[index] = updated
        writeProject(updated)
        if (newProgress >= 100 && old.progress < 100) {
            log(ActivityType.COMPLETED, "Proyecto completado: ${old.name}")
        } else {
            log(ActivityType.PROGRESS, "${old.name} avanzó a $newProgress%")
        }
        persistLocal()
    }

    /** Borra el proyecto de la lista y de Firestore. */
    fun deleteProject(projectId: String) {
        val project = getProject(projectId) ?: return
        projects.removeAll { it.id == projectId }
        remoteCollection(PROJECTS)?.document(projectId)?.delete()?.addOnFailureListener(::onSyncFailure)
        log(ActivityType.PROJECT, "Proyecto eliminado: ${project.name}")
        persistLocal()
    }

    /** Asigna un cliente al proyecto, o lo quita con clientId = null. */
    fun assignClientToProject(projectId: String, clientId: String?) {
        val index = projects.indexOfFirst { it.id == projectId }
        if (index == -1) return
        val updated = projects[index].copy(clientId = clientId)
        projects[index] = updated
        writeProject(updated)
        val clientName = clientId?.let { getClient(it)?.name }
        if (clientName != null) {
            log(ActivityType.CLIENT, "$clientName asignado a ${updated.name}")
        }
        persistLocal()
    }

    // =====================================================
    // TAREAS (el avance se calcula con las tareas completadas)
    // =====================================================

    /** Agrega una tarea a una fase (ignora títulos vacíos). */
    fun addTask(projectId: String, title: String, phaseIndex: Int) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        updateTasks(projectId) { it + ProjectTask(title = clean, phaseIndex = phaseIndex.coerceIn(0, 3)) }
    }

    /** Agrega varias tareas de una vez (las sugeridas por la IA). */
    fun addTasks(projectId: String, newTasks: List<ProjectTask>) {
        if (newTasks.isEmpty()) return
        updateTasks(projectId) { it + newTasks }
    }

    /** Marca una tarea como hecha, o la devuelve a "Por hacer". */
    fun toggleTask(projectId: String, taskId: String) = updateTask(projectId, taskId) {
        it.withStatus(if (it.done) TaskStatus.TODO else TaskStatus.DONE)
    }

    /** Mueve la tarea a otra columna del Kanban. */
    fun setTaskStatus(projectId: String, taskId: String, status: TaskStatus) =
        updateTask(projectId, taskId) { it.withStatus(status) }

    /** Inicia el cronómetro de una tarea. */
    fun startTaskTimer(projectId: String, taskId: String) = updateTask(projectId, taskId) { it.startTimer() }

    /** Detiene el cronómetro de una tarea. */
    fun stopTaskTimer(projectId: String, taskId: String) = updateTask(projectId, taskId) { it.stopTimer() }

    /** Guarda el número del issue de GitHub creado para la tarea. */
    fun linkTaskIssue(projectId: String, taskId: String, issueNumber: Int) =
        updateTask(projectId, taskId) { it.copy(issueNumber = issueNumber) }

    /** Marca como hechas las tareas cuyo issue se cerró en GitHub (número → cerrado). */
    fun applyIssueStates(projectId: String, closedByNumber: Map<Int, Boolean>) {
        updateTasks(projectId) { tasks ->
            tasks.map { task ->
                val closed = task.issueNumber?.let { closedByNumber[it] }
                if (closed == true && !task.done) task.withStatus(TaskStatus.DONE) else task
            }
        }
    }

    /** Cambia UNA tarea aplicándole [transform] (atajo de updateTasks). */
    private fun updateTask(projectId: String, taskId: String, transform: (ProjectTask) -> ProjectTask) =
        updateTasks(projectId) { tasks -> tasks.map { if (it.id == taskId) transform(it) else it } }

    /** Borra una tarea. */
    fun deleteTask(projectId: String, taskId: String) {
        updateTasks(projectId) { tasks -> tasks.filterNot { it.id == taskId } }
    }

    /**
     * Punto común de todos los cambios de tareas: aplica [transform], recalcula el avance
     * con las tareas, guarda en Firestore y registra si el proyecto se completó.
     * Si nada cambió, no hace nada (evita escrituras de más).
     */
    private fun updateTasks(projectId: String, transform: (List<ProjectTask>) -> List<ProjectTask>) {
        val index = projects.indexOfFirst { it.id == projectId }
        if (index == -1) return
        val old = projects[index]
        val withTasks = old.copy(tasks = transform(old.tasks))
        // Sin tareas se conserva el avance manual; con tareas se calcula.
        val updated = withTasks.copy(progress = withTasks.progressFromTasks ?: old.progress)
        if (updated == old) return
        projects[index] = updated
        writeProject(updated)
        if (updated.progress >= 100 && old.progress < 100) {
            log(ActivityType.COMPLETED, "Proyecto completado: ${updated.name}")
        }
        persistLocal()
    }

    /** Vincula (o desvincula con null) un repositorio de GitHub al proyecto. */
    fun linkRepo(projectId: String, repoFullName: String?) {
        val index = projects.indexOfFirst { it.id == projectId }
        if (index == -1) return
        val updated = projects[index].copy(repoFullName = repoFullName)
        if (updated == projects[index]) return
        projects[index] = updated
        writeProject(updated)
        if (repoFullName != null) {
            log(ActivityType.PROJECT, "${updated.name} vinculado a GitHub: $repoFullName")
        }
        persistLocal()
    }

    /** Proyectos asignados a un cliente. */
    fun projectsForClient(clientId: String): List<Project> =
        projects.filter { it.clientId == clientId }

    // =====================================================
    // CLIENTES
    // =====================================================

    /** Crea un cliente (al instante en la lista y luego en Firestore). */
    fun addClient(client: Client): Client {
        clients.add(0, client)
        writeClient(client)
        log(ActivityType.CLIENT, "Cliente agregado: ${client.name}")
        persistLocal()
        return client
    }

    /** Guarda los cambios de un cliente. */
    fun updateClient(client: Client) {
        val index = clients.indexOfFirst { it.id == client.id }
        if (index == -1) return
        clients[index] = client
        writeClient(client)
        persistLocal()
    }

    /** Busca un cliente por su id. */
    fun getClient(id: String): Client? = clients.firstOrNull { it.id == id }

    /** Borra un cliente. Sus proyectos NO se borran: solo quedan sin cliente. */
    fun deleteClient(clientId: String) {
        val client = getClient(clientId) ?: return
        clients.removeAll { it.id == clientId }
        remoteCollection(CLIENTS)?.document(clientId)?.delete()?.addOnFailureListener(::onSyncFailure)
        // Los proyectos se conservan, solo se desvinculan del cliente.
        for (i in projects.indices) {
            if (projects[i].clientId == clientId) {
                val updated = projects[i].copy(clientId = null)
                projects[i] = updated
                writeProject(updated)
            }
        }
        log(ActivityType.CLIENT, "Cliente eliminado: ${client.name}")
        persistLocal()
    }

    // =====================================================
    // EXPERIENCIA (XP)
    // =====================================================

    val experience: Int
        get() = experienceFor(projects, clients)

    // =====================================================
    // ACTIVIDAD
    // =====================================================

    /** Registra una acción en la actividad reciente y borra las más viejas (solo se guardan 20). */
    private fun log(type: ActivityType, text: String) {
        val entry = ActivityEntry(type = type, text = text)
        activity.add(0, entry)
        remoteCollection(ACTIVITY)?.document(entry.id)?.set(entry.toMap())
            ?.addOnFailureListener(::onSyncFailure)
        // Solo se conservan las últimas MAX_ACTIVITY entradas.
        while (activity.size > MAX_ACTIVITY) {
            val removed = activity.removeAt(activity.lastIndex)
            remoteCollection(ACTIVITY)?.document(removed.id)?.delete()
        }
    }

    // =====================================================
    // FIRESTORE
    // =====================================================

    /** Referencia a users/{uid}/{name} en Firestore. */
    private fun userCollection(uid: String, name: String): CollectionReference =
        firestore.collection("users").document(uid).collection(name)

    /** Colección de la cuenta activa, o null sin sesión (entonces no se escribe en la nube). */
    private fun remoteCollection(name: String): CollectionReference? =
        remoteUid?.let { userCollection(it, name) }

    /** Guarda (o reemplaza) el proyecto en Firestore. Sin internet, queda en cola. */
    private fun writeProject(project: Project) {
        remoteCollection(PROJECTS)?.document(project.id)?.set(project.toMap())
            ?.addOnFailureListener(::onSyncFailure)
    }

    /** Guarda (o reemplaza) el cliente en Firestore. */
    private fun writeClient(client: Client) {
        remoteCollection(CLIENTS)?.document(client.id)?.set(client.toMap())
            ?.addOnFailureListener(::onSyncFailure)
    }

    /**
     * Escucha en vivo proyectos, clientes y actividad de la cuenta.
     * Cada cambio (también desde otro teléfono) reemplaza la lista y Compose redibuja.
     * uid != remoteUid descarta respuestas tardías de una cuenta anterior.
     */
    private fun startSync(uid: String) {
        listeners += userCollection(uid, PROJECTS).addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener onSyncFailure(error)
            if (snapshot == null || uid != remoteUid) return@addSnapshotListener
            projects.replaceWith(
                snapshot.documents.mapNotNull { it.toProject() }.sortedByDescending { it.createdAt }
            )
        }
        listeners += userCollection(uid, CLIENTS).addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener onSyncFailure(error)
            if (snapshot == null || uid != remoteUid) return@addSnapshotListener
            clients.replaceWith(
                snapshot.documents.mapNotNull { it.toClient() }.sortedByDescending { it.createdAt }
            )
        }
        listeners += userCollection(uid, ACTIVITY)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(MAX_ACTIVITY.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener onSyncFailure(error)
                if (snapshot == null || uid != remoteUid) return@addSnapshotListener
                activity.replaceWith(snapshot.documents.mapNotNull { it.toActivity() })
            }
    }

    /** Deja de escuchar Firestore (al cambiar de cuenta o al destruirse el ViewModel). */
    private fun stopSync() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }

    /** Sube a Firestore los datos que la cuenta tenía guardados solo en el teléfono. */
    private fun migrateLocalDataIfNeeded(uid: String) {
        val raw = prefs.getString(localKey(uid), null) ?: return
        val local = runCatching { parseLocal(raw) }.getOrNull()
        if (local == null || local.isEmpty) {
            prefs.edit { remove(localKey(uid)) }
            return
        }

        // Se muestran de inmediato mientras se suben.
        projects.addAll(local.projects)
        clients.addAll(local.clients)
        activity.addAll(local.activity)

        val batch = firestore.batch()
        local.projects.forEach { batch.set(userCollection(uid, PROJECTS).document(it.id), it.toMap()) }
        local.clients.forEach { batch.set(userCollection(uid, CLIENTS).document(it.id), it.toMap()) }
        local.activity.forEach { batch.set(userCollection(uid, ACTIVITY).document(it.id), it.toMap()) }
        batch.commit()
            .addOnSuccessListener { prefs.edit { remove(localKey(uid)) } }
            .addOnFailureListener(::onSyncFailure)
    }

    /** Traduce el error de Firestore a un mensaje que la pantalla muestra en un Toast. */
    private fun onSyncFailure(error: Exception) {
        Log.e(TAG, "Error de sincronización con Firestore", error)
        val code = (error as? FirebaseFirestoreException)?.code
        syncError = when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Firestore rechazó el acceso. Revisa las reglas de seguridad en Firebase."
            FirebaseFirestoreException.Code.NOT_FOUND,
            FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                "La base de datos Firestore no está creada en Firebase."
            else -> "No se pudo sincronizar con la nube. Tus cambios se guardarán al reconectar."
        }
    }

    // =====================================================
    // ALMACENAMIENTO LOCAL (datos de versiones anteriores, se migran a Firestore)
    // =====================================================

    /** Guarda los datos en el teléfono (solo en modo sin cuenta; con cuenta se usa Firestore). */
    private fun persistLocal() {
        if (remoteUid != null) return
        val key = userKey ?: return
        val root = JSONObject().apply {
            put("projects", JSONArray().apply { projects.forEach { put(it.toJson()) } })
            put("clients", JSONArray().apply { clients.forEach { put(it.toJson()) } })
            put("activity", JSONArray().apply { activity.forEach { put(it.toJson()) } })
        }
        prefs.edit { putString(localKey(key), root.toString()) }
    }

    /** Lee los datos guardados en el teléfono. */
    private fun loadLocal(key: String) {
        val raw = prefs.getString(localKey(key), null) ?: return
        runCatching { parseLocal(raw) }.getOrNull()?.let { data ->
            projects.addAll(data.projects)
            clients.addAll(data.clients)
            activity.addAll(data.activity)
        }
    }

    /** Convierte el JSON guardado en listas de proyectos, clientes y actividad. */
    private fun parseLocal(raw: String): LocalData {
        val root = JSONObject(raw)
        val p = mutableListOf<Project>()
        val c = mutableListOf<Client>()
        val a = mutableListOf<ActivityEntry>()
        root.optJSONArray("projects")?.forEachObject { p.add(it.toProject()) }
        root.optJSONArray("clients")?.forEachObject { c.add(it.toClient()) }
        root.optJSONArray("activity")?.forEachObject { a.add(it.toActivity()) }
        return LocalData(p, c, a)
    }

    /** Clave de los datos locales de cada cuenta. */
    private fun localKey(key: String) = "data_$key"

    /** Datos leídos del almacenamiento local. */
    private class LocalData(
        val projects: List<Project>,
        val clients: List<Client>,
        val activity: List<ActivityEntry>
    ) {
        val isEmpty get() = projects.isEmpty() && clients.isEmpty() && activity.isEmpty()
    }

    private companion object {
        const val TAG = "ProjectsViewModel"
        const val MAX_ACTIVITY = 20
        const val PROJECTS = "projects"
        const val CLIENTS = "clients"
        const val ACTIVITY = "activity"
    }
}

/** XP: +100 por proyecto, +50 por cliente y +200 por proyecto completado. */
fun experienceFor(projects: List<Project>, clients: List<Client>): Int =
    projects.size * 100 + clients.size * 50 + projects.count { it.isCompleted } * 200

/** Reemplaza el contenido sin recrear la lista (Compose solo recompone lo que cambió). */
private fun <T> SnapshotStateList<T>.replaceWith(items: List<T>) {
    if (this == items) return
    clear()
    addAll(items)
}
