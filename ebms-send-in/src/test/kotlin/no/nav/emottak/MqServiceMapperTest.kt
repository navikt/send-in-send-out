package no.nav.emottak

import io.mockk.mockk
import no.nav.emottak.ebms.MqService
import no.nav.emottak.ebms.MqServiceMapper
import no.nav.emottak.ebms.utils.AsyncRoutingAction
import no.nav.emottak.ebms.utils.AsyncRoutingAction.Companion.toAsyncRoutingAction
import no.nav.emottak.ebms.utils.SupportedAsyncServiceType
import no.nav.emottak.ebms.utils.SupportedAsyncServiceType.Companion.toSupportedAsyncService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MqServiceMapperTest {

    @Test
    fun addAndGetWorksOk() {
        val service1 = mockk<MqService>()
        val service2 = mockk<MqService>()
        val service3 = mockk<MqService>()
        val service4 = mockk<MqService>()

        val mapper = MqServiceMapper()
        mapper.addMqService(SupportedAsyncServiceType.Trekkopplysning, service1)
        mapper.addMqService(SupportedAsyncServiceType.Sykmelding, service2)
        mapper.addMqService(AsyncRoutingAction.DialogmoteInnkallingKvittering, service3)
        mapper.addMqService(AsyncRoutingAction.DialogmoteInnkallingMoteRespons, service4)

        assertEquals(service1, mapper.get("Trekkopplysning".toSupportedAsyncService()))
        assertEquals(service2, mapper.get("Sykmelding".toSupportedAsyncService()))
        assertEquals(service3, mapper.get("Kvittering".toAsyncRoutingAction("DialogmoteInnkalling".toSupportedAsyncService())))
        assertEquals(service4, mapper.get("MoteRespons".toAsyncRoutingAction("DialogmoteInnkalling".toSupportedAsyncService())))
        assertNull(mapper.get("Unknown".toSupportedAsyncService()))
        assertNull(mapper.get("Unknown".toAsyncRoutingAction("DialogmoteInnkalling".toSupportedAsyncService())))
        assertNull(mapper.get("Kvittering".toAsyncRoutingAction("Trekkopplysning".toSupportedAsyncService())))
        val service = "DialogmoteInnkalling".toSupportedAsyncService()
        val exception = assertThrows<RuntimeException> {
            mapper.get(service)
        }
        assertEquals("Multiple MQ services found for $service, action must be specified", exception.message)
    }

    @Test
    fun listWorksOk() {
        val service1 = mockk<MqService>()
        val service2 = mockk<MqService>()
        val service3 = mockk<MqService>()
        val service4 = mockk<MqService>()

        val mapper = MqServiceMapper()
        mapper.addMqService(SupportedAsyncServiceType.Trekkopplysning, service1)
        mapper.addMqService(SupportedAsyncServiceType.Sykmelding, service2)
        mapper.addMqService(AsyncRoutingAction.DialogmoteInnkallingKvittering, service3)
        mapper.addMqService(AsyncRoutingAction.DialogmoteInnkallingMoteRespons, service4)

        val list = mapper.list()
        assertEquals(4, list.size)
        assertTrue(list.contains(service1))
        assertTrue(list.contains(service2))
        assertTrue(list.contains(service3))
        assertTrue(list.contains(service4))
    }
}
