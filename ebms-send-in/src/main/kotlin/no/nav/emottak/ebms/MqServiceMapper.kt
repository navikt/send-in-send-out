package no.nav.emottak.ebms

import no.nav.emottak.ebms.utils.AsyncRoutingAction
import no.nav.emottak.ebms.utils.SupportedAsyncServiceType

// Noen tjenester mappes direkte til 1 service, noen mappes til action-spesifikke services
class MqServiceMapper(val mqServiceMap: MutableMap<SupportedAsyncServiceType, MutableMap<String, MqService>> = mutableMapOf()) {

    fun addMqService(service: SupportedAsyncServiceType, mqService: MqService) {
        mqServiceMap.put(service, mutableMapOf("" to mqService))
    }

    fun get(service: SupportedAsyncServiceType): MqService? {
        val s = mqServiceMap.get(service)
        if (s == null) return null
        if (s.size == 1) return s.values.first()
        throw RuntimeException("Multiple MQ services found for $service, action must be specified")
    }

    fun addMqService(action: AsyncRoutingAction, mqService: MqService) {
        val map = mqServiceMap.get(action.service) ?: mutableMapOf()
        map.put(action.name, mqService)
        mqServiceMap.put(action.service, map)
    }

    fun get(action: AsyncRoutingAction): MqService? {
        mqServiceMap.get(action.service)?.let {
            return it.get(action.name)
        }
        return null
    }

    fun list(): List<MqService> {
        return mqServiceMap.values.flatMap { it.values }
    }
}
