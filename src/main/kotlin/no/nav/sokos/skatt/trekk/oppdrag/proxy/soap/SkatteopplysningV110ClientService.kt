package no.nav.sokos.skatt.trekk.oppdrag.proxy.soap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import no.nav.okonomi.skatteopplysningv110.Melding
import no.nav.okonomi.skatteopplysningv110.Skatteopplysning
import no.nav.okonomi.skatteopplysningv110.SkatteopplysningPort
import no.nav.okonomi.skatteopplysningv110.SkatteopplysningsParametere
import no.nav.sokos.skatt.trekk.oppdrag.proxy.config.PropertiesConfig
import no.nav.sokos.skatt.trekk.oppdrag.proxy.config.getSkatteopplysningV110SoapClient

class SkatteopplysningV110ClientService(
    private val hentPort: SkatteopplysningPort,
    private val lagrePort: SkatteopplysningPort,
) {
    constructor(properties: PropertiesConfig.SoapProperties = PropertiesConfig.soapProperties) : this(
        getSkatteopplysningV110SoapClient(properties.skatteopplysningV110HentEndpointUrl, properties),
        getSkatteopplysningV110SoapClient(properties.skatteopplysningV110LagreEndpointUrl, properties),
    )

    suspend fun hentSkatteopplysning(request: SkatteopplysningsParametere): Skatteopplysning =
        withContext(Dispatchers.IO) {
            hentPort.hentSkatteopplysning(request)
        }

    suspend fun lagreSkatteopplysning(request: Skatteopplysning): Melding =
        withContext(Dispatchers.IO) {
            lagrePort.lagreSkatteopplysning(request)
        }
}
