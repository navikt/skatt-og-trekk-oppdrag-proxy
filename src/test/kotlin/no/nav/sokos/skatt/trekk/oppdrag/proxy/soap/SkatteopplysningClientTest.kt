package no.nav.sokos.skatt.trekk.oppdrag.proxy.soap

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.util.concurrent.LinkedBlockingQueue
import java.util.logging.Logger
import javax.xml.parsers.DocumentBuilderFactory

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import jakarta.xml.ws.soap.SOAPFaultException
import org.apache.cxf.endpoint.Client
import org.apache.cxf.endpoint.PreexistingConduitSelector
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.message.Message
import org.apache.cxf.message.MessageImpl
import org.apache.cxf.transport.AbstractConduit
import org.apache.cxf.transport.MessageObserver
import org.apache.cxf.ws.addressing.EndpointReferenceUtils

import no.nav.okonomi.skatteopplysningv110.Melding
import no.nav.okonomi.skatteopplysningv110.Skatteopplysning
import no.nav.okonomi.skatteopplysningv110.SkatteopplysningsParametere
import no.nav.okonomi.skatteopplysningv200.LagreEtterbetalingTidligereAarRequest
import no.nav.okonomi.skatteopplysningv200.OmregnSkattRequest
import no.nav.okonomi.skatteopplysningv200.OppdaterSkattetrekkvedtakRequest
import no.nav.okonomi.skatteopplysningv200.T1SkattetrekkEtterbet
import no.nav.sokos.skatt.trekk.oppdrag.proxy.config.PropertiesConfig
import no.nav.sokos.skatt.trekk.oppdrag.proxy.config.getSkatteopplysningV110SoapClient
import no.nav.sokos.skatt.trekk.oppdrag.proxy.config.getSkatteopplysningV200SoapClient

class SkatteopplysningClientTest :
    FunSpec({
        test("v1.10 henter skatteopplysning med historikk fra hente-endepunktet") {
            val response =
                soapResponse(
                    "110",
                    "hentSkatteopplysning",
                    "hentSkatteopplysningResponse",
                    "<slaaAvAutomatiskValgAvDel>true</slaaAvAutomatiskValgAvDel>" +
                        "<returmelding><kode>OK</kode><alvorlighetsgrad>00</alvorlighetsgrad><beskrivelse>OK</beskrivelse></returmelding>",
                )
            SoapStub(response).use { stub ->
                val service = stub.v110Service()
                val request =
                    SkatteopplysningsParametere().apply {
                        trekkvedtakId = 42
                        isHistorikk = true
                    }

                val result = service.hentSkatteopplysning(request)

                result.isSlaaAvAutomatiskValgAvDel shouldBe true
                result.returmelding.alvorlighetsgrad shouldBe "00"
                stub.verifyRequest("/hent", "110", "hentSkatteopplysning", "trekkvedtakId" to "42", "historikk" to "true")
            }
        }

        test("v1.10 lagrer skatteopplysning til lagre-endepunktet og bevarer faglig feilmelding") {
            val response =
                soapResponse(
                    "110",
                    "lagreSkatteopplysning",
                    "lagreSkatteopplysningResponse",
                    "<kode>FEIL</kode><alvorlighetsgrad>08</alvorlighetsgrad><beskrivelse>Avvist</beskrivelse>",
                )
            SoapStub(response).use { stub ->
                val service = stub.v110Service()
                val request =
                    Skatteopplysning().apply {
                        isSlaaAvAutomatiskValgAvDel = true
                        returmelding =
                            Melding().apply {
                                kode = "OK"
                                alvorlighetsgrad = "00"
                                beskrivelse = "OK"
                            }
                    }

                val result = service.lagreSkatteopplysning(request)

                result.alvorlighetsgrad shouldBe "08"
                result.beskrivelse shouldBe "Avvist"
                stub.verifyRequest("/lagre", "110", "lagreSkatteopplysning", "slaaAvAutomatiskValgAvDel" to "true")
            }
        }

        listOf("hentSkattevedtak", "hentHistoriskeSkattevedtak").forEach { operation ->
            test("v2.00 kaller $operation med debitor og leser svaret") {
                val response =
                    soapResponse(
                        "200",
                        operation,
                        "hentSkattevedtakResponse",
                        "<tilbakemelding><returKode>00</returKode><returMelding>OK</returMelding></tilbakemelding>",
                    )
                SoapStub(response).use { stub ->
                    val service = stub.v200Service()

                    val result =
                        if (operation == "hentSkattevedtak") {
                            service.hentSkattevedtak("00000000000")
                        } else {
                            service.hentHistoriskeSkattevedtak("00000000000")
                        }

                    result.tilbakemelding.returKode shouldBe "00"
                    stub.verifyRequest("/v200", "200", operation, "debitor" to "00000000000")
                }
            }
        }

        test("v2.00 oppdaterer skattetrekkvedtak med SOAPAction fra WSDL") {
            SoapStub(tilbakemeldingResponse("oppdatereSkattetrekkvedtak")).use { stub ->
                val service = stub.v200Service()
                val request =
                    OppdaterSkattetrekkvedtakRequest().apply {
                        offnr = "00000000000"
                        brukerId = "TEST"
                        antallIBruk = 0
                        trekkvedtakAngitt = "N"
                    }

                service.oppdatereSkattetrekkvedtak(request).returKode shouldBe "04"

                stub.verifyRequest(
                    "/v200",
                    "200",
                    "oppdatereSkattetrekkvedtak",
                    "offnr" to "00000000000",
                    "brukerId" to "TEST",
                    soapAction = "oppdaterSkattetrekkvedtak",
                )
            }
        }

        test("v2.00 lagrer etterbetaling for tidligere aar") {
            SoapStub(tilbakemeldingResponse("lagreEtterbetalingTidligereAar")).use { stub ->
                val service = stub.v200Service()
                val request =
                    LagreEtterbetalingTidligereAarRequest().apply {
                        etterbetalingSkattevedtak =
                            T1SkattetrekkEtterbet().apply {
                                trekkvedtakId = 42
                                aarTrekkperFom = "2024"
                                aarTrekkperTom = "2025"
                            }
                    }

                service.lagreEtterbetalingTidligereAar(request).returKode shouldBe "04"

                stub.verifyRequest(
                    "/v200",
                    "200",
                    "lagreEtterbetalingTidligereAar",
                    "trekkvedtakId" to "42",
                    "aarTrekkperFom" to "2024",
                    "aarTrekkperTom" to "2025",
                )
            }
        }

        test("v2.00 starter omregning av skatt") {
            SoapStub(tilbakemeldingResponse("omregnSkatt")).use { stub ->
                val service = stub.v200Service()
                val request =
                    OmregnSkattRequest().apply {
                        trekkvedtakId = 42
                        brukerId = "TEST"
                    }

                service.omregnSkatt(request).returKode shouldBe "04"

                stub.verifyRequest("/v200", "200", "omregnSkatt", "trekkvedtakId" to "42", "brukerId" to "TEST")
            }
        }

        test("SOAP-feil videreformidles uten aa gjenta et oppdateringskall") {
            val fault =
                """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                    <soap:Body><soap:Fault><faultcode>soap:Server</faultcode><faultstring>Avvist</faultstring></soap:Fault></soap:Body>
                </soap:Envelope>
                """.trimIndent()
            SoapStub(fault, 500).use { stub ->
                val service = stub.v200Service()

                shouldThrow<SOAPFaultException> {
                    service.omregnSkatt(OmregnSkattRequest().apply { trekkvedtakId = 42 })
                }.fault.faultString shouldBe "Avvist"

                stub.verifyRequest("/v200", "200", "omregnSkatt", "trekkvedtakId" to "42")
            }
        }
    })

private fun tilbakemeldingResponse(operation: String): String =
    soapResponse(
        "200",
        operation,
        "tilbakemelding",
        "<returKode>04</returKode><returMelding>Advarsel</returMelding>",
    )

private fun soapResponse(
    version: String,
    operation: String,
    part: String,
    content: String,
): String =
    """
    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
        <soap:Body>
            <ns:${operation}Response xmlns:ns="http://okonomi.nav.no/Skatteopplysningv$version/">
                <$part>$content</$part>
            </ns:${operation}Response>
        </soap:Body>
    </soap:Envelope>
    """.trimIndent()

private class SoapStub(
    private val response: String,
    private val status: Int = 200,
) : AutoCloseable {
    private val requests = LinkedBlockingQueue<SoapRequest>()
    private val clients = mutableListOf<Client>()
    private val properties =
        PropertiesConfig.SoapProperties(
            skatteopplysningV110HentEndpointUrl = "https://soap.invalid/hent",
            skatteopplysningV110LagreEndpointUrl = "https://soap.invalid/lagre",
            skatteopplysningV200EndpointUrl = "https://soap.invalid/v200",
        )

    fun v110Service(): SkatteopplysningV110ClientService =
        SkatteopplysningV110ClientService(
            getSkatteopplysningV110SoapClient(properties.skatteopplysningV110HentEndpointUrl, properties).withStubTransport(),
            getSkatteopplysningV110SoapClient(properties.skatteopplysningV110LagreEndpointUrl, properties).withStubTransport(),
        )

    fun v200Service(): SkatteopplysningV200ClientService = SkatteopplysningV200ClientService(getSkatteopplysningV200SoapClient(properties).withStubTransport())

    private fun <T> T.withStubTransport(): T =
        also { port ->
            val client = ClientProxy.getClient(port)
            clients.add(client)
            val conduit =
                object : AbstractConduit(EndpointReferenceUtils.getEndpointReference("https://soap.invalid")) {
                    override fun getLogger(): Logger = Logger.getLogger(SoapStub::class.java.name)

                    override fun prepare(message: Message) {
                        message.setContent(OutputStream::class.java, ByteArrayOutputStream())
                    }

                    override fun close(message: Message) {
                        val output = message.getContent(OutputStream::class.java) as? ByteArrayOutputStream ?: return
                        val headers = message[Message.PROTOCOL_HEADERS] as Map<*, *>
                        requests.add(
                            SoapRequest(
                                URI(message[Message.ENDPOINT_ADDRESS] as String).path,
                                (headers["SOAPAction"] as List<*>).single().toString().trim('"'),
                                output.toString(Charsets.UTF_8),
                            ),
                        )
                        val incoming =
                            MessageImpl().apply {
                                exchange = message.exchange
                                put(Message.RESPONSE_CODE, status)
                                put(Message.CONTENT_TYPE, "text/xml; charset=UTF-8")
                                setContent(InputStream::class.java, response.byteInputStream())
                            }
                        messageObserver.onMessage(incoming)
                    }
                }.apply {
                    messageObserver = client as MessageObserver
                }
            client.conduitSelector =
                PreexistingConduitSelector(conduit).apply {
                    endpoint = client.endpoint
                }
        }

    fun verifyRequest(
        path: String,
        version: String,
        operation: String,
        vararg fields: Pair<String, String>,
        soapAction: String = operation,
    ) {
        requests.size shouldBe 1
        val request = requests.remove()
        request.path shouldBe path
        request.soapAction shouldBe "http://okonomi.nav.no/Skatteopplysningv$version/$soapAction"
        val document =
            DocumentBuilderFactory
                .newNSInstance()
                .newDocumentBuilder()
                .parse(request.body.byteInputStream())
        document.getElementsByTagNameNS("http://okonomi.nav.no/Skatteopplysningv$version/", operation).length shouldBe 1
        document
            .getElementsByTagNameNS("uri:no.nav.applikasjonsrammeverk", "callId")
            .item(0)
            .textContent
            .shouldNotBeBlank()
        fields.forEach { (name, value) ->
            document.getElementsByTagNameNS("*", name).item(0).textContent shouldBe value
        }
    }

    override fun close() {
        clients.forEach { it.destroy() }
    }
}

private data class SoapRequest(
    val path: String,
    val soapAction: String,
    val body: String,
)
