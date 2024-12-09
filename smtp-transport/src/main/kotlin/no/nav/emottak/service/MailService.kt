package no.nav.emottak.service

import jakarta.mail.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import no.nav.emottak.configuration.Config
import no.nav.emottak.log
import no.nav.emottak.smtp.EmailMsg
import no.nav.emottak.smtp.MailReader

class MailService(
    private val config: Config,
    private val store: Store
) {
    fun processMessages(): Flow<EmailMsg> {
        MailReader(config.mail, store, false).use { reader ->
            val countedMessages = reader.count()
            return when {
                countedMessages > 0 -> {
                    log.info("Starting to read $countedMessages messages from inbox")
                    reader.readMailBatches(countedMessages)
                        .asFlow()
                        .also { log.info("Finished reading all messages from inbox") }
                }

                else -> {
                    emptyFlow<EmailMsg>().also { log.info("No messages found in inbox") }
                }
            }
        }
    }
}
