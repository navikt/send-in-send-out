package no.nav.emottak.util

import io.ktor.http.Headers
import net.logstash.logback.marker.LogstashMarker
import net.logstash.logback.marker.Markers
import no.nav.emottak.constants.LogIndex.ACTION
import no.nav.emottak.constants.LogIndex.CONVERSATION_ID
import no.nav.emottak.constants.LogIndex.CPA_ID
import no.nav.emottak.constants.LogIndex.FROM_PARTY
import no.nav.emottak.constants.LogIndex.FROM_ROLE
import no.nav.emottak.constants.LogIndex.MESSAGE_ID
import no.nav.emottak.constants.LogIndex.SERVICE
import no.nav.emottak.constants.LogIndex.TO_PARTY
import no.nav.emottak.constants.LogIndex.TO_ROLE
import no.nav.emottak.constants.LogIndex.X_MAILER
import no.nav.emottak.constants.SMTPHeaders
import no.nav.emottak.melding.model.Header
import no.nav.emottak.melding.model.SendInRequest
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader

fun Header.marker(): LogstashMarker = Markers.appendEntries(
    mapOf(
        MESSAGE_ID to this.messageId,
        CONVERSATION_ID to this.conversationId,
        CPA_ID to this.cpaId,
        SERVICE to this.service,
        ACTION to this.action,
        TO_ROLE to this.to.role,
        FROM_ROLE to this.from.role,
        TO_PARTY to "${this.to.partyId.firstOrNull()?.type ?: UKJENT_VERDI}:${this.to.partyId.firstOrNull()?.value ?: UKJENT_VERDI}",
        FROM_PARTY to "${this.from.partyId.firstOrNull()?.type ?: UKJENT_VERDI}:${this.from.partyId.firstOrNull()?.value ?: UKJENT_VERDI}"
    )
)

fun SendInRequest.marker(): LogstashMarker = Markers.appendEntries(
    mapOf(
        MESSAGE_ID to this.messageId,
        CONVERSATION_ID to this.conversationId
    )
)

fun EIFellesformat.marker(): LogstashMarker = Markers.appendEntries(
    mapOf(
        MESSAGE_ID to this.mottakenhetBlokk.mottaksId,
        CONVERSATION_ID to this.mottakenhetBlokk.ebXMLSamtaleId
    )
)

fun MessageHeader.marker(loggableHeaderPairs: Map<String, String> = mapOf()): LogstashMarker = Markers.appendEntries(
    mapOf(
        MESSAGE_ID to this.messageData.messageId,
        CONVERSATION_ID to this.conversationId,
        CPA_ID to (this.cpaId ?: UKJENT_VERDI),
        SERVICE to (this.service.value ?: UKJENT_VERDI),
        ACTION to this.action,
        TO_ROLE to (this.to.role ?: UKJENT_VERDI),
        FROM_ROLE to (this.from.role ?: UKJENT_VERDI),
        TO_PARTY to "${this.to.partyId.firstOrNull()?.type}:${this.to.partyId.firstOrNull()?.value}",
        FROM_PARTY to "${this.from.partyId.firstOrNull()?.type}:${this.from.partyId.firstOrNull()?.value}"
    ) + loggableHeaderPairs
)

fun Headers.marker(): LogstashMarker = Markers.appendEntries(
    this.retrieveLoggableHeaderPairs()
)

fun Headers.retrieveLoggableHeaderPairs(): Map<String, String> {
    return mapOf(
        X_MAILER to (this[SMTPHeaders.X_MAILER] ?: "-"),
        "messageId" to (this[SMTPHeaders.MESSAGE_ID] ?: this["X-Request-Id"] ?: "-")
    )
}

const val UKJENT_VERDI = "Ukjent" // Egentlig null
