package no.nav.sokos.skatt.trekk.oppdrag.proxy.api

import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.Routing

fun Routing.dummySwaggerApi() {
    swaggerUI(
        path = "api/v1/docs",
        swaggerFile = "openapi/skatt-og-trekk-oppdrag-proxy-v1-swagger.yaml",
    )
}
