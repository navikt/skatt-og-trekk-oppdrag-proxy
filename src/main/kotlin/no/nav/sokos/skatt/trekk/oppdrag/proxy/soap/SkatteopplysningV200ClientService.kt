package no.nav.sokos.skatt.trekk.oppdrag.proxy.soap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import no.nav.okonomi.skatteopplysningv200.HentSkattevedtakResponse
import no.nav.okonomi.skatteopplysningv200.LagreEtterbetalingTidligereAarRequest
import no.nav.okonomi.skatteopplysningv200.OmregnSkattRequest
import no.nav.okonomi.skatteopplysningv200.OppdaterSkattetrekkvedtakRequest
import no.nav.okonomi.skatteopplysningv200.Skatteopplysningv200Port
import no.nav.okonomi.skatteopplysningv200.Tilbakemelding
import no.nav.sokos.skatt.trekk.oppdrag.proxy.config.PropertiesConfig
import no.nav.sokos.skatt.trekk.oppdrag.proxy.config.getSkatteopplysningV200SoapClient

class SkatteopplysningV200ClientService(
    private val port: Skatteopplysningv200Port,
) {
    constructor(properties: PropertiesConfig.SoapProperties = PropertiesConfig.soapProperties) : this(
        getSkatteopplysningV200SoapClient(properties),
    )

    suspend fun hentSkattevedtak(debitor: String): HentSkattevedtakResponse =
        withContext(Dispatchers.IO) {
            port.hentSkattevedtak(debitor)
        }

    suspend fun hentHistoriskeSkattevedtak(debitor: String): HentSkattevedtakResponse =
        withContext(Dispatchers.IO) {
            port.hentHistoriskeSkattevedtak(debitor)
        }

    suspend fun oppdatereSkattetrekkvedtak(request: OppdaterSkattetrekkvedtakRequest): Tilbakemelding =
        withContext(Dispatchers.IO) {
            port.oppdatereSkattetrekkvedtak(request)
        }

    suspend fun lagreEtterbetalingTidligereAar(request: LagreEtterbetalingTidligereAarRequest): Tilbakemelding =
        withContext(Dispatchers.IO) {
            port.lagreEtterbetalingTidligereAar(request)
        }

    suspend fun omregnSkatt(request: OmregnSkattRequest): Tilbakemelding =
        withContext(Dispatchers.IO) {
            port.omregnSkatt(request)
        }
}
