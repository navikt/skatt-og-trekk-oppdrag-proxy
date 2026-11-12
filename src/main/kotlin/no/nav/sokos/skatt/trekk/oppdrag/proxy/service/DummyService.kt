package no.nav.sokos.skatt.trekk.oppdrag.proxy.service

import mu.KotlinLogging

import no.nav.sokos.skatt.trekk.oppdrag.proxy.config.TEAM_LOGS_MARKER
import no.nav.sokos.skatt.trekk.oppdrag.proxy.domain.DummyDomain
import no.nav.sokos.skatt.trekk.oppdrag.proxy.metrics.Metrics

private val logger = KotlinLogging.logger {}

class DummyService {
    fun sayHello(): DummyDomain {
        Metrics.exampleCounter.inc()
        logger.info(marker = TEAM_LOGS_MARKER) { "Dette havner i team logs!" }
        return DummyDomain("This is a template for Team Skatt og Trekk")
    }
}
