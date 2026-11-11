package no.nav.emottak.kafka

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.emottak.ebms.kafka.retryOnRebalance
import org.apache.kafka.common.errors.AuthenticationException
import org.apache.kafka.common.errors.RebalanceInProgressException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RebalanceRecoveryTest {
    @Test
    fun `rebalance failures restart consumption and allow further messages`() = runBlocking {
        var subscriptions = 0
        val received = flow {
            subscriptions++
            emit(subscriptions)
            if (subscriptions <= 2) throw RebalanceInProgressException("Rebalancing")
        }.retryOnRebalance("test-topic").toList()

        assertEquals(3, subscriptions)
        assertEquals(listOf(1, 2, 3), received)
    }

    @Test
    fun `normal completion does not restart consumption`() = runBlocking {
        assertEquals(listOf(1, 2), flowOf(1, 2).retryOnRebalance("test-topic").toList())
    }

    @Test
    fun `unrelated Kafka failures are propagated without retry`() = runBlocking {
        var subscriptions = 0
        val failure = AuthenticationException("Invalid credentials")
        val result = runCatching {
            flow<Int> {
                subscriptions++
                throw failure
            }.retryOnRebalance("test-topic").collect {}
        }

        assertSame(failure, result.exceptionOrNull())
        assertEquals(1, subscriptions)
    }

    @Test
    fun `upstream cancellation is propagated without retry`() = runBlocking {
        var subscriptions = 0
        val cancellation = CancellationException("Shutting down")
        val result = runCatching {
            flow<Int> {
                subscriptions++
                throw cancellation
            }.retryOnRebalance("test-topic").collect {}
        }

        assertSame(cancellation, result.exceptionOrNull())
        assertEquals(1, subscriptions)
    }

    @Test
    fun `cancellation during retry delay stops the receiver`() = runBlocking {
        var subscriptions = 0
        val receiver = launch(start = CoroutineStart.UNDISPATCHED) {
            flow<Int> {
                subscriptions++
                throw RebalanceInProgressException("Rebalancing")
            }.retryOnRebalance("test-topic").collect {}
        }

        assertEquals(1, subscriptions)
        receiver.cancelAndJoin()

        assertTrue(receiver.isCancelled)
        assertEquals(1, subscriptions)
    }

    @Test
    fun `downstream processing failures are not retried`() = runBlocking {
        var subscriptions = 0
        val failure = IllegalStateException("Processing failed")
        val result = runCatching {
            flow {
                subscriptions++
                emit(1)
            }.retryOnRebalance("test-topic").collect { throw failure }
        }

        assertSame(failure, result.exceptionOrNull())
        assertEquals(1, subscriptions)
    }
}
