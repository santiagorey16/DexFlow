package com.santiagorey.devflow.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Guarda textos sensibles (claves de IA, token de GitHub) cifrados con AES-GCM.
 *
 * La llave de cifrado vive en el Android Keystore: no se puede extraer del teléfono,
 * así que aunque alguien copie el archivo de preferencias no puede leer las claves.
 * El archivo además está excluido de las copias de seguridad (ver res/xml).
 */
class SecureStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Lee y descifra un valor. Si no se puede descifrar (p. ej. se perdió la llave), devuelve null. */
    fun getString(key: String): String? {
        val stored = prefs.getString(key, null) ?: return null
        return runCatching { decrypt(stored) }.getOrNull()
    }

    /** Cifra y guarda un valor. Un valor vacío o null borra la clave. */
    fun putString(key: String, value: String?) {
        if (value.isNullOrEmpty()) {
            prefs.edit { remove(key) }
        } else {
            prefs.edit { putString(key, encrypt(value)) }
        }
    }

    /** Borra un valor cifrado. */
    fun remove(key: String) = prefs.edit { remove(key) }

    // Valores no sensibles (proveedor y modelo elegidos) se guardan sin cifrar.
    fun getPlain(key: String): String? = prefs.getString(PLAIN_PREFIX + key, null)
    /** Guarda un valor sin cifrar (null lo borra). */
    fun putPlain(key: String, value: String?) = prefs.edit {
        if (value == null) remove(PLAIN_PREFIX + key) else putString(PLAIN_PREFIX + key, value)
    }

    /** Cifra con AES-GCM. Cada vez se genera un IV aleatorio, que se guarda junto al dato: "iv:datos". */
    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val data = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        return "$iv:$data"
    }

    /** Separa el IV de los datos y descifra con la llave del Keystore. */
    private fun decrypt(stored: String): String {
        val (ivPart, dataPart) = stored.split(":", limit = 2)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(128, Base64.decode(ivPart, Base64.NO_WRAP))
        )
        return String(cipher.doFinal(Base64.decode(dataPart, Base64.NO_WRAP)), Charsets.UTF_8)
    }

    /** Llave AES-256 del Android Keystore: se crea la primera vez y nunca sale del teléfono. */
    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFS_NAME = "devflow_secure"
        const val PLAIN_PREFIX = "plain_"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "devflow_secrets_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
