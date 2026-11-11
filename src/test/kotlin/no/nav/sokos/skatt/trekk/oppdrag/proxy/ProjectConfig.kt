package no.nav.sokos.skatt.trekk.oppdrag.proxy

import io.kotest.core.config.AbstractProjectConfig
import io.ktor.server.config.ApplicationConfig

import no.nav.sokos.skatt.trekk.oppdrag.proxy.config.PropertiesConfig

private const val APPLICATION_TEST_CONFIG = "application-test.conf"

class ProjectConfig : AbstractProjectConfig() {
    override suspend fun beforeProject() {
        PropertiesConfig.load(ApplicationConfig(APPLICATION_TEST_CONFIG))
    }
}
