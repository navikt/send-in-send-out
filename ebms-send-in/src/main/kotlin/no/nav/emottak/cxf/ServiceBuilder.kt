package no.nav.emottak.cxf

import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.headers.Header
import org.apache.cxf.jaxb.JAXBDataBinding
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.transport.http.HTTPConduit
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor
import org.apache.wss4j.common.ConfigurationConstants
import org.apache.wss4j.common.ext.WSPasswordCallback
import org.slf4j.LoggerFactory
import javax.security.auth.callback.Callback
import javax.security.auth.callback.CallbackHandler
import javax.xml.namespace.QName

internal val log = LoggerFactory.getLogger("no.nav.emottak.cxf")

class ServiceBuilder<T>(resultClass: Class<T>) {
    var resultClass: Class<T>
    private val factoryBean: JaxWsProxyFactoryBean

    init {
        factoryBean = JaxWsProxyFactoryBean()
        factoryBean.serviceClass = resultClass
        this.resultClass = resultClass
    }

    fun withWsdl(wsdl: String?): ServiceBuilder<T> {
        factoryBean.wsdlURL = wsdl
        return this
    }

    fun withServiceName(name: QName?): ServiceBuilder<T> {
        factoryBean.serviceName = name
        return this
    }

    fun withEndpointName(name: QName?): ServiceBuilder<T> {
        factoryBean.endpointName = name
        return this
    }

    fun withAddress(address: String?): ServiceBuilder<T> {
        factoryBean.address = address
        return this
    }

    fun withLogging(): ServiceBuilder<T> {
        factoryBean.features.add(LoggingFeature())
        return this
    }

    fun withFrikortContextClasses(): ServiceBuilder<T> {
        factoryBean.properties = (factoryBean.properties ?: mutableMapOf()).apply {
            this["jaxb.additionalContextClasses"] = arrayOf(
                no.kith.xmlstds.nav.egenandel._2016_06_10.ObjectFactory::class.java,
                no.kith.xmlstds.nav.egenandel._2010_02_01.ObjectFactory::class.java,
                no.kith.xmlstds.nav.egenandelmengde._2016_06_10.ObjectFactory::class.java,
                no.kith.xmlstds.nav.egenandelmengde._2010_10_06.ObjectFactory::class.java
            )
        }
        return this
    }

    fun get(): JaxWsProxyFactoryBean {
        return factoryBean
    }

    fun build(): PortTypeBuilder<T> {
        return PortTypeBuilder<T>(factoryBean.create(resultClass))
    }

    inner class PortTypeBuilder<R>(val portType: R) {
        fun withBasicSecurity(username: String, password: String): PortTypeBuilder<R> {
            val conduit: HTTPConduit = ClientProxy.getClient(portType).conduit as HTTPConduit
            conduit.authorization.userName = username
            conduit.authorization.password = password
            return this
        }

        fun withOrgnrHeader(orgnr: String?): PortTypeBuilder<R> {
            if (orgnr == null) return this
            val headersList: MutableList<Header> = ArrayList()
            val testHeader = Header(
                QName("no.nav.emottak.utbetaling", "orgnr"),
                orgnr,
                JAXBDataBinding(String::class.java)
            )
            headersList.add(testHeader)
            ClientProxy.getClient(portType).contexts.requestContext[Header.HEADER_LIST] = headersList
            return this
        }

        fun withUserNameToken(username: String, password: String): PortTypeBuilder<R> {
            val map = HashMap<String, Any>()
            map[ConfigurationConstants.ACTION] = ConfigurationConstants.USERNAME_TOKEN
            map[ConfigurationConstants.PASSWORD_TYPE] = "PasswordText"
            map[ConfigurationConstants.USER] = username
            val passwordCallbackHandler = CallbackHandler { callbacks: Array<Callback> ->
                val callback = callbacks[0] as WSPasswordCallback
                callback.password = password
            }
            map.put(ConfigurationConstants.PW_CALLBACK_REF, passwordCallbackHandler)
            ClientProxy.getClient(portType).outInterceptors.add(WSS4JOutInterceptor(map))
            return this
        }

        fun get(): R {
            return portType
        }
    }
}
