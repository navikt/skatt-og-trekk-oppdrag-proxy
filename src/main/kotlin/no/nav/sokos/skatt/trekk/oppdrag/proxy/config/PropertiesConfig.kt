package no.nav.sokos.skatt.trekk.oppdrag.proxy.config

import kotlinx.serialization.Serializable

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.HoconApplicationConfig
import io.ktor.server.config.getAs
import io.ktor.server.config.withFallback

private const val APPLICATION_CONF = "application"
private const val AZUREAD_CONF = "azureAd"

object PropertiesConfig {
    lateinit var config: ApplicationConfig
        private set

    fun load(applicationConfig: ApplicationConfig) {
        if (!::config.isInitialized) {
            config = applicationConfig
        }
    }

    val applicationProperties by lazy {
        config.property(APPLICATION_CONF).getAs<ApplicationProperties>()
    }

    val azureAdProperties by lazy {
        config.property(AZUREAD_CONF).getAs<AzureAdProperties>()
    }

    val soapProperties by lazy {
        config.property("soap").getAs<SoapProperties>()
    }

    @Serializable
    data class ApplicationProperties(
        val profile: Profile,
        val appName: String,
        val namespace: String,
    ) {
        val isLocal = profile == Profile.LOCAL
        val isTest = profile == Profile.TEST
        val isProd = profile == Profile.PROD
    }

    @Serializable
    data class AzureAdProperties(
        val clientId: String,
        val wellKnownUrl: String,
    )

    @Serializable
    data class SoapProperties(
        val skatteopplysningV110HentEndpointUrl: String,
        val skatteopplysningV110LagreEndpointUrl: String,
        val skatteopplysningV200EndpointUrl: String,
        val connectionTimeoutMs: Long = 10_000,
        val receiveTimeoutMs: Long = 60_000,
    ) {
        init {
            require(connectionTimeoutMs > 0) { "connectionTimeoutMs must be positive" }
            require(receiveTimeoutMs > 0) { "receiveTimeoutMs must be positive" }
        }
    }
}

fun ApplicationConfig.loadEnv(): ApplicationConfig {
    val hoconConfig = HoconApplicationConfig(ConfigFactory.load())
    val environmentName = System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")
    val environment = environmentName?.lowercase()?.substringBefore("-") ?: "local"

    val environmentConfig = ApplicationConfig("application-$environment.conf")
    return environmentConfig overriding this overriding hoconConfig
}

infix fun ApplicationConfig.overriding(other: ApplicationConfig): ApplicationConfig = this.withFallback(other)

enum class Profile {
    LOCAL,
    TEST,
    DEV,
    PROD,
}
