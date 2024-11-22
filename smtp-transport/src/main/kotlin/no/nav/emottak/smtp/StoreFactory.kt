package no.nav.emottak.smtp

import jakarta.mail.Authenticator
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Store
import no.nav.emottak.configuration.Smtp
import no.nav.emottak.configuration.toProperties

object StoreFactory {
    fun createStore(smtp: Smtp): Store {
        val auth = object : Authenticator() {
            override fun getPasswordAuthentication() = PasswordAuthentication(smtp.username, smtp.password)
        }
        return Session.getInstance(smtp.toProperties(), auth)
            .getStore(smtp.storeProtocol)
            .also { it.connect() }
    }
}
