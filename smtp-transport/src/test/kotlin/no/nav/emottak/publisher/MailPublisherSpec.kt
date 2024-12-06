package no.nav.emottak.publisher

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import io.github.nomisRev.kafka.receiver.KafkaReceiver
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import no.nav.emottak.KafkaSpec
import no.nav.emottak.config
import no.nav.emottak.configuration.Config
import no.nav.emottak.configuration.withKafka
import no.nav.emottak.kafkaPublisherSettings
import no.nav.emottak.smtp.EmailMsg
import no.nav.emottak.smtp.Part

class MailPublisherSpec : KafkaSpec(
    {
        lateinit var config: Config

        beforeSpec {
            config = config()
                .withKafka {
                    copy(bootstrapServers = container.bootstrapServers)
                }
        }

        "All published messages are received - one message single part" {
            val singlePartConfig = config.withKafka { copy(topic = "single-part-topic") }

            turbineScope {
                val publisher = MailPublisher(
                    singlePartConfig,
                    kafkaPublisherSettings(singlePartConfig.kafka)
                )

                publisher.publishMessages(getMailMessagesSinglePart())

                val receiver = KafkaReceiver(receiverSettings())
                val consumer = receiver.receive(singlePartConfig.kafka.topic)
                    .map { Pair(it.key(), it.value()) }

                consumer.test {
                    val (key, value) = awaitItem()
                    key shouldBe "custom-id:singlepart"
                    value shouldBe "part-value".toByteArray()
                }
            }
        }

        "All published messages are received - one message multi part" {
            val multiPartConfig = config.withKafka { copy(topic = "multi-part-topic") }

            turbineScope {
                val publisher = MailPublisher(
                    multiPartConfig,
                    kafkaPublisherSettings(multiPartConfig.kafka)
                )

                publisher.publishMessages(getMailMessagesMultiPart())

                val receiver = KafkaReceiver(receiverSettings())
                val consumer = receiver.receive(multiPartConfig.kafka.topic)
                    .map { Pair(it.key(), it.value()) }

                consumer.test {
                    val (key, value) = awaitItem()
                    key shouldBe "custom-id:multipart"
                    value shouldBe "part-value-again".toByteArray()
                }
            }
        }
    }
)

fun getMailMessagesSinglePart() = flowOf(
    EmailMsg(
        headers = mapOf("Message-Id" to "custom-id"),
        parts = listOf(
            Part(
                headers = mapOf("Content-Type" to "text/plain; charset=UTF-8"),
                bytes = "part-value".toByteArray()
            )
        )
    )
)

fun getMailMessagesMultiPart() = flowOf(
    EmailMsg(
        headers = mapOf("Message-Id" to "custom-id"),
        parts = listOf(
            Part(
                headers = mapOf("Content-Type" to "text/plain; charset=UTF-8"),
                bytes = "part-value-again".toByteArray()
            ),
            Part(
                headers = mapOf("Content-Type" to "application/pkcs7-mime"),
                bytes = "encrypted-value".toByteArray()
            )
        )
    )
)
