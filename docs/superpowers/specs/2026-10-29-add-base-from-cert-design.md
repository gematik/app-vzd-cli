# Design: `admin <env> add-base --from-cert`, lokale Zertifikatsprüfung und klarere Fehlermeldungen

Datum: 2026-10-29
Status: Entwurf, wartet auf Freigabe

## Intent

Ein Verzeichniseintrag soll in einem Schritt direkt aus dem ENC-Zertifikat
(SMC-B: C.HCI.ENC, HBA: C.HP.ENC) angelegt werden. Heute zwingt der Ablauf
`add-base` → `add-cert` den Nutzer, `entryType` und weitere Werte zu raten,
die der VZD anschließend ohnehin aus dem Zertifikat ableitet. Für eine
Apotheke in der RU waren dadurch fünf Anläufe nötig (Issue, siehe unten).

Zielbild:

```
vzd-cli admin ru add-base --from-cert smcb.p12
```

legt den Basiseintrag an, hängt das Zertifikat im selben Request an, fragt
fehlende Pflichtwerte interaktiv ab und zeigt das Ergebnis.

## Spezifikationsgrundlage

Geprüft wurden:

- gemSpec_VZD 1.20.0 (Tab_VZD_Datenbeschreibung,
  Tab_VZD_Mapping_Eintragstyp_und_ProfessionOID, A_18448, A_20271-02,
  Zertifikatstyp-Prüfung, A_22224)
- gematik/api-vzd auf GitHub, Stand main vom 2026-10-28:
  `docs/gemILF_Pflege_VZD.adoc` Version 1.5.18 vom 11.03.2026
  (Tabelle_Plausi V001–V015, TAB_VZD_Wertebereiche_der_Attribute,
  Tabelle_TID_Mapping, Attributtabellen 2–20), `docs/LDAP2FHIR_Sync.adoc`,
  `docs/LDAP_Attribute.adoc`, `docs/FHIR_VZD_HOWTO_Data.adoc`,
  `src/openapi/DirectoryAdministration.yaml`
- gemSpec_OID Tab_PKI_405 (Zertifikatstyp-OIDs)

### Was der VZD bei `add_Directory_Entry` mit Zertifikat selbst setzt

| Attribut | Verhalten laut Spezifikation | Konsequenz für die CLI |
|---|---|---|
| `telematikID` | aus Admission.registrationNumber des Zertifikats; wird sie mitgegeben, muss sie übereinstimmen (sonst 422) | aus dem Zertifikat setzen (Pflichtfeld im Datenmodell der CLI) |
| `entryType` | aus professionOID über Mapping-Tabelle; ein mitgegebener Wert muss passen, sonst 400 | **nicht senden**. Lokal über das Mapping berechnen, nur zur Anzeige und um ein falsches `--set entryType` vor dem Upload abzufangen |
| `professionOID` | aus dem Zertifikat | nicht senden |
| `personalEntry`, `dataFromAuthority`, `holder` | vom VZD gesetzt (holder = aufrufender Client, wenn leer) | nicht senden |
| `cn` | Kopie von `displayName`, wenn nicht übergeben | nicht senden |
| `sn` | HBA: aus Zertifikat (surname); SMC-B: Kopie von `displayName` | nicht senden |
| `givenName` | HBA: aus Zertifikat; SMC-B: unzulässig (ILF 1.5.18) | nicht senden |
| `displayName` | **wird nicht aus dem Zertifikat ermittelt**; fehlt er, schreibt der VZD `-` (V007: mindestens 2 alphanumerische Zeichen); HBA-Konvention `Name, Vorname` | muss die CLI setzen |
| `streetAddress`, `postalCode`, `localityName` | nicht aus dem Zertifikat; Pflicht je Sektor (Matrix unten), serverseitig geprüft (V009, umgesetzt seit ILF 1.5.15) | aus dem Subject des Zertifikats vorbelegen, sonst nachfragen |
| `countryCode` | kein Default mehr (V015, ILF 1.5.17); sobald PLZ, Ort, Straße oder Bundesland geliefert wird, ist er Pflicht, sonst 400; ISO-3166-1 alpha-2 (V003) | aus Subject `C` übernehmen, sonst nachfragen mit Vorschlag `DE` |
| `stateOrProvinceName` | bei `DE` nur Werte aus CodeSystem Region (V001), sonst 422; Pflicht nur bei eGBR- und HWK-Institutionen | aus Subject `ST` übernehmen und validieren |
| `postalCode` | bei `DE` fünfstellig (V002) | validieren |
| `specialization` | Format je Sektor: Apotheken nur der Code aus PharmacyTypeLDAPCS (`"10"`, `"40"`, V006); andere SMC-B `urn:psc:<OID>:<Code>` aus HealthcareServiceTypeVS (V004); HBA `urn:as:<OID>:<Code>` aus PractitionerQualificationVS (V005). Pflicht je Sektor (Matrix) | Format sektorabhängig validieren, fehlende Pflichtwerte abfragen |
| `domainID` | Pflicht bei KV (BSNR), KZV, eGBR | abfragen, wenn Sektor es verlangt |
| `organization` | Pflicht nur bei GKV-SV; sonst optionaler Zweitname | abfragen, wenn Sektor es verlangt, sonst nur `--set` |
| `otherName`, `title`, `meta`, `maxKOMLEadr` | optional, teils veraltet | nur über `--set` |

Zertifikatsprüfung durch den VZD (gemSpec_VZD, Zertifikatstyp): Nur C.HCI.ENC
(Policy-OID `1.2.276.0.76.4.76`) oder C.HP.ENC (`1.2.276.0.76.4.74`) werden
akzeptiert, keyUsage ohne `digitalSignature`. Sonst 422 mit
`attributeName=userCertificate`. Die RU meldet dafür nur
`Error when KOMLECertPolicies-check!`. Beobachtung an den Testzertifikaten im
Repo: RSA-ENC hat `keyEncipherment, dataEncipherment`, ECC-ENC nur
`keyAgreement`. Die lokale Prüfung muss daher primär auf die Policy-OID gehen.
Zertifikate dürfen laut ILF 3.3.2 vor Freischaltung der Karte eingetragen
werden, eine OCSP-Vorprüfung wäre also falsch.

Weitere Typ-OIDs (gemSpec_OID Tab_PKI_405) für die Anzeige in `cert-info`:
C.HCI.AUT `.77`, C.HCI.OSIG `.78`, C.HP.AUT `.75`, C.HP.QES `.72`,
C.HP.SIG `.73`.

### Mapping entryType ↔ professionOID (Tab_VZD_Mapping_Eintragstyp_und_ProfessionOID)

| entryType | Bedeutung | professionOIDs (`1.2.276.0.76.4.` + …) |
|---|---|---|
| 1 | Berufsgruppe (HBA) | 30–48, 178, 232–241, 274–277, 305, 308, 312, 313, 315, 316, 318, 319 |
| 2 | Versicherte/-r | 49 |
| 3 | Leistungserbringerinstitution | 50–57, 245–257, 278–281, 304, 306, 311, 314, 317 |
| 4 | Organisation | 58, 187, 190, 210, 223–231, 242–244, 262–271, 284, 285, 292 |
| 5 | Krankenkasse | 59 |
| 7 | KIM | 286 |
| 8 | TIM | 295 |
| 9 | DiGA | 282 |
| 10 | Ombudsstelle | 303 |

Es gibt keinen Eintragstyp 6. Öffentliche Apotheke `.54` → entryType 3; das
README-Beispiel `entryType=4` ist für Apotheken falsch. `.273`
(Krankenversicherung ePA, im CLI-Mapping vorhanden) fehlt in der Spec-Tabelle;
unbekannte OIDs sind laut A_22224 jederzeit möglich und dürfen nicht blocken.

### Sektoren und Pflichtfelder (gemILF_Pflege_VZD Tabelle_TID_Mapping + Attributtabellen)

Der VZD ordnet jeden Eintrag **anhand der TelematikID** einer Attributtabelle
zu (ILF: "Der Sektor wird anhand der TelematikId erkannt") und prüft deren
Pflichtfelder (V009). Die professionOID spielt für den Sektor keine Rolle,
sie bestimmt nur den entryType. Eine TelematikID wie `3-2-Test01` aus dem
Issue passt zu keiner Tabelle; der Server prüft dann keine sektorspezifischen
Pflichtfelder, die CLI kann es auch nicht.

`displayName` und `holder` sind überall Pflicht (holder setzt der VZD).
Zusätzlich:

| TelematikID (regEx) | ILF-Tabelle | Adresse (Straße, PLZ, Ort) | Bundesland | specialization | weitere Pflicht | specialization-Format |
|---|---|---|---|---|---|---|
| `^1-20.*` | KVen (SMC-B) | Pflicht | – | Pflicht | domainID (BSNR) | `urn:psc:` |
| `^2-2.*` | KZVen (SMC-B) | Pflicht | – | Pflicht | domainID | `urn:psc:` |
| `^3-...2.*` oder `^9-2.54.*` | Apotheken (SMC-B) | Pflicht | – | Pflicht, LEER laut Tabelle zulässig | – | PharmacyTypeLDAPCS-Code |
| `^3-...3.*` oder `^9-1.(32\|33\|34).*` | Apotheker (HBA) | – | – | – | – | `urn:as:` |
| `^5-.*` | DKTIG (SMC-B) | Pflicht | – | Pflicht | – | `urn:psc:` |
| `^4-1.*` | Psychotherapeuten (HBA) | Pflicht | – | Pflicht | – | `urn:as:` |
| `^1-1*` | BÄK (HBA) | – | – | Pflicht | – | `urn:as:` |
| `^2-0?1.*` | BZÄK (HBA) | – | – | Pflicht | – | `urn:as:` |
| `^8-01.*` | GKV-SV SMC-B KTR | Pflicht | – | – | organization | – |
| `^8-(03\|04).*` | GKV-SV SM-B KTR ePA | Pflicht | – | – | organization | – |
| `^9-2.(58\|187\|210\|…\|295).*` | SMC-B Org (gematik) | Pflicht | – | – | – | `urn:psc:` |
| `^10-67.(245\|246\|247\|278\|279\|280\|281\|311).*` | eGBR Institutionen (SMC-B) | Pflicht | Pflicht | Pflicht | domainID | `urn:psc:` |
| `^10-67.(232\|…\|312).*` | eGBR Gesundheitsfachberufe (HBA) | – | – | Pflicht | domainID | `urn:as:` |
| `^9-2.(50\|51\|34\|254\|255\|256).*` | Weitere ärztliche Institutionen (SMC-B) | Pflicht | – | Pflicht | – | `urn:psc:` |
| `^9-2.(59\|273\|303).*` | Kostenträger/PKV (SMC-B) | Pflicht | – | – | – | – |
| `^9-2.282.*` | DiGA (SMC-B) | Pflicht | – | – | – | – |
| `^11-2.*` | HWK Institutionen (SMC-B) | Pflicht | Pflicht | Pflicht | – | `urn:psc:` |
| `^11-1.*` | HWK Gesundheitsfachberufe (BA) | – | – | Pflicht | – | `urn:as:` |
| `^9-2.292.*` | NCPeH (SMC-B) | – | – | – | – | – |

Die vollständigen Regex-Ausdrücke stehen im ILF (Tabelle_TID_Mapping) und
werden 1:1 in die Ressource übernommen.

### LDAP2FHIR-Synchronisation der Apotheken-Codes

`docs/LDAP2FHIR_Sync.adoc`, Tabelle "Synchronisierung der Attribute
professionOID & specialization für Apotheken":

| LDAP specialization | HealthcareService.type (PharmacyTypeCS) | HealthcareService.specialty (PharmacyHealthcareSpecialtyCS) |
|---|---|---|
| 10 Offizin-Apotheke | offizin-apotheke | 10 Handverkauf |
| 20 Krankenhausversorgende Apotheke | krankenhausversorgende-apotheke | – |
| 30 Heimversorgende Apotheke | heimversorgende-apotheke | – |
| 40 Versandapotheke | versandapotheke | 40 Versand |

Die Codes in `PharmacyTypeCS` (`offizin-apotheke`, …) sind also FHIR-Werte
und im LDAP-VZD **nicht** gültig; dort gelten nur die numerischen Codes aus
PharmacyTypeLDAPCS. Das erklärt die Verwirrung aus dem Issue.

### Befunde am Repo

- `PharmacySpecializationSpecialCase` löst Apotheken-Codes über
  `PharmacyHealthcareSpecialtyCS` auf (10 = Handverkauf). Für die LDAP-Anzeige
  in `show` ist das die falsche Beschriftung (10 = Offizin-Apotheke).
  `CodeSystem-PharmacyTypeLDAPCS.json` liegt im Repo, wird aber nicht geladen.
  Wird mit korrigiert, weil die Validierung dasselbe CodeSystem braucht.
- `pers extract` setzt beim Ableiten eines Basiseintrags aus dem Zertifikat
  hart `entryType = 1`; für SMC-B ist das falsch.
- `LDAP2FHIR_Sync.adoc` behauptet noch, der VZD ergänze `countryCode`
  automatisch mit dem Default für Deutschland. Das ist seit ILF 1.5.17
  überholt; gemSpec_VZD und ILF (V015) gelten.

## Goals

- `add-base --from-cert <file>` legt Basiseintrag und Zertifikat in einem
  `add_Directory_Entry`-Request an.
- Alle vom VZD abgeleiteten Werte werden nicht gesendet; die CLI rät nichts,
  was der Server besser weiß.
- Fehlende Pflichtwerte (sektorabhängig nach ILF) werden interaktiv
  abgefragt. Ohne Terminal oder mit `--no-prompt` bricht die CLI mit einer
  Liste der fehlenden Attribute und dem passenden `--set`-Hinweis ab.
- `.p12`/`.pfx` als Eingabe; das ENC-Zertifikat wird automatisch gewählt.
- `add-cert` prüft lokal vor dem Upload und erklärt den Fehler statt den
  Server-String zu zeigen.
- `cert-info` zeigt Zertifikatstyp und abgeleiteten entryType.
- Bekannte Server-Fehler (`entryType mismatch`, `HealthcareSpeciality
  mismatch`, `KOMLECertPolicies`, `stateOrProvinceName`) werden mit erlaubten
  Werten bzw. Mapping ergänzt.
- README-Beispiele korrigieren (Apotheke, entryType 3, specialization-Codes).

## Non-Goals

- Kein `add-cert --create-base`. `add-base --from-cert --ignore` deckt den
  Fall "Eintrag existiert schon, Zertifikat trotzdem anhängen" ab.
- Keine Änderung an `modify-base`/`modify-base-attr` über die Fehlerhinweise
  hinaus.
- Keine OCSP-Prüfung vor dem Upload (ILF erlaubt Eintrag vor Freischaltung;
  OCSP gibt es per `cert-info --ocsp`).
- Keine Umstellung der `kind`-Klassifikation (`infereKind`) auf professionOID.
  Der Server selbst arbeitet sektorbezogen mit der TelematikID; die
  `kind`-Anzeige ist FHIR-Mapping-Logik in `directory-lib/elaborate` und ein
  eigenes Thema. Als Folge-Issue festhalten.
- Keine GUI/BFF-Änderungen.

## Anti-Goals

- Werte "erfinden": kein stiller Default für `countryCode`, `specialization`
  oder `entryType`. Entweder aus dem Zertifikat, aus `--set`, aus einem
  Prompt mit sichtbarem Vorschlag, oder Abbruch.
- Server-Prüfungen nachbauen, die die CLI nicht sicher kennt (z. B.
  Dublettenprüfung, OCSP-Status). Lokale Prüfungen sind Vorprüfungen; der
  Server bleibt die Wahrheit.

## Constraints

- Kotlin/JVM 11, Clikt 3.5.1, BouncyCastle 1.70, kotlinx.serialization,
  yamlkt. Keine neuen Bibliotheken nötig: PKCS#12 über `java.security.KeyStore`,
  Policy-OIDs über BouncyCastle `X509CertificateHolder`.
- Keine Netzwerkzugriffe in Unit-Tests. Testzertifikate liegen in
  `vzd-cli/testdata` (RSA-ENC und ECC-ENC einer SMC-B, ein HBA-ENC mit zwei
  Professions). Ein Test-`.p12` wird im Test aus diesen Zertifikaten erzeugt
  (KeyStore mit trusted-cert-Einträgen, dazu ein generiertes Dummy-Schlüsselpaar
  mit selbstsigniertem Nicht-ENC-Zertifikat, damit der Fall "mehrere
  Kandidaten, genau ein ENC" abgedeckt ist).
- Bestehende Ausgaben (`cert-info`, `dump`) dürfen nur additiv wachsen:
  neue Felder in `CertificateInfo` mit Default `null`, damit `toYamlNoDefaults`
  alte Ausgaben nicht verändert, wenn der Wert nicht ermittelbar ist.
- Code-Stil wie im Repo (EUPL-Header, keine erklärenden Kommentare zu
  Offensichtlichem, ktlint).
- Commit-Messages ohne AI-Attribution.

## Entscheidungen

### Kommandozeile

```
vzd-cli admin <env> add-base --from-cert FILE [--passin pass:…|env:VAR|file:PATH]
                             [-s ATTR=VALUE]… [--no-prompt] [--ignore] [--yaml|--json]
```

- `--from-cert FILE` ist exklusiv zum Dateiargument (`add-base entry.yaml`);
  beides zusammen ist ein UsageError.
- `FILE`: DER, PEM (`-----BEGIN CERTIFICATE-----`) oder PKCS#12 (`.p12`,
  `.pfx`). Erkennung über Inhalt (PEM-Header, ASN.1-Struktur), nicht nur über
  Endung; Endung dient als Hinweis in Fehlermeldungen.
- `--passin` wie bei OpenSSL. Fehlt es bei PKCS#12 und ein Terminal ist da,
  wird das Passwort verdeckt abgefragt. Mit `--no-prompt` ohne `--passin` →
  UsageError.
- `--no-prompt`: niemals interaktiv nachfragen. Wird auch automatisch
  angenommen, wenn `System.console()` null ist (Pipe, CI).
- `-s/--set` wie bisher; überschreibt Werte aus dem Zertifikat. Für
  `entryType` gilt: wird er gesetzt und weicht vom Mapping ab, UsageError mit
  Erklärung (`Zertifikat professionOID 1.2.276.0.76.4.54 (Öffentliche
  Apotheke) ergibt entryType 3, angegeben wurde 4`). Stimmt er, wird er
  trotzdem nicht gesendet.
- `-s ATTR=` (leerer Wert) bedeutet im `--from-cert`-Pfad "bewusst leer":
  das Attribut wird nicht gesendet und ein Pflichtfeld-Befund dazu wird zur
  Warnung. Das ist der nicht-interaktive Weg, einen Wert aus dem Zertifikat
  wegzulassen oder eine Pflicht bewusst zu übergehen (z. B. Apotheke ohne
  specialization, was der ILF als LEER zulässt).
- `--ignore` (vorhanden): bei 409 wird der bestehende Eintrag gelesen und das
  Zertifikat per `add_Directory_Entry_Certificate` angehängt (ist dort schon
  dasselbe Zertifikat → Warnung, kein Fehler). Damit ersetzt
  `--from-cert --ignore` den Vorschlag `add-cert --create-base`.

`add-cert` bekommt dieselbe Zertifikatsquelle (DER/PEM/P12, `--passin`) und
die lokale Vorprüfung. Keine neuen Optionen außer `--passin`.

### Ablauf `add-base --from-cert`

1. Zertifikate laden (`CertificateSource`): aus DER/PEM genau eines, aus
   PKCS#12 alle Zertifikate (Key-Einträge und trusted certs, Chains werden
   mitgelesen, CA-Zertifikate ohne Admission-Extension ignoriert).
2. ENC-Kandidaten filtern: Policy-OID `.76` oder `.74`, keyUsage ohne
   `digitalSignature`, Admission-Extension vorhanden.
   - 0 Kandidaten: Fehler mit Liste aller gefundenen Endnutzer-Zertifikate
     (Subject-CN, erkannter Typ, Seriennummer) und Hinweis
     `VZD erwartet C.HCI.ENC/C.HP.ENC`.
   - Kandidaten außerhalb von notBefore/notAfter (lokale Uhr, keine OCSP)
     werden mit Warnung verworfen; der Server würde den gesamten Request
     daran scheitern lassen. Bleibt keiner übrig → Fehler.
   - ≥ 1 Kandidaten mit **derselben** TelematikID: alle werden mitgeschickt
     (`userCertificates`, bis 50 erlaubt; RSA- und ECC-ENC derselben Karte
     sind der Normalfall). Reihenfolge aufsteigend nach notBefore, weil der
     VZD die automatisch extrahierten Werte aus dem **letzten** Zertifikat
     nimmt (ILF, POST /DirectoryEntries). Der Basiseintrag wird aus
     demselben, also dem neuesten Zertifikat abgeleitet. Unterscheiden sich
     die Subjects der Kandidaten, wird das im Log vermerkt.
   - Kandidaten mit **verschiedenen** TelematikIDs: Fehler mit Liste, Nutzer
     muss die Datei trennen.
3. Basiseintrag ableiten (`BaseDirectoryEntry.fromCertificate`):
   - `telematikID` ← registrationNumber
   - `displayName` ← HBA (entryType 1): `"<sn>, <givenName>"` wenn beide im
     Subject stehen (Konvention aus gemSpec_VZD und LDAP2FHIR_Sync), sonst
     CN. SMC-B: CN.
   - `streetAddress` ← `STREET`, `postalCode` ← `postalCode`,
     `localityName` ← `L`, `stateOrProvinceName` ← `ST`
   - `countryCode` ← `C`, aber nur, wenn mindestens eine weitere
     Adresskomponente vorhanden ist. HBA-Zertifikate tragen `C=DE` ohne
     Adresse; ein Eintrag mit countryCode ohne Adresse ist sinnlos.
   - nur lokal abgeleitet: `entryType` (Mapping über professionOID), Sektor
     (ILF-Regex über TelematikID)
4. `--set` anwenden (bestehendes `setAttributes`; leere Werte werden im
   `--from-cert`-Pfad als "bewusst leer" vermerkt statt als `""` gesetzt).
5. Validieren (`BaseEntryPreflight`), Ergebnis ist eine Liste von Befunden
   mit Attributname, Schwere, Problem, Vorschlag, erlaubten Werten:
   - `displayName`: fehlt oder < 2 alphanumerische Zeichen → ERROR
   - Adresse: ist eines von Straße/PLZ/Ort/Bundesland gesetzt und
     `countryCode` fehlt → ERROR, Vorschlag `DE`
   - Sektor-Pflichtfelder aus der Matrix (Straße, PLZ, Ort, Bundesland,
     specialization, domainID, organization) fehlen → ERROR mit
     Sektorname und ILF-Tabelle im Text
   - `countryCode == DE`: PLZ fünfstellig → sonst ERROR;
     `stateOrProvinceName` nicht im Region-CodeSystem → ERROR mit Liste
     der 18 Werte
   - `specialization`-Format passt nicht zum Sektor (Apotheke: Code nicht
     in PharmacyTypeLDAPCS, inkl. dem Fall `offizin-apotheke`; SMC-B: kein
     `urn:psc:`; HBA: kein `urn:as:`) → ERROR mit erlaubten Werten bzw.
     Format und Beispiel
   - `urn:psc:`/`urn:as:` syntaktisch korrekt, Code aber nicht in der
     lokalen Kopie des ValueSets → WARNING (die Kopie kann dem Server
     hinterherhinken)
   - `entryType` aus `--set` ≠ Mapping → UsageError (siehe oben)
   - professionOID nicht im Mapping → WARNING
   - TelematikID passt zu keinem ILF-Sektor → WARNING ("keine
     sektorspezifische Prüfung möglich")
   - Attribut mit `-s ATTR=` bewusst leer gelassen → ein ERROR dazu wird zur
     WARNING
   Werte aus dem Zertifikat werden nie stillschweigend verändert oder
   weggelassen; ein ungültiger Wert aus dem Subject ist ein ERROR, der
   interaktiv korrigiert oder per `-s ATTR=` entfernt wird.
6. Befunde abarbeiten:
   - interaktiv: jeder ERROR wird per Clikt `prompt` abgefragt, mit Vorschlag
     (Zertifikatswert oder `DE`) und erlaubten Werten im Prompt-Text
     (Apotheke: die fünf Codes; Region: die 18 Namen; `urn:psc:`/`urn:as:`:
     Format plus Beispiel aus der ILF-Tabelle des Sektors). Leere Eingabe =
     bewusst leer, der Befund wird zur WARNING.
   - WARNINGs werden auf stderr ausgegeben, der Request wird gesendet.
   - `--no-prompt`: jeder verbleibende ERROR führt zum Abbruch mit
     Zusammenfassung:
     ```
     Folgende Attribute fehlen oder sind ungültig (Sektor: Apotheken SMC-B, gemILF_Pflege_VZD Tabelle 4):
       countryCode: Pflicht, da eine Adresse angegeben ist. Erlaubt: ISO-3166-1 alpha-2, z. B. DE
       specialization: obligatorisch. Erlaubt: 10 Offizin-Apotheke, 20 Krankenhausversorgende Apotheke, 30 Heimversorgende Apotheke, 40 Versandapotheke, 50 Sterilherstellung
     Setze sie mit -s countryCode=DE -s specialization=10, lasse sie mit -s specialization= bewusst leer, oder starte ohne --no-prompt.
     ```
7. Vorschau des Basiseintrags (YAML, wie `template base`) auf stderr, dann
   `POST /DirectoryEntries` mit `CreateDirectoryEntry(base, userCertificates)`.
8. Ergebnis lesen und wie heute ausgeben (`--yaml`/`--json`).

### Lokale Vorprüfung in `add-cert`

Vor jedem Upload: ENC-Typ, Admission vorhanden, TelematikID gleich dem
gefundenen Basiseintrag (heute implizit, weil per TelematikID gesucht wird),
entryType des Zertifikats gleich `entryType` des Basiseintrags, sofern dort
gesetzt. Jeder Verstoß ist ein `CliktError` mit Erklärung, z. B.
`AUT-Zertifikat erkannt (C.HCI.AUT, 1.2.276.0.76.4.77). Der VZD akzeptiert nur
C.HCI.ENC oder C.HP.ENC.` oder `Zertifikat ergibt entryType 3 (Öffentliche
Apotheke), Basiseintrag hat entryType 4. Korrigiere mit modify-base-attr -t …
-s entryType=3, solange kein Zertifikat im Eintrag ist.`

### `cert-info`

`CertificateInfo` bekommt zwei neue optionale Felder:

- `certificateType`: `C.HCI.ENC`, `C.HCI.AUT`, `C.HCI.OSIG`, `C.HP.ENC`,
  `C.HP.AUT`, `C.HP.QES`, `C.HP.SIG` oder `null`, wenn keine bekannte
  Typ-OID in den Certificate Policies steht.
- `entryType`: aus professionOIDs über das Mapping (bei mehreren OIDs der
  erste Treffer; sind die Treffer uneinheitlich, `null` plus Warnung im Log).

Beide werden in `CertificateDataDER.certificateInfo` berechnet, damit sie in
`list-cert`, `dump` und der Human-Ausgabe von `show` automatisch erscheinen.

### Fehlerhinweise bei Server-Fehlern

In `catching` (Cli.kt) bleibt die Struktur; `AdminResponseException.details`
wird um `explain(details)` ergänzt, das bekannte Textmuster erkennt und einen
zweiten Absatz anhängt:

- `entryType mismatch` → Mapping-Zeile für die professionOID(s) des
  gesendeten Zertifikats, wenn bekannt; sonst die Tabelle kompakt.
- `HealthcareSpeciality mismatch` → erlaubte Formate je Sektor und für
  Apotheken die PharmacyTypeLDAPCS-Codes mit dem Hinweis, dass die
  FHIR-Codes (`offizin-apotheke`) im LDAP-VZD nicht gelten.
- `KOMLECertPolicies` → erkannter Zertifikatstyp und erwarteter Typ.
- `stateOrProvinceName` (422 mit attributeName) → Region-Werte.

Die Mustererkennung ist bewusst tolerant (Teilstring, case-insensitive), weil
die Texte Server-Implementierungsdetails sind.

### Neue Bausteine in `directory-lib`

- `pki/CertificateType.kt`: Enum mit Typ-OIDs aus gemSpec_OID Tab_PKI_405
  und `fun X509Certificate.certificateType(): CertificateType?`, plus
  `isEncryptionCertificate`.
- `pki/CertificateSource.kt`: `loadCertificates(path, password?)` →
  `List<CertificateDataDER>` für DER/PEM/PKCS#12; Fehler als
  `DirectoryException` mit lesbarer Nachricht.
- `admin/EntryTypeMapping.kt` + Ressource `mappings/EntryType.mapping.json`
  (OID → entryType, Tabelle oben). Anzeigetexte kommen aus den vorhandenen
  CodeSystems `OrganizationProfessionOID` und `PractitionerProfessionOID`.
- `admin/SectorMapping.kt` + Ressource `mappings/Sector.mapping.json`:
  pro ILF-Tabelle Regex, Name, Tabellennummer, Kartentyp (HBA/SMC-B),
  Pflichtattribute, specialization-Format. Inhalt ist die Matrix oben, 1:1
  aus ILF 1.5.18 übernommen, damit sie bei ILF-Änderungen ohne Code-Änderung
  gepflegt werden kann.
- `admin/BaseEntryFromCertificate.kt`: `BaseDirectoryEntry.Companion
  .fromCertificate(cert)`. Die private Funktion `baseEntryFromCert` in
  `pers/ExtractCommand.kt` wird darauf umgestellt.
- `admin/BaseEntryPreflight.kt`: Regeln V001–V007, V009 (über
  SectorMapping), V015 als reine Funktionen über `BaseDirectoryEntry` +
  Sektor + Menge der bewusst leeren Attribute, Ergebnis
  `List<PreflightFinding>` mit `FindingSeverity` (bestehendes Enum in
  `validation/Finding.kt` wiederverwenden).
- `fhir/SimpleCodeSystem.kt`: `PharmacyTypeLDAPCS` und `Region` laden;
  `PharmacySpecializationSpecialCase` auf `PharmacyTypeLDAPCS` umstellen.

Neue Bausteine in `vzd-cli`:

- `cli/admin/CertificateInputOptions.kt`: Clikt `OptionGroup` mit `--passin`
  und der Logik "Passwort beschaffen" (pass/env/file/prompt), geteilt von
  `add-base`, `add-cert`, `cert-info`.
- `AddBaseCommand`: neuer Zweig `--from-cert`; bestehender Pfad bleibt
  unverändert (Datei/STDIN/`-s telematikID -s entryType`).
- `AddCertCommand`: Zertifikatsquelle + Vorprüfung.
- `Cli.kt`: Fehlererklärung.

### Tests

Unit (directory-lib, ohne Netz):
- `CertificateType` für die drei Testzertifikate (RSA-ENC, ECC-ENC, HP-ENC)
  und ein synthetisches AUT-Zertifikat.
- `EntryTypeMapping`: `.54` → 3, `.30` → 1, unbekannte OID → null; HBA-Cert
  mit zwei Professions → 1.
- `SectorMapping`: `3-SMC-B-Testkarte-…2…` → Apotheken; `1-SMC-B-Testkarte-…`
  → BÄK (HBA-Tabelle, weil Regex `^1-1*`); `3-2-Test01` → kein Sektor;
  `5-…` → DKTIG.
- `fromCertificate`: SMC-B-Testzertifikat ergibt displayName
  `Praxis Lilo Gräfin de BoerTEST-ONLY`, Adresse `Am Markt 1`, `25832`,
  `Tönning`, `DE`; HBA-Testzertifikat ergibt `Fit, Heinrich`, keine Adresse,
  kein countryCode.
- `BaseEntryPreflight`: Adresse ohne countryCode → ERROR; PLZ 4-stellig bei
  DE → ERROR; stateOrProvinceName `Schleswig Holstein` → ERROR mit Liste;
  Apotheke mit `specialization=offizin-apotheke` → ERROR (LDAP erwartet
  `10`); Apotheke ohne specialization → ERROR, mit bewusst leer → WARNING;
  Arztpraxis `1-20…` mit `urn:psc:1.3.6.1.4.1.19376.3.276.1.5.4:ALLG` → ok,
  ohne domainID → ERROR; HBA mit `urn:as:1.2.276.0.76.5.514:011001` → ok;
  HBA mit `urn:as:1.2.276.0.76.5.514:999999` → WARNING.
- `CertificateSource`: PEM, DER, PKCS#12 mit einem ENC + einem Nicht-ENC
  (ENC wird gewählt), PKCS#12 mit zwei TelematikIDs (Fehler), falsches
  Passwort (Fehlermeldung nennt die Datei).

Der Zusammenbau des Requests (Zertifikate → Basiseintrag → `--set` →
Preflight → `CreateDirectoryEntry`) liegt als reine Funktion in
`directory-lib` und wird dort getestet; `Client` wird nicht gemockt und
nicht refaktoriert:
- SMC-B-ENC (DER) → `CreateDirectoryEntry` mit Zertifikat, `entryType` null,
  `professionOID` null, Adresse aus dem Subject.

CLI (vzd-cli, Clikt `test()` ohne Server, nur Abbruchpfade vor dem Request):
- `--no-prompt` mit Adresse im Zertifikat und `-s countryCode=` →
  Exit ≠ 0, Meldung enthält `-s countryCode=`.
- `-s entryType=4` bei `.54` → UsageError mit `entryType 3`.
- `add-cert aut.der` → Fehler mit `C.HCI.AUT`.

Integrationstests gegen RU (bestehendes Muster `TestCreate` mit
`domainID = Testklasse`) nur, wenn Credentials vorhanden; sie werden nicht in
den Standard-Lauf aufgenommen.

### Dokumentation

- README `add-base`: Abschnitt `--from-cert` mit Apotheken-Beispiel
  (`.p12`, `--passin env:SMCB_PIN`, Ergebnis), Hinweis, dass `entryType` und
  `professionOID` vom VZD gesetzt werden; altes Beispiel `entryType=4` durch
  `entryType=3` mit Erklärung ersetzen; specialization-Formate je Sektor
  kurz tabellarisch mit dem Hinweis auf LDAP- vs. FHIR-Codes bei Apotheken.
- README `add-cert`: Vorprüfung erwähnen, `.p12` erlaubt.
- README `fhir <env> login`: der Befehl existiert im Code nicht mehr (seit
  3.3.0 `vzd-cli login`); Abschnitt entfernen bzw. auf `vzd-cli login`
  umstellen.
- ReleaseNotes.adoc unter "Unreleased" ergänzen.

## Entscheidungen aus dem Builder-Check

Vom Autor des Dokuments entschieden, nicht vom Auftraggeber bestätigt:

1. Pflichtfelder kommen sektorabhängig aus den ILF-Tabellen (Matrix oben)
   und sind ERROR-Befunde. Interaktiv wird jeder abgefragt; eine leere
   Antwort oder `-s ATTR=` heißt "bewusst leer" und macht den Befund zur
   WARNING. Mit `--no-prompt` ohne diese Markierung bricht die CLI ab. Damit
   bleibt die ILF-Aussage "Apotheke: specialization obligatorisch, LEER
   zulässig" bedienbar, ohne dass die CLI still etwas weglässt.
2. Befunde haben die Schwere ERROR/WARNING wie oben. Wertemengen-Treffer
   (`urn:psc:`/`urn:as:` gegen lokale ValueSet-Kopien) sind nur WARNING,
   Format- und Region-Verstöße sind ERROR, weil der Server sie laut ILF
   sicher ablehnt.
3. Abgelaufene oder noch nicht gültige ENC-Kandidaten aus einer PKCS#12
   werden mit Warnung verworfen; der Basiseintrag kommt aus dem neuesten
   Kandidaten, die Zertifikate gehen aufsteigend nach notBefore in den
   Request.
4. `countryCode` wird nur zusammen mit einer weiteren Adresskomponente aus
   dem Zertifikat übernommen (HBA bekommt damit keine Adresse).
5. Request-Zusammenbau wird als reine Funktion in `directory-lib` getestet;
   kein Mock und kein Refactoring von `Client`.
6. Sektorerkennung folgt dem Server: TelematikID-Regex aus dem ILF, nicht
   professionOID. Die professionOID bestimmt nur den entryType.

## Offen für den Builder

- Exakte Wortwahl der Fehlermeldungen (Repo mischt heute Deutsch und
  Englisch; neue Meldungen auf Englisch wie die bestehenden CLI-Fehler,
  Attributnamen und ILF-Begriffe unverändert).
- Ob die Vorschau des Basiseintrags vor dem Upload immer oder nur mit
  `--verbose` gezeigt wird (Vorschlag: immer auf stderr, eine Zeile pro
  Attribut).
- Reihenfolge der Prompts (Vorschlag: displayName, Adresse in Lesereihenfolge,
  countryCode, stateOrProvinceName, domainID, organization, specialization).

## Referenz

- Issue-Text: "Eintrag direkt aus Zertifikat anlegen (add-base --from-cert)
  und klarere Fehlermeldungen bei add-cert", Reproduktion mit
  `3-2-Test01`.
- gemSpec_VZD: https://gemspec.gematik.de/docs/gemSpec/gemSpec_VZD/latest/
- gemILF_Pflege_VZD 1.5.18: https://github.com/gematik/api-vzd/blob/main/docs/gemILF_Pflege_VZD.adoc
- LDAP2FHIR_Sync: https://github.com/gematik/api-vzd/blob/main/docs/LDAP2FHIR_Sync.adoc
- LDAP_Attribute: https://github.com/gematik/api-vzd/blob/main/docs/LDAP_Attribute.adoc
- DirectoryAdministration.yaml: https://github.com/gematik/api-vzd/blob/main/src/openapi/DirectoryAdministration.yaml
- gemSpec_OID Tab_PKI_405: https://gemspec.gematik.de/docs/gemSpec/gemSpec_OID/latest/
