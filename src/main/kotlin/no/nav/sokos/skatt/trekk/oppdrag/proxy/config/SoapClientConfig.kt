package no.nav.sokos.skatt.trekk.oppdrag.proxy.config

import java.net.URI
import javax.xml.namespace.QName

import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.transport.http.HTTPConduit
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy

import no.nav.okonomi.skatteopplysningv110.SkatteopplysningPort
import no.nav.okonomi.skatteopplysningv200.Skatteopplysningv200Port
import no.nav.sokos.skatt.trekk.oppdrag.proxy.soap.CallIdInterceptor

internal fun getSkatteopplysningV110SoapClient(
    endpointUrl: String,
    properties: PropertiesConfig.SoapProperties,
): SkatteopplysningPort = createSoapClient(endpointUrl, "110", "Skatteopplysning", properties)

internal fun getSkatteopplysningV200SoapClient(properties: PropertiesConfig.SoapProperties): Skatteopplysningv200Port =
    createSoapClient(properties.skatteopplysningV200EndpointUrl, "200", "Skatteopplysning2", properties)

private inline fun <reified T> createSoapClient(
    endpointUrl: String,
    version: String,
    portName: String,
    properties: PropertiesConfig.SoapProperties,
): T {
    val uri = URI(endpointUrl)
    require(uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank() && uri.userInfo == null) {
        "SOAP endpoint must be an absolute HTTP(S) URL without credentials"
    }
    val namespace = "http://okonomi.nav.no/Skatteopplysningv$version/"
    return JaxWsProxyFactoryBean()
        .apply {
            address = endpointUrl
            wsdlURL = "wsdl/Skatteopplysningv$version.wsdl"
            serviceName = QName(namespace, "Skatteopplysningv$version")
            endpointName = QName(namespace, portName)
            serviceClass = T::class.java
            outInterceptors.add(CallIdInterceptor())
        }.create(T::class.java)
        .also {
            val conduit = ClientProxy.getClient(it).conduit as HTTPConduit
            conduit.client =
                HTTPClientPolicy().apply {
                    connectionTimeout = properties.connectionTimeoutMs
                    receiveTimeout = properties.receiveTimeoutMs
                    isAutoRedirect = false
                }
        }
}
