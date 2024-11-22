package no.nav.emottak.configuration

import java.util.Properties

data class Config(
    val mail: Mail,
    val ebms: Ebms,
    val smtp: Smtp
)

data class Mail(val inboxLimit: Int)

data class Ebms(val providerUrl: String)

data class Smtp(
    val username: String,
    val password: String,
    val pop3Port: Int,
    val pop3Host: String,
    val imapPort: Int,
    val imapHost: String,
    val storeProtocol: String,
    val pop3FactoryPort: Int,
    val imapFactoryPort: Int,
    val pop3FactoryFallback: Boolean,
    val imapFactoryFallback: Boolean
)

private const val MAIL_POP_3_HOST = "mail.pop3.host"
private const val MAIL_POP_3_PORT = "mail.pop3.port"
private const val MAIL_IMAP_HOST = "mail.imap.host"
private const val MAIL_IMAP_PORT = "mail.imap.port"
private const val MAIL_POP_3_SOCKET_FACTORY_FALLBACK = "mail.pop3.socketFactory.fallback"
private const val MAIL_POP_3_SOCKET_FACTORY_PORT = "mail.pop3.socketFactory.port"
private const val MAIL_IMAP_SOCKET_FACTORY_FALLBACK = "mail.imap.socketFactory.fallback"
private const val MAIL_IMAP_SOCKET_FACTORY_PORT = "mail.imap.socketFactory.port"

fun Smtp.toProperties() = Properties()
    .apply {
        setProperty(MAIL_POP_3_SOCKET_FACTORY_FALLBACK, "$pop3FactoryFallback")
        setProperty(MAIL_POP_3_SOCKET_FACTORY_PORT, "$pop3FactoryPort")
        setProperty(MAIL_POP_3_PORT, "$pop3Port")
        setProperty(MAIL_POP_3_HOST, pop3Host)
        setProperty(MAIL_IMAP_SOCKET_FACTORY_FALLBACK, "$imapFactoryFallback")
        setProperty(MAIL_IMAP_SOCKET_FACTORY_PORT, "$imapFactoryPort")
        setProperty(MAIL_IMAP_PORT, "$imapPort")
        setProperty(MAIL_IMAP_HOST, imapHost)
    }
