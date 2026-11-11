package com.santiagorey.devflow.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Acciones de contacto con un cliente. Usan intents del sistema, por lo que
 * no requieren permisos: el usuario confirma la llamada / mensaje en la app
 * correspondiente.
 */
object ContactActions {

    fun call(context: Context, phone: String) {
        if (phone.isBlank()) return noPhone(context)
        launch(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone.trim())}")), "No hay app de teléfono disponible.")
    }

    fun sms(context: Context, phone: String) {
        if (phone.isBlank()) return noPhone(context)
        launch(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phone.trim())}")), "No hay app de mensajes disponible.")
    }

    /** Abre un chat de WhatsApp. El número debe incluir el código de país (ej. +57...). */
    fun whatsApp(context: Context, phone: String) {
        val digits = phone.filter { it.isDigit() }
        if (digits.isBlank()) return noPhone(context)
        launch(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits")), "No se pudo abrir WhatsApp.")
    }

    fun email(context: Context, email: String) {
        if (email.isBlank()) return
        launch(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${email.trim()}")), "No hay app de correo disponible.")
    }

    private fun launch(context: Context, intent: Intent, errorMessage: String) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun noPhone(context: Context) {
        Toast.makeText(context, "Este cliente no tiene número registrado.", Toast.LENGTH_SHORT).show()
    }
}
