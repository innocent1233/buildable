package com.libraryx.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.libraryx.data.model.SaasStudent
import java.net.URLEncoder

/**
 * Replaces the URL-building half of src/lib/saas/whatsappService.ts. The web app opened
 * `https://wa.me/<phone>?text=<message>` in a new tab; here we launch the same wa.me deep
 * link via an `ACTION_VIEW` Intent so it opens directly in the WhatsApp app if installed.
 */
object WhatsAppLauncher {

    fun buildReminderMessage(student: SaasStudent, labName: String, amountDue: Double): String {
        return "Hi ${student.name}, this is a reminder from $labName that your membership fee of " +
            "Rs. ${amountDue.toInt()} is due. Please make the payment at your earliest convenience. Thank you!"
    }

    fun intentFor(phone: String, message: String): Intent {
        val normalized = phone.filter { it.isDigit() || it == '+' }
        val encoded = URLEncoder.encode(message, "UTF-8")
        val uri = Uri.parse("https://wa.me/$normalized?text=$encoded")
        return Intent(Intent.ACTION_VIEW, uri)
    }

    fun launch(context: Context, phone: String, message: String) {
        context.startActivity(intentFor(phone, message).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
