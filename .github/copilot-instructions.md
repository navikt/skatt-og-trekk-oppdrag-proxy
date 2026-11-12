---
applyTo: "**"
---

# skatt-og-trekk-oppdrag-proxy

Kotlin/Ktor backend-template for Team Skatt og Trekk (`skatt-og-trekk`-namespace på nais/gcp).
Stack: Ktor · Azure AD JWT · Micrometer/Prometheus · Kotest · Gradle Kotlin DSL · JVM 25

## Fil-spesifikke instruksjoner

Copilot laster detaljerte regler automatisk basert på filtype:
- **Kotlin-kode**: `.github/instructions/kotlin-ktor.instructions.md`
- **Tester**: `.github/instructions/testing.instructions.md`

## Kritiske regler — gjelder alltid

- Logg aldri PII (fnr, navn, adresse) uten `TEAM_LOGS_MARKER`
- All konfig via `PropertiesConfig` — aldri `System.getenv()` i forretningslogikk
- Aldri `runBlocking` i produksjonskode eller testblokker
- Aldri `!!` uten forutgående null-sjekk
- Aldri `resources.limits.cpu` i Nais-manifest
- Kjør `./gradlew build` etter alle endringer for å verifisere

