package no.nav.emottak.publisher

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import arrow.fx.coroutines.resourceScope
import io.github.nomisRev.kafka.receiver.KafkaReceiver
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.map
import no.nav.emottak.KafkaSpec
import no.nav.emottak.config
import no.nav.emottak.configuration.Config
import no.nav.emottak.configuration.withKafka
import no.nav.emottak.kafkaPublisher
import java.util.UUID

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
            val modifiedConfig = config.withKafka { copy(topic = "single-part-topic") }
            resourceScope {
                turbineScope {
                    val publisher = MailPublisher(
                        modifiedConfig.kafka,
                        kafkaPublisher(modifiedConfig.kafka)
                    )

                    val referenceId = UUID.randomUUID()
                    val content = "content".toByteArray()

                    publisher.publishMessage(referenceId, content)

                    val receiver = KafkaReceiver(receiverSettings())
                    val consumer = receiver.receive(modifiedConfig.kafka.topic)
                        .map { Pair(it.key(), it.value()) }

                    consumer.test {
                        val (key, value) = awaitItem()
                        key shouldBe referenceId.toString()
                        value shouldBe content
                    }
                }
            }
        }
    }
)
