package no.nav.sokos.skatt.trekk.oppdrag.proxy.config

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing

import no.nav.sokos.skatt.trekk.oppdrag.proxy.api.dummyApi
import no.nav.sokos.skatt.trekk.oppdrag.proxy.api.dummySwaggerApi

fun Application.routingConfig(applicationState: ApplicationState) {
    routing {
        internalNaisRoutes(applicationState)
        dummySwaggerApi()
        authenticate(AUTHENTICATION_NAME) {
            dummyApi()
        }
    }
}
