package no.nav.emottak.ebms.kafka

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen
import org.apache.kafka.common.errors.RebalanceInProgressException
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

private val log = LoggerFactory.getLogger("no.nav.emottak.ebms.kafka.RebalanceRecovery")

internal fun <T> Flow<T>.retryOnRebalance(topic: String): Flow<T> =
    retryWhen { cause, attempt ->
        if (cause is RebalanceInProgressException) {
            log.warn(
                "Kafka receiver on topic {} interrupted by a rebalance; restarting in 1 second (retry {}). Uncommitted messages may be redelivered.",
                topic,
                attempt + 1,
                cause
            )
            delay(1.seconds)
            true
        } else {
            false
        }
    }
