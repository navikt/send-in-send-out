package no.nav.emottak.util

import no.kith.xmlstds.apprec._2004_11_21.AppRec
import no.kith.xmlstds.apprec._2004_11_21.AppRec.Receiver
import no.kith.xmlstds.apprec._2004_11_21.CS
import no.kith.xmlstds.apprec._2004_11_21.CV
import no.kith.xmlstds.apprec._2004_11_21.HCP
import no.kith.xmlstds.apprec._2004_11_21.HCPerson
import no.kith.xmlstds.apprec._2004_11_21.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.HealthcareProfessional
import no.kith.xmlstds.msghead._2006_05_24.Ident
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.kith.xmlstds.msghead._2006_05_24.MsgInfo
import org.apache.commons.lang3.StringUtils
import java.time.Instant
import javax.xml.bind.JAXBContext
import kotlin.uuid.Uuid

private val objectFactory = ObjectFactory()
private const val OID_8221 = "2.16.578.1.12.4.1.1.8221"
private const val VERSION_1_0 = "1.0 2004-11-21"

private const val APPREC = "APPREC"
private const val APPLIKASJONSKVITTERING = "Applikasjonskvittering"

private const val NAV = "NAV"
private const val NAV_HERID = "79768"
private const val NAV_ORGNO = "889640782"

private const val DEPT_NAME = "Elektronisk mottak"

private const val NOK_VALUE = "2"
private const val NOK_DESCRIPTION = "Avvist"

val apprecMarshaller = XmlMarshaller(
    JAXBContext.newInstance(
        ObjectFactory::class.java
    )
)

fun createNegativeApprec(
    msgHead: MsgHead,
    exception: Exception
): AppRec = createBaseApprec(msgHead).apply {
    status = createCS(NOK_VALUE, NOK_DESCRIPTION)
    error.add(createApprecError(exception.soapFaultString() ?: exception.localizedMessage ?: "Ukjent feil"))
}

private fun createBaseApprec(msgHead: MsgHead): AppRec = objectFactory.createAppRec().apply {
    genDate = Instant.now().toXmlGregorianCalendar()
    id = Uuid.random().toString()
    miGversion = VERSION_1_0
    msgType = createCS(APPREC, APPLIKASJONSKVITTERING)
    originalMsgId = createOriginalMessageId(msgHead.msgInfo)
    receiver = createReceiver(msgHead.msgInfo)
    sender = createSender()
}

private fun createApprecError(errorMessage: String? = null): CV = objectFactory.createCV().apply {
    s = OID_8221
    v = "T99"
    dn = "Annen feil på format"
    ot = errorMessage
}

private fun createReceiver(msgInfo: MsgInfo): Receiver = objectFactory.createAppRecReceiver().apply {
    val org = msgInfo.sender.organisation
    val name = org.organisationName
    val ident = org.ident.getPreferredIdent()
    hcp = createHealthCareProfessional(ident.id, name, OrgUnit.valueOf(ident.typeId.v), org.healthcareProfessional)
}

private fun createHealthCareProfessional(id: String, name: String, orgUnit: OrgUnit, healthcareProfessional: HealthcareProfessional? = null): HCP =
    objectFactory.createHCP().apply {
        inst = objectFactory.createInst().apply {
            this.id = id
            this.name = name
            this.typeId = createCS(orgUnit.name, orgUnit.description)
            if (healthcareProfessional != null) {
                this.hcPerson.add(createHCPerson(healthcareProfessional))
            }
        }
    }

private fun createHCPerson(healthcareProfessional: HealthcareProfessional): HCPerson =
    objectFactory.createHCPerson().apply {
        val ident: Ident = healthcareProfessional.ident.getPreferredIdent()
        id = ident.id
        typeId = createCS(ident.typeId.v)
        name = StringUtils.normalizeSpace(
            StringUtils.join(
                listOf(
                    healthcareProfessional.givenName,
                    healthcareProfessional.middleName,
                    healthcareProfessional.familyName
                ),
                ' '
            )
        )
    }

private fun createSender(): AppRec.Sender =
    objectFactory.createAppRecSender().apply {
        hcp = createHealthCareProfessional(NAV_ORGNO, NAV, OrgUnit.ENH).apply {
            inst.dept.add(objectFactory.createDept().also { it.name = DEPT_NAME })
        }
    }

private fun createOriginalMessageId(msgInfo: MsgInfo) = objectFactory.createOriginalMsgId().apply {
    id = msgInfo.msgId
    issueDate = msgInfo.genDate
    msgType = createCS(msgInfo.type.v, msgInfo.type.dn)
}

private fun createCS(v: String, dn: String? = null): CS = objectFactory.createCS().apply {
    this.v = v
    if (dn != null) this.dn = dn
}

private fun List<Ident>.getPreferredIdent(): Ident =
    this.firstOrNull { it.typeId.v == OrgUnit.ENH.name }
        ?: this.firstOrNull { it.typeId.v == OrgUnit.HER.name }
        ?: this.firstOrNull() ?: throw RuntimeException("Ident liste tom, dette skal ikke skje!")

private enum class OrgUnit(val description: String) {
    ENH("Organisasjonsnummeret i Enhetsregisteret"),
    HER("Identifikator fra Helsetjenesteenhetsregisteret (HER-id)"),
    LIN("Lokal identifikator for institusjoner");
}
