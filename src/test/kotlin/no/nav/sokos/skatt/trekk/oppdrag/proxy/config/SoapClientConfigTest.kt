package no.nav.sokos.skatt.trekk.oppdrag.proxy.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.getAs
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.transport.http.HTTPConduit

class SoapClientConfigTest :
    FunSpec({
        val properties =
            PropertiesConfig.SoapProperties(
                skatteopplysningV110HentEndpointUrl = "https://soap.invalid/hent",
                skatteopplysningV110LagreEndpointUrl = "https://soap.invalid/lagre",
                skatteopplysningV200EndpointUrl = "https://soap.invalid/v200",
                connectionTimeoutMs = 1234,
                receiveTimeoutMs = 5678,
            )

        test("setter tidsgrenser og tillater ikke videresending av SOAP-kall") {
            val ports =
                listOf(
                    getSkatteopplysningV110SoapClient(properties.skatteopplysningV110HentEndpointUrl, properties),
                    getSkatteopplysningV200SoapClient(properties),
                )
            ports.forEach { port ->
                val client = ClientProxy.getClient(port)
                try {
                    val policy = (client.conduit as HTTPConduit).client
                    policy.connectionTimeout shouldBe 1234L
                    policy.receiveTimeout shouldBe 5678L
                    policy.isAutoRedirect shouldBe false
                } finally {
                    client.destroy()
                }
            }
        }

        test("avviser manglende endepunkt, andre protokoller og credentials i URL") {
            listOf("", "/relative", "file:///tmp/soap", "https://user:password@soap.invalid").forEach { endpoint ->
                shouldThrow<IllegalArgumentException> {
                    getSkatteopplysningV110SoapClient(endpoint, properties)
                }
                shouldThrow<IllegalArgumentException> {
                    getSkatteopplysningV200SoapClient(properties.copy(skatteopplysningV200EndpointUrl = endpoint))
                }
            }
        }

        test("avviser tidsgrenser som ellers ville deaktivert timeout") {
            shouldThrow<IllegalArgumentException> { properties.copy(connectionTimeoutMs = 0) }
            shouldThrow<IllegalArgumentException> { properties.copy(receiveTimeoutMs = -1) }
        }

        test("kan lese SOAP-konfigurasjon fra HOCON") {
            val configured = ApplicationConfig("application-test.conf").property("soap").getAs<PropertiesConfig.SoapProperties>()

            configured.connectionTimeoutMs shouldBe 10_000L
            configured.receiveTimeoutMs shouldBe 60_000L
        }
    })
