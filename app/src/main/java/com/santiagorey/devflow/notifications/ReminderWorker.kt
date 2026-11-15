package com.santiagorey.devflow.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.santiagorey.devflow.MainActivity
import com.santiagorey.devflow.R
import com.santiagorey.devflow.github.GitHubApi
import com.santiagorey.devflow.github.GitHubTokenStore
import com.santiagorey.devflow.projects.Project
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Revisa cada pocas horas (aunque la app esté cerrada) las entregas y el CI de los repos
 * vinculados, y avisa con una notificación. Cada aviso se manda como mucho una vez al día.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val cache = AlertCache.read(applicationContext) ?: return Result.success()
        val ciRuns = if (cache.qualityAlerts && cache.uid != null) {
            val token = GitHubTokenStore(applicationContext).get(cache.uid)
            if (token == null) emptyList() else {
                val api = GitHubApi(token)
                cache.projects.mapNotNull { it.repoFullName }.distinct()
                    .mapNotNull { repo -> runCatching { api.latestWorkflowRun(repo) }.getOrNull() }
            }
        } else {
            emptyList()
        }
        val alerts = buildAlerts(cache.projects, ciRuns, audit = null, qualityAlertsEnabled = cache.qualityAlerts)
        DevFlowNotifications.notifyNew(applicationContext, alerts)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "devflow_reminders"

        /** Programa la revisión periódica (si ya existe, se mantiene). */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

/** Copia mínima de los proyectos para que el trabajo en segundo plano pueda calcular avisos. */
object AlertCache {
    private const val PREFS = "devflow_alert_cache"
    private const val KEY = "cache"

    class Snapshot(val uid: String?, val qualityAlerts: Boolean, val projects: List<Project>)

    fun save(context: Context, uid: String?, qualityAlerts: Boolean, projects: List<Project>) {
        val array = JSONArray()
        projects.filter { !it.isCompleted }.forEach { p ->
            array.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("progress", p.progress)
                    .put("delivery", p.deliveryDateMillis ?: JSONObject.NULL)
                    .put("repo", p.repoFullName ?: JSONObject.NULL)
            )
        }
        val root = JSONObject().put("uid", uid ?: JSONObject.NULL).put("quality", qualityAlerts).put("projects", array)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY, root.toString()) }
    }

    fun clear(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { clear() }

    fun read(context: Context): Snapshot? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            val array = root.getJSONArray("projects")
            val projects = (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Project(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    progress = o.optInt("progress"),
                    deliveryDateMillis = if (o.isNull("delivery")) null else o.optLong("delivery"),
                    repoFullName = if (o.isNull("repo")) null else o.optString("repo")
                )
            }
            Snapshot(if (root.isNull("uid")) null else root.optString("uid"), root.optBoolean("quality"), projects)
        }.onFailure { Log.w("DevFlowAlerts", "Caché de avisos inválido", it) }.getOrNull()
    }
}

/** Canal y envío de notificaciones de DevFlow. */
object DevFlowNotifications {
    private const val CHANNEL_ID = "devflow_alerts"
    private const val PREFS = "devflow_notified"
    const val EXTRA_OPEN = "open"
    const val OPEN_NOTIFICATIONS = "notifications"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Entregas y alertas", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Entregas vencidas o próximas y fallos de CI en tus repositorios"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Notifica solo los avisos que no se mandaron hoy. */
    fun notifyNew(context: Context, alerts: List<DevFlowAlert>) {
        if (!canNotify(context) || alerts.isEmpty()) return
        createChannel(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        val manager = NotificationManagerCompat.from(context)

        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN, OPEN_NOTIFICATIONS)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alerts.filter { prefs.getString(it.id, null) != today }.forEach { alert ->
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(alert.title)
                .setContentText(alert.detail)
                .setStyle(NotificationCompat.BigTextStyle().bigText(alert.detail))
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .setPriority(
                    if (alert.severity == DevFlowAlert.Severity.HIGH) NotificationCompat.PRIORITY_HIGH
                    else NotificationCompat.PRIORITY_DEFAULT
                )
                .build()
            try {
                manager.notify(alert.id.hashCode(), notification)
                prefs.edit { putString(alert.id, today) }
            } catch (e: SecurityException) {
                Log.w("DevFlowAlerts", "Sin permiso de notificaciones", e)
            }
        }
    }
}
