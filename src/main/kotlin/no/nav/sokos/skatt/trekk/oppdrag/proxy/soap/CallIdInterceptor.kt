package no.nav.sokos.skatt.trekk.oppdrag.proxy.soap

import java.util.UUID
import javax.xml.namespace.QName

import org.apache.cxf.binding.soap.SoapHeader
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.jaxb.JAXBDataBinding
import org.apache.cxf.phase.AbstractPhaseInterceptor
import org.apache.cxf.phase.Phase

class CallIdInterceptor(
    private val callIdGenerator: () -> String = { UUID.randomUUID().toString() },
) : AbstractPhaseInterceptor<SoapMessage>(Phase.PRE_STREAM) {
    override fun handleMessage(message: SoapMessage) {
        message.headers.add(
            SoapHeader(
                QName("uri:no.nav.applikasjonsrammeverk", "callId"),
                callIdGenerator(),
                JAXBDataBinding(String::class.java),
            ),
        )
    }
}
