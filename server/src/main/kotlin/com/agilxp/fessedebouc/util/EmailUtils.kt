package com.agilxp.fessedebouc.util

import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.*

class EmailUtils {
    companion object {
        private val properties = Properties()
        private val session: Session
        init {
            properties.setProperty("mail.transport.protocol", "smtp")
            properties.setProperty("mail.host", "smtp.gmail.com")
            properties["mail.smtp.host"] = "smtp.gmail.com"
            properties["mail.smtp.port"] = "465"
            properties["mail.smtp.socketFactory.fallback"] = "false"
            properties.setProperty("mail.smtp.quitwait", "false")
            properties["mail.smtp.socketFactory.port"] = "465"
            properties["mail.smtp.starttls.enable"] = "true"
            properties["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
            properties["mail.smtp.ssl.enable"] = "true"
            properties["mail.smtp.auth"] = "true"
            session = Session.getInstance(properties, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(
                        "geoffrey@agilxp.com",
                        "milyiwnuzmwkmvzf"
                    )
                }
            })
        }
        @Throws(MessagingException::class)
        fun sendEmail(recipient: String, subject: String, text: String) {
            val message = MimeMessage(session)
            message.setFrom(InternetAddress("no-reply@agilxp.com"))
            message.setRecipient(Message.RecipientType.TO, InternetAddress(recipient))
            message.subject = subject
            message.setText(text)
            Transport.send(message)
        }
    }
}
