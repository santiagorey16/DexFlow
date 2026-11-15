package com.santiagorey.devflow.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

private const val SETTINGS_PREFS = "devflow_settings"

/**
 * Un solo estado por preferencia para toda la app: si Ajustes cambia un valor,
 * las demás pantallas (Dashboard, campana, detalle de proyecto) lo ven al instante.
 */
private val sharedStates = mutableMapOf<String, MutableState<*>>()

/**
 * Estado de Compose que se guarda en el teléfono: el valor se conserva al cerrar la app.
 * Se usa igual que `remember { mutableStateOf(...) }`:
 *
 * ```
 * var coach by rememberBooleanPreference("coach_live", true)
 * ```
 */
@Composable
fun rememberBooleanPreference(key: String, default: Boolean): MutableState<Boolean> {
    val prefs = settingsPrefs()
    return remember(key) {
        sharedState(key) {
            PersistedState(mutableStateOf(prefs.getBoolean(key, default))) { value ->
                prefs.edit { putBoolean(key, value) }
            }
        }
    }
}

@Composable
fun rememberStringPreference(key: String, default: String): MutableState<String> {
    val prefs = settingsPrefs()
    return remember(key) {
        sharedState(key) {
            PersistedState(mutableStateOf(prefs.getString(key, default) ?: default)) { value ->
                prefs.edit { putString(key, value) }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T> sharedState(key: String, create: () -> MutableState<T>): MutableState<T> =
    synchronized(sharedStates) { sharedStates.getOrPut(key) { create() } as MutableState<T> }

@Composable
private fun settingsPrefs(): SharedPreferences {
    val context = LocalContext.current.applicationContext
    return remember(context) { context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE) }
}

private class PersistedState<T>(
    private val state: MutableState<T>,
    private val persist: (T) -> Unit
) : MutableState<T> {
    override var value: T
        get() = state.value
        set(newValue) {
            if (newValue == state.value) return
            state.value = newValue
            persist(newValue)
        }

    override fun component1(): T = value
    override fun component2(): (T) -> Unit = { value = it }
}
