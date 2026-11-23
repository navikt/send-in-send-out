package no.nav.emottak.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.kith.xmlstds.msghead._2006_05_24.CV
import no.kith.xmlstds.msghead._2006_05_24.HealthcareProfessional
import no.kith.xmlstds.msghead._2006_05_24.Ident
import no.nav.emottak.fellesformat.unmarshal
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PayloadDataUtilTest {
    @Test
    fun `extracts receiver identifiers rather than NAV sender identifiers from msghead response`() {
        val eventData = exampleMsgHeadResponse().eventData()

        assertEquals("INNRAPPORTERING_TREKK_RETUR", eventData["msgType"])
        assertEquals("f2f0f2f6-f0f4-f2f1-f1f3-f2f5f1f6f9f4", eventData["msgId"])
        assertEquals("nav:qass:36181", eventData["partnerReferanse"])
        assertEquals("3fec5045-7c58-488f-8e47-7ad7468bb372", eventData["ediLoggId"])
        assertEquals("12345678", eventData["herId"])
        assertEquals("", eventData["orgNr"])
        assertEquals("", eventData["behandlerHerId"])
        assertEquals("", eventData["behandlerHpr"])
    }

    @Test
    fun `extracts all receiver identifiers from msghead`() {
        val eventData = msgHeadResponseWithIdentifiers().eventData()

        assertEquals("987654321", eventData["orgNr"])
        assertEquals("12345678", eventData["herId"])
        assertEquals("123456", eventData["behandlerHerId"])
        assertEquals("123456789", eventData["behandlerHpr"])
    }

    @Test
    fun `supports orgnummer as msghead organisation identifier type`() {
        val response = msgHeadResponseWithIdentifiers()
        response.msgHead.msgInfo.receiver.organisation.ident.single { it.typeId.v == "ENH" }.typeId.v = "orgnummer"

        assertEquals("987654321", response.eventData()["orgNr"])
    }

    @Test
    fun `finds msghead identifiers after unrelated entries`() {
        val response = msgHeadResponseWithIdentifiers()
        response.msgHead.msgInfo.receiver.organisation.apply {
            ident.add(0, ident("local-org", "LIN"))
            healthcareProfessional.ident.add(0, ident("unrelated-person", "FNR"))
        }

        val eventData = response.eventData()

        assertEquals("987654321", eventData["orgNr"])
        assertEquals("12345678", eventData["herId"])
        assertEquals("123456", eventData["behandlerHerId"])
        assertEquals("123456789", eventData["behandlerHpr"])
    }

    @Test
    fun `ignores unrelated msghead identifier types`() {
        val response = msgHeadResponseWithIdentifiers()
        response.msgHead.msgInfo.receiver.organisation.apply {
            ident.forEach { it.typeId.v = "LIN" }
            healthcareProfessional.ident.forEach { it.typeId.v = "FNR" }
        }

        val eventData = response.eventData()

        assertEquals("", eventData["orgNr"])
        assertEquals("", eventData["herId"])
        assertEquals("", eventData["behandlerHerId"])
        assertEquals("", eventData["behandlerHpr"])
    }

    @Test
    fun `ignores msghead identifiers without type information`() {
        val response = msgHeadResponseWithIdentifiers()
        response.msgHead.msgInfo.receiver.organisation.apply {
            ident.first().typeId = null
            ident.last().typeId.v = null
            healthcareProfessional.ident.first().typeId = null
            healthcareProfessional.ident.last().typeId.v = null
        }

        val eventData = response.eventData()

        assertEquals("", eventData["orgNr"])
        assertEquals("", eventData["herId"])
        assertEquals("", eventData["behandlerHerId"])
        assertEquals("", eventData["behandlerHpr"])
    }

    @Test
    fun `returns empty identifiers for empty msghead identifier lists`() {
        val response = msgHeadResponseWithIdentifiers()
        response.msgHead.msgInfo.receiver.organisation.apply {
            ident.clear()
            healthcareProfessional.ident.clear()
        }

        val eventData = response.eventData()

        assertEquals("", eventData["orgNr"])
        assertEquals("", eventData["herId"])
        assertEquals("", eventData["behandlerHerId"])
        assertEquals("", eventData["behandlerHpr"])
    }

    @Test
    fun `returns empty identifiers for missing msghead receiver details`() {
        val responses = listOf(
            msgHeadResponseWithIdentifiers().apply { msgHead.msgInfo.receiver.organisation.healthcareProfessional = null },
            msgHeadResponseWithIdentifiers().apply { msgHead.msgInfo.receiver.organisation = null },
            msgHeadResponseWithIdentifiers().apply { msgHead.msgInfo.receiver = null },
            msgHeadResponseWithIdentifiers().apply { msgHead.msgInfo = null }
        )

        responses.forEach { response ->
            val eventData = response.eventData()
            assertEquals("", eventData["behandlerHerId"])
            assertEquals("", eventData["behandlerHpr"])
        }
        assertEquals("987654321", responses.first().eventData()["orgNr"])
        assertEquals("12345678", responses.first().eventData()["herId"])
        responses.drop(1).forEach { response ->
            assertEquals("", response.eventData()["orgNr"])
            assertEquals("", response.eventData()["herId"])
        }
        responses.dropLast(1).forEach { response ->
            assertEquals("INNRAPPORTERING_TREKK_RETUR", response.eventData()["msgType"])
        }
        assertEquals("MsgHead", responses.last().eventData()["msgType"])
        assertEquals("", responses.last().eventData()["msgId"])
    }

    @Test
    fun `returns empty msghead message ID when absent`() {
        val response = msgHeadResponseWithIdentifiers()
        response.msgHead.msgInfo.msgId = null

        val eventData = response.eventData()

        assertEquals("INNRAPPORTERING_TREKK_RETUR", eventData["msgType"])
        assertEquals("", eventData["msgId"])
        assertEquals("12345678", eventData["herId"])
    }

    @Test
    fun `falls back to MsgHead when message type is absent`() {
        val responses = listOf(
            exampleMsgHeadResponse().apply { msgHead.msgInfo.type = null },
            exampleMsgHeadResponse().apply { msgHead.msgInfo.type.v = null }
        )

        responses.forEach { response ->
            val eventData = response.eventData()
            assertEquals("MsgHead", eventData["msgType"])
            assertEquals("f2f0f2f6-f0f4-f2f1-f1f3-f2f5f1f6f9f4", eventData["msgId"])
            assertEquals("12345678", eventData["herId"])
        }
    }

    @Test
    fun `returns empty envelope references when absent for msghead`() {
        val response = exampleMsgHeadResponse()
        response.mottakenhetBlokk.apply {
            partnerReferanse = null
            ediLoggId = null
        }

        val eventData = response.eventData()

        assertEquals("", eventData["partnerReferanse"])
        assertEquals("", eventData["ediLoggId"])
        assertEquals("f2f0f2f6-f0f4-f2f1-f1f3-f2f5f1f6f9f4", eventData["msgId"])
    }

    @Test
    fun `extracts receiver identifiers from example apprec`() {
        val eventData = exampleResponse().eventData()

        assertEquals("987654321", eventData["orgNr"])
        assertEquals("654321", eventData["herId"])
        assertEquals("123456", eventData["behandlerHerId"])
        assertEquals("123456789", eventData["behandlerHpr"])
    }

    @Test
    fun `extracts organisation number from primary identifier and HER from additional identifiers`() {
        val response = exampleResponse()
        val inst = response.appRec.receiver.hcp.inst
        inst.id = "987654321"
        inst.typeId.v = "ENH"
        inst.additionalId.single().apply {
            id = "654321"
            type.v = "HER"
        }

        val eventData = response.eventData()

        assertEquals("987654321", eventData["orgNr"])
        assertEquals("654321", eventData["herId"])
    }

    @Test
    fun `extracts HPR from primary person identifier`() {
        val response = exampleResponse()
        response.appRec.receiver.hcp.inst.hcPerson.single().apply {
            id = "123456789"
            typeId.v = "HPR"
            additionalId.clear()
        }

        assertEquals("123456789", response.eventData()["behandlerHpr"])
    }

    @Test
    fun `extracts HER from additional person identifiers`() {
        val response = exampleResponse()
        response.appRec.receiver.hcp.inst.hcPerson.single().apply {
            id = "123456789"
            typeId.v = "HPR"
            additionalId.single().apply {
                id = "123456"
                type.v = "HER"
            }
        }

        val eventData = response.eventData()

        assertEquals("123456", eventData["behandlerHerId"])
        assertEquals("123456789", eventData["behandlerHpr"])
    }

    @Test
    fun `prefers primary person HER over additional HER`() {
        val response = exampleResponse()
        response.appRec.receiver.hcp.inst.hcPerson.single().additionalId.single().apply {
            id = "additional-her"
            type.v = "HER"
        }

        assertEquals("123456", response.eventData()["behandlerHerId"])
    }

    @Test
    fun `prefers primary identifiers over additional identifiers of the same type`() {
        val response = exampleResponse()
        response.appRec.receiver.hcp.inst.apply {
            id = "primary-org"
            typeId.v = "ENH"
            hcPerson.single().apply {
                id = "primary-hpr"
                typeId.v = "HPR"
            }
        }

        val eventData = response.eventData()

        assertEquals("primary-org", eventData["orgNr"])
        assertEquals("primary-hpr", eventData["behandlerHpr"])
    }

    @Test
    fun `prefers primary HER over additional HER`() {
        val response = exampleResponse()
        response.appRec.receiver.hcp.inst.additionalId.single().apply {
            id = "additional-her"
            type.v = "HER"
        }

        assertEquals("654321", response.eventData()["herId"])
    }

    @Test
    fun `finds matching identifiers after unrelated entries`() {
        val response = exampleResponse()
        response.appRec.receiver.hcp.inst.apply {
            additionalId.add(0, exampleResponse().appRec.receiver.hcp.inst.additionalId.single().apply { type.v = "LIN" })
            hcPerson.add(
                0,
                exampleResponse().appRec.receiver.hcp.inst.hcPerson.single().apply {
                    typeId.v = "FNR"
                    additionalId.clear()
                }
            )
        }

        val eventData = response.eventData()

        assertEquals("987654321", eventData["orgNr"])
        assertEquals("123456", eventData["behandlerHerId"])
        assertEquals("123456789", eventData["behandlerHpr"])
    }

    @Test
    fun `supports orgnummer as organisation identifier type`() {
        val response = exampleResponse()
        val inst = response.appRec.receiver.hcp.inst
        inst.additionalId.single().type.v = "orgnummer"

        assertEquals("987654321", response.eventData()["orgNr"])

        inst.id = "primary-org"
        inst.typeId.v = "orgnummer"

        assertEquals("primary-org", response.eventData()["orgNr"])
    }

    @Test
    fun `ignores unrelated identifier types`() {
        val response = exampleResponse()
        response.appRec.receiver.hcp.inst.apply {
            typeId.v = "LIN"
            additionalId.single().type.v = "LIN"
            hcPerson.single().apply {
                typeId.v = "FNR"
                additionalId.single().type.v = "FNR"
            }
        }

        val eventData = response.eventData()

        assertEquals("", eventData["orgNr"])
        assertEquals("", eventData["herId"])
        assertEquals("", eventData["behandlerHerId"])
        assertEquals("", eventData["behandlerHpr"])
    }

    @Test
    fun `returns empty identifiers for missing receiver details`() {
        val responses = listOf(
            exampleResponse().apply { appRec.receiver.hcp.inst.hcPerson.clear() },
            exampleResponse().apply { appRec.receiver.hcp.inst = null },
            exampleResponse().apply { appRec.receiver.hcp = null },
            exampleResponse().apply { appRec.receiver = null },
            exampleResponse().apply { appRec = null }
        )

        responses.forEach { response ->
            assertEquals("", response.eventData()["behandlerHerId"])
            assertEquals("", response.eventData()["behandlerHpr"])
        }
        responses.drop(1).forEach { response ->
            assertEquals("", response.eventData()["orgNr"])
            assertEquals("", response.eventData()["herId"])
        }
    }

    private fun exampleMsgHeadResponse(): EIFellesformat {
        val xml = javaClass.classLoader.getResourceAsStream("trekkopplysning/trekkopplysning_kvittering.xml")!!
            .use { it.readAllBytes().decodeToString() }
        return unmarshal(xml, EIFellesformat::class.java)
    }

    private fun msgHeadResponseWithIdentifiers(): EIFellesformat = exampleMsgHeadResponse().apply {
        msgHead.msgInfo.receiver.organisation.apply {
            ident.add(ident("987654321", "ENH"))
            healthcareProfessional = HealthcareProfessional().apply {
                ident.add(ident("123456", "HER"))
                ident.add(ident("123456789", "HPR"))
            }
        }
        msgHead.msgInfo.sender.organisation.apply {
            ident.add(ident("sender-org", "ENH"))
            healthcareProfessional = HealthcareProfessional().apply {
                ident.add(ident("sender-her", "HER"))
                ident.add(ident("sender-hpr", "HPR"))
            }
        }
    }

    private fun ident(value: String, type: String): Ident = Ident().apply {
        id = value
        typeId = CV().apply { v = type }
    }

    private fun exampleResponse(): EIFellesformat {
        val xml = javaClass.classLoader.getResourceAsStream("legemelding_response3.xml")!!
            .use { it.readAllBytes().decodeToString() }
        return unmarshal(xml, EIFellesformat::class.java)
    }

    private fun EIFellesformat.eventData(): Map<String, String> =
        Json.parseToJsonElement(toEventDataJson()).jsonObject.mapValues { it.value.jsonPrimitive.content }
}
