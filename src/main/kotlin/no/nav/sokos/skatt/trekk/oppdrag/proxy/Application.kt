package no.nav.sokos.skatt.trekk.oppdrag.proxy

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KotlinLogging

import no.nav.sokos.skatt.trekk.oppdrag.proxy.config.ApplicationState
import no.nav.sokos.skatt.trekk.oppdrag.proxy.config.PropertiesConfig
import no.nav.sokos.skatt.trekk.oppdrag.proxy.config.applicationLifecycleConfig
import no.nav.sokos.skatt.trekk.oppdrag.proxy.config.commonConfig
import no.nav.sokos.skatt.trekk.oppdrag.proxy.config.loadEnv
import no.nav.sokos.skatt.trekk.oppdrag.proxy.config.routingConfig
import no.nav.sokos.skatt.trekk.oppdrag.proxy.config.securityConfig

private val logger = KotlinLogging.logger {}

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(true)
}

private fun Application.module() {
    PropertiesConfig.load(environment.config.loadEnv())

    val applicationState = ApplicationState()

    applicationLifecycleConfig(applicationState)
    commonConfig()
    securityConfig(PropertiesConfig.azureAdProperties)
    routingConfig(applicationState)

    logger.info { "Application started with profile: ${PropertiesConfig.applicationProperties.profile}" }
}
