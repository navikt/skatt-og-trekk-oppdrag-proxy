# Innholdsoversikt

# Tjenestekall avhengigheter
```
Tjenestekall fra skattogtrekk-web

Tjeneste              | Kall                           | Formål
----------------------|--------------------------------|-------------------------------------
SkattOgTrekkServiceEJB | K231M760                      | Søke skatte- og trekkvedtak
SkattOgTrekkServiceEJB | K231M770                      | Hente trekkdetaljer fra UR
SkattOgTrekkServiceEJB | HENTVEDTAK                    | Hente vedtak og historikk
SkattOgTrekkServiceEJB | OPPDATERVEDTAK                | Opprette/oppdatere vedtak og maksbeløp
SkattOgTrekkServiceEJB | HENTMAKSBELOP                 | Hente maksbeløp
Direkte CICS           | K231M792                      | Behandle negative trekk
Direkte CICS           | K231M810                      | Søke/hente forpliktelser
Direkte CICS           | K231M870                      | Søke/hente effektuerte trekk
Direkte CICS           | K231M841Inn                   | Kvittere trekkhendelser
Skatteopplysning v1.10 | hentSkatteopplysning          | Hente skatteopplysninger/historikk
Skatteopplysning v1.10 | lagreSkatteopplysning         | Lagre skatteopplysninger
Skatteopplysning v2.00 | hentSkattevedtak              | Hente gjeldende skattevedtak
Skatteopplysning v2.00 | hentHistoriskeSkattevedtak    | Hente historiske skattevedtak
Skatteopplysning v2.00 | oppdatereSkattetrekkvedtak    | Oppdatere vedtak og skattekort
Skatteopplysning v2.00 | lagreEtterbetalingTidligereAar| Lagre skatt for tidligere års etterbet.
Skatteopplysning v2.00 | omregnSkatt                   | Starte omregning av skatt
PDL                    | hentPerson                    | Hente navn/adresser/adressebeskyttelse
PDL                    | hentPersonBolk                | Kontrollere adressebeskyttelse i bolk
NOM/skjerming          | skjermet                      | Kontrollere skjerming av én person
NOM/skjerming          | skjermetBulk                  | Kontrollere skjerming i bolk
Azure AD               | client_credentials            | Hente token til PDL og NOM
TSS                    | B910                          | Hente/søke samhandlere med ID
TSS                    | B985                          | Søke samhandlere etter navn
TSS                    | B986                          | Søke samhandlere etter gironummer
TSS                    | B990                          | Hente kodeverk
Enhetsregisteret       | SR01                          | Hente organisasjonsnavn
Elektronisk mottak     | sendMessage → jms/innrappTrekk| Sende XML på nytt ved manuell oppfølging
SessionHolderEJB       | getSessionTable               | Hente sesjonsdata ved innlogging
SessionHolderEJB       | removeUserContainer           | Hente brukerdata ved innlogging
Oppdrag DB2            | JDBC/Hibernate → jdbc/Oppdrag | Lese/oppdatere trekkdata og kodeverk

Omfatter direkte kall og kall gjennom fellesmodulene.
Basert på kildekoden, ikke observerte produksjonskall.
```

## SOAP-klienter i proxyen

`soap/SkatteopplysningV110ClientService` og `soap/SkatteopplysningV200ClientService`
dekker de sju SOAP-operasjonene i listen over:

| Klient | Operasjoner |
|--------|-------------|
| Skatteopplysning v1.10 | `hentSkatteopplysning`, `lagreSkatteopplysning` |
| Skatteopplysning v2.00 | `hentSkattevedtak`, `hentHistoriskeSkattevedtak`, `oppdatereSkattetrekkvedtak`, `lagreEtterbetalingTidligereAar`, `omregnSkatt` |

Klientene er utgående integrasjoner, ikke REST-endepunkter. De kan opprettes med
standardkonstruktøren etter at `PropertiesConfig` er lastet, eller injiseres med
SOAP-porter i tester. Metodene er `suspend` og kjører blokkerende SOAP-kall på
`Dispatchers.IO`. Faglige returmeldinger og SOAP-feil videreformidles uendret;
klientene foretar ikke automatiske applikasjonsretries av oppdateringer.

### Kontrakter og klientoppsett

WSDL-ene i `src/main/resources/wsdl/` er hentet fra `nav-oppdrag`:

- v1.10: `nav-oppdrag-cics-ws/src/main/resources/wsdl/Skatteopplysningv110.wsdl`
- v2.00: `nav-oppdrag-skattogtrekk-web/src/main/resources/eskattekort/Skatteopplysningv200.wsdl`

Gradle genererer Jakarta JAX-WS/JAXB-typer under `build/generated/sources/soap/`
før kompilering. Kontraktene bruker SOAP 1.1 RPC/literal, inkludert det eksisterende
avviket der operasjonen `oppdatereSkattetrekkvedtak` har SOAPAction
`oppdaterSkattetrekkvedtak`.

Apache CXF og `callId`-headeren følger mønsteret fra `sokos-trekk`.
STS/SAML og WS-Addressing er ikke aktivert: Skatteopplysning-portene i
`nav-oppdrag` bruker ikke dette, og WSDL-ene krever det ikke. SOAP-bodyer logges
ikke, og TLS-sertifikatvalidering er ikke deaktivert.

### Konfigurasjon

| Miljøvariabel | Bruk |
|--------------|------|
| `SKATTEOPPLYSNING_V110_HENT_ENDPOINT_URL` | Endepunkt for henting i v1.10 |
| `SKATTEOPPLYSNING_V110_LAGRE_ENDPOINT_URL` | Endepunkt for lagring i v1.10 |
| `SKATTEOPPLYSNING_V200_ENDPOINT_URL` | Felles endepunkt for de fem operasjonene i v2.00 |
| `SOAP_CONNECTION_TIMEOUT_MS` | Tilkoblingstimeout, standard 10000 ms |
| `SOAP_RECEIVE_TIMEOUT_MS` | Svartimeout, standard 60000 ms |

Endepunktene må konfigureres før klientene opprettes. Miljøspesifikke adresser og
tilhørende utgående Nais-tilgang må avklares før integrasjon mot dev/prod.
CICS/EJB-, REST/GraphQL-, MQ- og databaseintegrasjonene i listen er ikke omfattet.