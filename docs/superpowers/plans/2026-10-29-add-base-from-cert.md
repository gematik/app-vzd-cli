# add-base --from-cert Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `vzd-cli admin <env> add-base --from-cert FILE` creates a VZD base entry plus certificate in one request, derives everything from the ENC certificate, prompts for missing sector-mandatory values; `add-cert` and `cert-info` gain local certificate checks and PKCS#12 input; server errors get explanations.

**Architecture:** All domain logic (certificate type, entryType mapping, sector rules, base-entry derivation, preflight validation, request assembly, error hints) lives in `directory-lib` as pure functions over `BaseDirectoryEntry`/`CertificateDataDER`, tested without network. `vzd-cli` only adds Clikt options, prompting and output. Spec tables are shipped as JSON resources under `mappings/` so they can be updated without code changes.

**Tech Stack:** Kotlin 1.9.22 / JVM 11 toolchain, Gradle 8.7, Clikt 3.5.1, BouncyCastle 1.70 (bcprov + bcpkix), kotlinx.serialization + yamlkt, kotest 5.5.4 (FeatureSpec).

**Spec:** `docs/superpowers/specs/2026-10-29-add-base-from-cert-design.md`

## Global Constraints

- No new library dependencies. PKCS#12 via `java.security.KeyStore.getInstance("PKCS12")`, policy OIDs via BouncyCastle `X509CertificateHolder`.
- Never send `entryType`, `professionOID`, `cn`, `sn`, `givenName`, `personalEntry`, `holder` in the `--from-cert` path. Only `telematikID`, `displayName`, address fields, and `--set` overrides.
- `countryCode` is taken from subject `C` only when at least one of street/postalCode/locality/state is present.
- ENC detection is by certificate policy OID `1.2.276.0.76.4.76` (C.HCI.ENC) or `1.2.276.0.76.4.74` (C.HP.ENC) and absence of `digitalSignature` in keyUsage.
- Sector detection follows the server: TelematikID regex from gemILF_Pflege_VZD Tabelle_TID_Mapping, first match wins, table order as in the ILF. professionOID only determines entryType.
- Unit tests make no network calls. Test certificates (all DER encoded, all **expired**, so any validity-dependent test passes an explicit `now`):
  - `vzd-cli/testdata/1-SMC-B-Testkarte-883110000117035-787024799120720.der`: RSA ENC, keyUsage keyEncipherment+dataEncipherment, policy .76, professionOID `1.2.276.0.76.4.50` (Betriebsstätte Arzt → entryType 3), telematikID `1-SMC-B-Testkarte-883110000117035`, valid 2020-01-27 to 2024-12-11, subject `C=DE, L=Tönning, postalCode=25832, street=Am Markt 1, O=Praxis Lilo Gräfin de BoerNOT-VALID, SN=Boer, GN=Lilo, title=Prof. Dr., CN=Praxis Lilo Gräfin de BoerTEST-ONLY`
  - `vzd-cli/testdata/1-SMC-B-Testkarte-883110000117035-463705411240438.der`: ECC ENC of the same card, keyUsage keyAgreement only, same validity window
  - `vzd-cli/testdata/ecc-ID007-c.hp.enc-bptk-beide_professions.crt`: HBA ENC, policy .74, professionOIDs `1.2.276.0.76.4.46` and `.47` (→ entryType 1), telematikID `4-11a4103-001`, valid 2017-09-28 to 2022-09-28, subject `C=DE, SN=Fit, GN=Heinrich, serialNumber=123456789013, CN=Heinrich Fit TEST-ONLY`
- Shared test helpers live in a `testFixtures` source set of `directory-lib` (Gradle `java-test-fixtures` plugin, no external dependency) so both modules use the same `TestCertificates` object. `TestCertificates.testdata` resolves to the first existing of `testdata` and `../vzd-cli/testdata`.
- New `CertificateInfo` fields default to `null` so existing `toYamlNoDefaults` output does not change when the value is unknown.
- Every source file starts with the EUPL header copied from an existing file in the same module. Comments only for non-obvious intent. ktlint style (run `./gradlew ktlintFormat` if the task exists, otherwise match surrounding code).
- New CLI messages in English like existing CLI errors; attribute names and ILF terms verbatim.
- Commits: plain messages, no AI attribution lines, no Co-Authored-By.
- Build commands: `./gradlew :directory-lib:test --tests '<FQCN>'` and `./gradlew :vzd-cli:test --tests '<FQCN>'`. Tests in `vzd-cli` that need `TEST_ACCESS_TOKEN` are pre-existing and may fail without it; only the test classes named in each task must pass.

## Review Focus

1. A PKCS#12 that contains only an AUT certificate must fail with a candidate list naming `C.HCI.AUT`, never upload (Task 3 + Task 9 tests).
2. A certificate whose subject has `C=DE` but no street/PLZ/city must not produce `countryCode` (Task 7 test).
3. A TelematikID matching no ILF sector (`3-2-Test01`) must not abort; only generic rules apply plus a WARNING (Task 8 test).
4. `-s specialization=` (empty) must suppress the mandatory finding and send no `specialization`, not `[""]` (Task 6 + Task 8 tests).
5. `-s entryType=4` with a `.54` certificate must fail before any request with the text `entryType 3` (Task 9 test).

---

### Task 0: Branch and toolchain

**Files:** none

- [ ] **Step 1: Create the feature branch**

```bash
git checkout -b feature/add-base-from-cert
git add docs/superpowers
git commit -m "Add design and plan for add-base --from-cert"
```

- [ ] **Step 2: Make a JDK 11 available to Gradle**

The build uses `jvmToolchain(11)`; only JDK 21 is installed (sdkman). Install one:

```bash
sdk install java 11.0.26-tem
```

Answer "n" to "set as default". Gradle auto-detects sdkman JDKs.

- [ ] **Step 3: Verify the build compiles**

Run: `./gradlew :directory-lib:compileTestKotlin :vzd-cli:compileTestKotlin -q`
Expected: no output, exit 0.

---

### Task 1: CertificateType detection

**Files:**
- Modify: `directory-lib/build.gradle.kts` (add plugin `` `java-test-fixtures` ``; `testFixturesImplementation("org.bouncycastle:bcprov-jdk15on:$bcVersion")`, `testFixturesImplementation("org.bouncycastle:bcpkix-jdk15on:$bcVersion")`, `testFixturesImplementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")`)
- Modify: `vzd-cli/build.gradle.kts` (add `testImplementation(testFixtures(project(":directory-lib")))`)
- Create: `directory-lib/src/main/kotlin/de/gematik/ti/directory/pki/CertificateType.kt`
- Create: `directory-lib/src/testFixtures/kotlin/de/gematik/ti/directory/pki/TestCertificates.kt` (shared test helpers)
- Test: `directory-lib/src/test/kotlin/de/gematik/ti/directory/pki/TestCertificateType.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class CertificateType(val oid: String, val label: String) {
      C_HCI_ENC("1.2.276.0.76.4.76", "C.HCI.ENC"), C_HCI_AUT("1.2.276.0.76.4.77", "C.HCI.AUT"),
      C_HCI_OSIG("1.2.276.0.76.4.78", "C.HCI.OSIG"), C_HP_ENC("1.2.276.0.76.4.74", "C.HP.ENC"),
      C_HP_AUT("1.2.276.0.76.4.75", "C.HP.AUT"), C_HP_QES("1.2.276.0.76.4.72", "C.HP.QES"),
      C_HP_SIG("1.2.276.0.76.4.73", "C.HP.SIG");
      val isEncryption: Boolean   // true for C_HCI_ENC, C_HP_ENC
      companion object { fun fromOid(oid: String): CertificateType? }
  }
  fun X509Certificate.policyOids(): List<String>
  fun X509Certificate.certificateType(): CertificateType?   // first policy OID that maps
  fun X509Certificate.isEncryptionCertificate(): Boolean    // certificateType()?.isEncryption == true && keyUsage digitalSignature (bit 0) not set
  fun X509Certificate.hasAdmission(): Boolean               // extension 1.3.36.8.3.3 present
  ```
- Test helpers (used by Tasks 3, 7, 8, 9):
  ```kotlin
  object TestCertificates {
      val testdata: Path                         // first existing of Paths.get("testdata"), Paths.get("../vzd-cli/testdata")
      val smcbRsaEnc: Path; val smcbEccEnc: Path; val hbaEnc: Path   // the three files above
      val smcbValidAt: Instant = Instant.parse("2023-06-01T00:00:00Z")
      fun load(path: Path): CertificateDataDER
      fun synthetic(
          subject: String,                       // e.g. "CN=Test AUT,C=DE"
          policyOid: String,                     // e.g. "1.2.276.0.76.4.77"
          keyUsage: Int,                         // org.bouncycastle.asn1.x509.KeyUsage bits
          telematikID: String?,                  // null → no admission extension
          professionOid: String = "1.2.276.0.76.4.54",
          notBefore: Instant = Clock.System.now() - 1.days, notAfter: Instant = Clock.System.now() + 1.days,
      ): Pair<X509Certificate, PrivateKey>       // self-signed EC P-256 via JcaX509v3CertificateBuilder, policies + keyUsage + ISIS-MTT admission (AdmissionSyntax/Admissions/ProfessionInfo)
      fun toDer(cert: X509Certificate): CertificateDataDER
      fun writeDer(cert: X509Certificate, dir: Path, name: String): Path
      fun pkcs12(password: CharArray, keyEntries: List<Pair<X509Certificate, PrivateKey>>, trusted: List<X509Certificate>): ByteArray
      // ready-made: a currently valid pharmacy ENC with address, telematikID 3-01.2.2023000, professionOID .54,
      // subject "CN=Linden Apotheke TEST,STREET=Hauptstr. 1,postalCode=10117,L=Berlin,C=DE"
      fun pharmacyEnc(): X509Certificate
      fun pharmacyEncWithoutAddress(): X509Certificate   // subject "CN=Linden Apotheke TEST,C=DE"
      fun smcbAut(): X509Certificate                     // policy .77, keyUsage digitalSignature, telematikID 3-01.2.2023000
  }
  ```

- [ ] **Step 1: Write the failing tests**

```kotlin
class TestCertificateType : FeatureSpec({
    feature("Certificate type from policy OIDs") {
        scenario("SMC-B RSA ENC") {
            val cert = TestCertificates.load(TestCertificates.smcbRsaEnc).certificate
            cert.certificateType() shouldBe CertificateType.C_HCI_ENC
            cert.isEncryptionCertificate() shouldBe true
            cert.hasAdmission() shouldBe true
        }
        scenario("SMC-B ECC ENC with keyAgreement only") {
            TestCertificates.load(TestCertificates.smcbEccEnc).certificate.isEncryptionCertificate() shouldBe true
        }
        scenario("HBA ENC") {
            TestCertificates.load(TestCertificates.hbaEnc).certificate.certificateType() shouldBe CertificateType.C_HP_ENC
        }
        scenario("Synthetic AUT is not an encryption certificate") {
            val (cert, _) = TestCertificates.synthetic("CN=AUT", "1.2.276.0.76.4.77", KeyUsage.digitalSignature, "3-SMC-B-Test")
            cert.certificateType() shouldBe CertificateType.C_HCI_AUT
            cert.isEncryptionCertificate() shouldBe false
        }
        scenario("Unknown policy") {
            val (cert, _) = TestCertificates.synthetic("CN=X", "1.2.3.4", KeyUsage.keyAgreement, null)
            cert.certificateType() shouldBe null
            cert.hasAdmission() shouldBe false
        }
    }
})
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :directory-lib:test --tests 'de.gematik.ti.directory.pki.TestCertificateType'`
Expected: compilation failure (unresolved `certificateType`).

- [ ] **Step 3: Implement the Gradle test-fixtures wiring, `TestCertificates` and `CertificateType.kt`**

Policy OIDs: `CertificatePolicies.fromExtensions(X509CertificateHolder(encoded).extensions)?.policyInformation?.map { it.policyIdentifier.id }`. Admission check: `getExtensionValue(ISISMTTObjectIdentifiers.id_isismtt_at_admission.id) != null`.

- [ ] **Step 4: Run tests to verify they pass**

Run: same as Step 2. Expected: all 5 scenarios PASS.

- [ ] **Step 5: Commit**

```bash
git add directory-lib/build.gradle.kts vzd-cli/build.gradle.kts directory-lib/src
git commit -m "Detect gematik certificate type from policy OIDs"
```

---

### Task 2: EntryType mapping and new CertificateInfo fields

**Files:**
- Create: `directory-lib/src/main/resources/mappings/EntryType.mapping.json`
- Create: `directory-lib/src/main/kotlin/de/gematik/ti/directory/admin/EntryTypeMapping.kt`
- Modify: `directory-lib/src/main/kotlin/de/gematik/ti/directory/pki/Certificates.kt` (`CertificateInfo`, construction in `certificateInfo` lazy)
- Test: `directory-lib/src/test/kotlin/de/gematik/ti/directory/admin/TestEntryTypeMapping.kt`

**Interfaces:**
- Produces:
  ```kotlin
  @Serializable data class EntryTypeMappingEntry(val entryType: Int, val name: String, val professionOIDs: List<String>)
  object EntryTypeMapping {
      val entries: List<EntryTypeMappingEntry>          // loaded from /mappings/EntryType.mapping.json
      fun entryTypeOf(professionOID: String): Int?
      fun entryTypeOf(professionOIDs: List<String>): Int? // null if no OID known or known OIDs disagree
      fun nameOf(entryType: Int): String?
      fun describe(professionOID: String): String        // "1.2.276.0.76.4.54 (Öffentliche Apotheke)" using OrganizationProfessionOID/PractitionerProfessionOID display, OID alone if unknown
  }
  ```
- `CertificateInfo` gains `val certificateType: String? = null` and `val entryType: String? = null` as the **last** two constructor parameters (keeps positional construction working).

- [ ] **Step 1: Write the resource**

`EntryType.mapping.json`, exact content from the spec table (all OIDs prefixed `1.2.276.0.76.4.`):

```json
{ "mapping": [
  {"entryType": 1, "name": "Berufsgruppe", "professionOIDs": ["30","31","32","33","34","35","36","37","38","39","40","41","42","43","44","45","46","47","48","178","232","233","234","235","236","237","238","239","240","241","274","275","276","277","305","308","312","313","315","316","318","319"]},
  {"entryType": 2, "name": "Versicherte/-r", "professionOIDs": ["49"]},
  {"entryType": 3, "name": "Leistungserbringerinstitution", "professionOIDs": ["50","51","52","53","54","55","56","57","245","246","247","248","249","250","251","252","253","254","255","256","257","278","279","280","281","304","306","311","314","317"]},
  {"entryType": 4, "name": "Organisation", "professionOIDs": ["58","187","190","210","223","224","225","226","227","228","229","230","231","242","243","244","262","263","264","265","266","267","268","269","270","271","284","285","292"]},
  {"entryType": 5, "name": "Krankenkasse", "professionOIDs": ["59"]},
  {"entryType": 7, "name": "KIM", "professionOIDs": ["286"]},
  {"entryType": 8, "name": "TIM", "professionOIDs": ["295"]},
  {"entryType": 9, "name": "DiGA", "professionOIDs": ["282"]},
  {"entryType": 10, "name": "Ombudsstelle", "professionOIDs": ["303"]}
]}
```

Store the suffixes as above and prepend `1.2.276.0.76.4.` in the loader (`professionOIDs` in the data class hold full OIDs).

- [ ] **Step 2: Write the failing tests**

```kotlin
class TestEntryTypeMapping : FeatureSpec({
    feature("professionOID to entryType") {
        scenario("Öffentliche Apotheke is 3") { EntryTypeMapping.entryTypeOf("1.2.276.0.76.4.54") shouldBe 3 }
        scenario("Arzt is 1") { EntryTypeMapping.entryTypeOf("1.2.276.0.76.4.30") shouldBe 1 }
        scenario("Unknown OID") { EntryTypeMapping.entryTypeOf("1.2.276.0.76.4.9999") shouldBe null }
        scenario("Two HBA professions agree") { EntryTypeMapping.entryTypeOf(listOf("1.2.276.0.76.4.45", "1.2.276.0.76.4.46")) shouldBe 1 }
        scenario("Disagreeing OIDs give null") { EntryTypeMapping.entryTypeOf(listOf("1.2.276.0.76.4.30", "1.2.276.0.76.4.54")) shouldBe null }
        scenario("describe uses CodeSystem display") { EntryTypeMapping.describe("1.2.276.0.76.4.54") shouldBe "1.2.276.0.76.4.54 (Öffentliche Apotheke)" }
    }
    feature("CertificateInfo") {
        scenario("SMC-B ENC exposes type and entryType") {
            val info = TestCertificates.load(TestCertificates.smcbRsaEnc).certificateInfo
            info.certificateType shouldBe "C.HCI.ENC"
            info.entryType shouldBe "3"   // professionOID 1.2.276.0.76.4.50 Betriebsstätte Arzt
        }
        scenario("HBA ENC with two professions") {
            val info = TestCertificates.load(TestCertificates.hbaEnc).certificateInfo
            info.certificateType shouldBe "C.HP.ENC"
            info.entryType shouldBe "1"   // .46 and .47 both map to 1
        }
    }
})
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew :directory-lib:test --tests 'de.gematik.ti.directory.admin.TestEntryTypeMapping'`
Expected: compilation failure.

- [ ] **Step 4: Implement `EntryTypeMapping` and extend `CertificateInfo`**

In `Certificates.kt` compute `certificate.certificateType()?.label` and `EntryTypeMapping.entryTypeOf(admission.professionOids)?.toString()` when building `CertificateInfo`. Load JSON with the `fhir` package's `json` (`internal val json` in `fhir/Common.kt`, visible module-wide).

- [ ] **Step 5: Run tests to verify they pass**

Run: same as Step 3 plus `./gradlew :directory-lib:test --tests 'de.gematik.ti.directory.pki.*'`. Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add directory-lib/src
git commit -m "Map professionOID to entryType and expose certificate type in CertificateInfo"
```

---

### Task 3: CertificateSource (DER, PEM, PKCS#12)

**Files:**
- Create: `directory-lib/src/main/kotlin/de/gematik/ti/directory/pki/CertificateSource.kt`
- Test: `directory-lib/src/test/kotlin/de/gematik/ti/directory/pki/TestCertificateSource.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class CertificateFileFormat { DER, PEM, PKCS12 }
  fun detectCertificateFileFormat(bytes: ByteArray): CertificateFileFormat
      // PEM if text starts with "-----BEGIN"; PKCS12 if ASN.1 SEQUENCE whose first element is INTEGER (PFX version 3); else DER
  class CertificatePasswordRequired(val file: Path) : DirectoryException("Password required for PKCS#12 file $file")
  fun loadCertificates(file: Path, password: () -> CharArray?): List<CertificateDataDER>
      // DER/PEM: exactly one certificate (PEM may hold several → all).
      // PKCS12: all certificates from key entries (incl. chain) and trusted entries, deduplicated by encoded bytes, CA certificates (BasicConstraints cA=true) dropped.
      // password() is called only for PKCS12; null → CertificatePasswordRequired. Wrong password → DirectoryException("Cannot open PKCS#12 file $file: wrong password or corrupt file").
  ```

- [ ] **Step 1: Write the failing tests**

```kotlin
class TestCertificateSource : FeatureSpec({
    val tmp = tempdir()  // kotest
    feature("Load certificates") {
        scenario("DER") {
            loadCertificates(TestCertificates.smcbRsaEnc) { null }.size shouldBe 1
        }
        scenario("PEM") {
            val b64 = Base64.toBase64String(TestCertificates.smcbRsaEnc.readBytes()).chunked(64).joinToString("\n")
            val pem = tmp.resolve("c.pem"); pem.writeText("-----BEGIN CERTIFICATE-----\n$b64\n-----END CERTIFICATE-----\n")
            loadCertificates(pem) { null }.first().certificateInfo.admissionStatement.registrationNumber shouldBe "1-SMC-B-Testkarte-883110000117035"
        }
        scenario("PKCS#12 with ENC key entry and AUT trusted cert returns both end-entity certs") {
            val enc = TestCertificates.synthetic("CN=ENC", "1.2.276.0.76.4.76", KeyUsage.keyAgreement, "3-SMC-B-Test")
            val aut = TestCertificates.synthetic("CN=AUT", "1.2.276.0.76.4.77", KeyUsage.digitalSignature, "3-SMC-B-Test")
            val p12 = tmp.resolve("card.p12"); p12.writeBytes(TestCertificates.pkcs12("secret".toCharArray(), listOf(enc), listOf(aut.first)))
            val certs = loadCertificates(p12) { "secret".toCharArray() }
            certs.map { it.certificateInfo.subjectInfo.cn }.toSet() shouldBe setOf("ENC", "AUT")
        }
        scenario("PKCS#12 without password") {
            shouldThrow<CertificatePasswordRequired> { loadCertificates(p12) { null } }
        }
        scenario("PKCS#12 wrong password names the file") {
            shouldThrow<DirectoryException> { loadCertificates(p12) { "nope".toCharArray() } }.message shouldContain "card.p12"
        }
        scenario("Format detection") {
            detectCertificateFileFormat(p12.readBytes()) shouldBe CertificateFileFormat.PKCS12
            detectCertificateFileFormat(TestCertificates.smcbRsaEnc.readBytes()) shouldBe CertificateFileFormat.DER
        }
    }
})
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :directory-lib:test --tests 'de.gematik.ti.directory.pki.TestCertificateSource'`
Expected: compilation failure.

- [ ] **Step 3: Implement `CertificateSource.kt`**

PKCS12 via `KeyStore.getInstance("PKCS12")`, iterate `aliases()`, `getCertificateChain(alias)` and `getCertificate(alias)`. Wrong password surfaces as `IOException` with `UnrecoverableKeyException` cause; map to the message above.

- [ ] **Step 4: Run tests to verify they pass**

Run: same as Step 2. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add directory-lib/src
git commit -m "Load certificates from DER, PEM and PKCS#12 files"
```

---

### Task 4: PharmacyTypeLDAPCS and Region code systems

**Files:**
- Modify: `directory-lib/src/main/kotlin/de/gematik/ti/directory/fhir/SimpleCodeSystem.kt`
- Modify: `directory-lib/src/main/kotlin/de/gematik/ti/directory/elaborate/specialcases/PharmacySpecializationSpecialCase.kt`
- Test: `directory-lib/src/test/kotlin/de/gematik/ti/directory/fhir/TestSimpleFHIR.kt` (add scenarios)

**Interfaces:**
- Produces: `val PharmacyTypeLDAPCS: SimpleCodeSystem`, `val Region: SimpleCodeSystem` (top-level in `fhir` package, loaded from `CodeSystem-PharmacyTypeLDAPCS.json` and `CodeSystem-Region.json`).

- [ ] **Step 1: Write the failing tests**

```kotlin
scenario("PharmacyTypeLDAPCS codes") {
    PharmacyTypeLDAPCS.concept.map { it.code } shouldBe listOf("10", "20", "30", "40", "50")
    PharmacyTypeLDAPCS.resolveCode("10")?.display shouldBe "Offizin-Apotheke"
}
scenario("Region has 18 values") {
    Region.concept.size shouldBe 18
    Region.resolveCode("Schleswig-Holstein") shouldNotBe null
}
scenario("Pharmacy specialization resolves to LDAP code system") {
    val entry = DirectoryEntry(BaseDirectoryEntry(telematikID = "3-01.2.2023000", specialization = listOf("10")))
    entry.elaborate().base.specialization?.first()?.display shouldBe "Offizin-Apotheke"
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :directory-lib:test --tests 'de.gematik.ti.directory.fhir.TestSimpleFHIR'`
Expected: compilation failure / `Handverkauf` instead of `Offizin-Apotheke`.

- [ ] **Step 3: Implement**

Add the two `loadSimpleCodeSystem(...)` vals; switch `PharmacySpecializationSpecialCase` to `PharmacyTypeLDAPCS.resolveCode`. Check `TestKind` and `TestValidation` still pass.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :directory-lib:test`. Expected: PASS (network-free tests only; `TestClient`/`TestAuth` may be skipped or need env — report, do not fix).

- [ ] **Step 5: Commit**

```bash
git add directory-lib/src
git commit -m "Resolve pharmacy specialization codes via PharmacyTypeLDAPCS and load Region code system"
```

---

### Task 5: SectorMapping (ILF Tabelle_TID_Mapping)

**Files:**
- Create: `directory-lib/src/main/resources/mappings/Sector.mapping.json`
- Create: `directory-lib/src/main/kotlin/de/gematik/ti/directory/admin/SectorMapping.kt`
- Test: `directory-lib/src/test/kotlin/de/gematik/ti/directory/admin/TestSectorMapping.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class CardType { HBA, SMC_B }
  enum class SpecializationFormat { PHARMACY_LDAP, PSC, AS, NONE }
  @Serializable data class Sector(
      val pattern: String, val name: String, val table: Int, val cardType: CardType,
      val mandatory: List<String>,                 // attribute names beyond displayName
      val specializationFormat: SpecializationFormat,
  ) { val regex: Regex }                           // Regex(pattern, DOT_MATCHES_ALL)
  object SectorMapping {
      val sectors: List<Sector>
      fun sectorOf(telematikID: String): Sector?   // first match in file order
  }
  ```

- [ ] **Step 1: Write the resource**

`Sector.mapping.json` with `{"sectors": [...]}` in this exact order (regex verbatim from ILF 1.5.18 Tabelle_TID_Mapping, mandatory lists from the spec matrix):

| pattern | name | table | cardType | mandatory | specializationFormat |
|---|---|---|---|---|---|
| `^1-20.*` | Kassenärztliche Vereinigungen (SMC-B) | 2 | SMC_B | streetAddress, postalCode, localityName, specialization, domainID | PSC |
| `^2-2.*` | Kassenzahnärztliche Vereinigungen (SMC-B) | 3 | SMC_B | streetAddress, postalCode, localityName, specialization, domainID | PSC |
| `(^3-...2.*\|^9-2.54.*)` | Apotheken (SMC-B) | 4 | SMC_B | streetAddress, postalCode, localityName, specialization | PHARMACY_LDAP |
| `(^3-...3.*\|^9-1.(32\|33\|34).*)` | Apotheker, Pharmazieingenieure, Apothekerassistenten (HBA) | 5 | HBA | – | AS |
| `^5-.*` | DKTIG Krankenhaus (SMC-B) | 6 | SMC_B | streetAddress, postalCode, localityName, specialization | PSC |
| `^4-1.*` | Psychotherapeuten (HBA) | 7 | HBA | streetAddress, postalCode, localityName, specialization | AS |
| `^1-1*` | Ärzte-Kammern (HBA) | 8 | HBA | specialization | AS |
| `^2-0?1.*` | Zahnärzte-Kammern (HBA) | 9 | HBA | specialization | AS |
| `^8-01.*` | GKV-SV SMC-B KTR | 10 | SMC_B | streetAddress, postalCode, localityName, organization | NONE |
| `^8-(03\|04).*` | GKV-SV SM-B KTR ePA | 11 | SMC_B | streetAddress, postalCode, localityName, organization | NONE |
| `^9-2.(58\|187\|210\|223\|226\|227\|228\|224\|225\|229\|230\|231\|242\|243\|244\|245\|262\|263\|264\|265\|266\|267\|268\|269\|270\|271\|284\|285\|286\|295).*` | SMC-B-Org (gematik) | 12 | SMC_B | streetAddress, postalCode, localityName | PSC |
| `^10-67.(245\|246\|247\|278\|279\|280\|281\|311).*` | eGBR Nichtärztliche Institutionen (SMC-B) | 13 | SMC_B | streetAddress, postalCode, localityName, stateOrProvinceName, specialization, domainID | PSC |
| `^10-67.(232\|233\|234\|235\|236\|274\|275\|276\|277\|312).*` | eGBR Weitere Gesundheitsfachberufe (HBA) | 14 | HBA | specialization, domainID | AS |
| `^9-2.(50\|51\|34\|254\|255\|256).*` | Weitere ärztliche Institutionen (SMC-B) | 15 | SMC_B | streetAddress, postalCode, localityName, specialization | PSC |
| `^9-2.(59\|273\|303).*` | Weitere Kostenträger und PKV (SMC-B) | 16 | SMC_B | streetAddress, postalCode, localityName | NONE |
| `^9-2.282.*` | DiGA (SMC-B) | 17 | SMC_B | streetAddress, postalCode, localityName | NONE |
| `^11-2.*` | Handwerkskammern Nichtärztliche Institutionen (SMC-B) | 18 | SMC_B | streetAddress, postalCode, localityName, stateOrProvinceName, specialization | PSC |
| `^11-1.*` | Handwerkskammern Weitere Gesundheitsfachberufe (BA) | 19 | HBA | specialization | AS |
| `^9-2.292.*` | NCPeH (SMC-B) | 20 | SMC_B | – | NONE |

(In JSON the `\|` is a plain `|`.)

- [ ] **Step 2: Write the failing tests**

```kotlin
class TestSectorMapping : FeatureSpec({
    feature("Sector by TelematikID") {
        scenario("Pharmacy") { SectorMapping.sectorOf("3-01.2.2023000")?.table shouldBe 4 }
        scenario("EU mail-order pharmacy") { SectorMapping.sectorOf("9-2.54.123")?.table shouldBe 4 }
        scenario("KV practice wins over BÄK") { SectorMapping.sectorOf("1-2012345678")?.table shouldBe 2 }
        scenario("Test card falls into BÄK by ILF regex") { SectorMapping.sectorOf("1-SMC-B-Testkarte-883110000117035")?.table shouldBe 8 }
        scenario("Issue TelematikID has no sector") { SectorMapping.sectorOf("3-2-Test01") shouldBe null }
        scenario("Hospital") { SectorMapping.sectorOf("5-2-KHAUS-123")?.specializationFormat shouldBe SpecializationFormat.PSC }
        scenario("eGBR institution requires state") { SectorMapping.sectorOf("10-67.245.1")?.mandatory shouldContain "stateOrProvinceName" }
    }
})
```

- [ ] **Step 3: Run tests to verify they fail** — `./gradlew :directory-lib:test --tests 'de.gematik.ti.directory.admin.TestSectorMapping'`; expected compilation failure.

- [ ] **Step 4: Implement `SectorMapping.kt`** (loader pattern like `TelematikIDMappings`).

- [ ] **Step 5: Run tests to verify they pass.**

- [ ] **Step 6: Commit**

```bash
git add directory-lib/src
git commit -m "Add ILF sector mapping by TelematikID with mandatory attributes"
```

---

### Task 6: Move `setAttributes` into directory-lib with explicit-empty semantics

**Files:**
- Create: `directory-lib/src/main/kotlin/de/gematik/ti/directory/admin/BaseEntryAttributes.kt`
- Modify: `vzd-cli/src/main/kotlin/de/gematik/ti/directory/cli/admin/AddBaseCommand.kt` (remove local `setAttributes`, import from lib)
- Modify: `vzd-cli/src/main/kotlin/de/gematik/ti/directory/cli/admin/ModifyBaseAttrCommand.kt` (import)
- Test: `directory-lib/src/test/kotlin/de/gematik/ti/directory/admin/TestBaseEntryAttributes.kt`

**Interfaces:**
- Produces:
  ```kotlin
  fun setAttributes(baseDirectoryEntry: BaseDirectoryEntry, attrs: Map<String, String>)
      // identical behaviour to today's vzd-cli version; unknown attribute or unsupported type → DirectoryException
  fun applyOverrides(base: BaseDirectoryEntry, attrs: Map<String, String>): Set<String>
      // entries with blank value: property set to null (not written as "" / [""]) and name returned in the result set ("deliberately empty");
      // non-blank values delegate to setAttributes
  ```

- [ ] **Step 1: Write the failing tests**

```kotlin
scenario("String, Int, List and Boolean") {
    val b = BaseDirectoryEntry("x"); setAttributes(b, mapOf("displayName" to "A", "maxKOMLEadr" to "3", "specialization" to "10, 40", "active" to "false"))
    b.displayName shouldBe "A"; b.maxKOMLEadr shouldBe 3; b.specialization shouldBe listOf("10", "40"); b.active shouldBe false
}
scenario("Unknown attribute") { shouldThrow<DirectoryException> { setAttributes(BaseDirectoryEntry("x"), mapOf("nope" to "1")) } }
scenario("Blank value clears and is reported") {
    val b = BaseDirectoryEntry("x", specialization = listOf("10"), stateOrProvinceName = "Bayern")
    applyOverrides(b, mapOf("specialization" to "", "stateOrProvinceName" to "", "displayName" to "B")) shouldBe setOf("specialization", "stateOrProvinceName")
    b.specialization shouldBe null; b.stateOrProvinceName shouldBe null; b.displayName shouldBe "B"
}
```

- [ ] **Step 2: Run to verify failure** — `./gradlew :directory-lib:test --tests 'de.gematik.ti.directory.admin.TestBaseEntryAttributes'`.

- [ ] **Step 3: Implement** — move the function body from `AddBaseCommand.kt` (replace `UsageError` with `DirectoryException`), add `applyOverrides`, fix imports in the two CLI commands.

- [ ] **Step 4: Verify** — lib tests pass and `./gradlew :vzd-cli:compileKotlin -q` succeeds.

- [ ] **Step 5: Commit**

```bash
git add directory-lib/src vzd-cli/src
git commit -m "Move base entry attribute setter into directory-lib"
```

---

### Task 7: Base entry from certificate

**Files:**
- Modify: `directory-lib/src/main/kotlin/de/gematik/ti/directory/admin/Model.kt` (add `companion object` to `BaseDirectoryEntry`)
- Create: `directory-lib/src/main/kotlin/de/gematik/ti/directory/admin/BaseEntryFromCertificate.kt`
- Modify: `vzd-cli/src/main/kotlin/de/gematik/ti/directory/cli/pers/ExtractCommand.kt:125-137` (replace `baseEntryFromCert` body with the lib function)
- Test: `directory-lib/src/test/kotlin/de/gematik/ti/directory/admin/TestBaseEntryFromCertificate.kt`

**Interfaces:**
- Produces: `fun BaseDirectoryEntry.Companion.fromCertificate(cert: CertificateDataDER): BaseDirectoryEntry`
  - `telematikID` = registrationNumber; `entryType` = null (not set)
  - `displayName` = `"$sn, $givenName"` when `EntryTypeMapping.entryTypeOf(professionOids) == 1` and both present, else subject CN
  - street/postalCode/localityName/stateOrProvinceName from `subjectInfo`
  - `countryCode` from subject `C` only if any of the four above is non-null

- [ ] **Step 1: Write the failing tests**

```kotlin
scenario("SMC-B") {
    val b = BaseDirectoryEntry.fromCertificate(TestCertificates.load(TestCertificates.smcbRsaEnc))
    b.telematikID shouldBe "1-SMC-B-Testkarte-883110000117035"
    b.displayName shouldBe "Praxis Lilo Gräfin de BoerTEST-ONLY"
    b.streetAddress shouldBe "Am Markt 1"; b.postalCode shouldBe "25832"; b.localityName shouldBe "Tönning"; b.countryCode shouldBe "DE"
    b.entryType shouldBe null; b.professionOID shouldBe null; b.cn shouldBe null; b.sn shouldBe null
}
scenario("HBA") {
    val b = BaseDirectoryEntry.fromCertificate(TestCertificates.load(TestCertificates.hbaEnc))
    b.displayName shouldBe "Fit, Heinrich"
    b.streetAddress shouldBe null; b.countryCode shouldBe null
}
```

- [ ] **Step 2: Run to verify failure.** `./gradlew :directory-lib:test --tests 'de.gematik.ti.directory.admin.TestBaseEntryFromCertificate'`

- [ ] **Step 3: Implement**, then make `ExtractCommand.baseEntryFromCert` return `BaseDirectoryEntry.fromCertificate(cert)` (the hard-coded `entryType = 1` goes away).

- [ ] **Step 4: Verify** — test passes; `./gradlew :vzd-cli:compileKotlin -q`.

- [ ] **Step 5: Commit**

```bash
git add directory-lib/src vzd-cli/src
git commit -m "Derive base directory entry from ENC certificate subject"
```

---

### Task 8: BaseEntryPreflight

**Files:**
- Create: `directory-lib/src/main/kotlin/de/gematik/ti/directory/admin/BaseEntryPreflight.kt`
- Test: `directory-lib/src/test/kotlin/de/gematik/ti/directory/admin/TestBaseEntryPreflight.kt`

**Interfaces:**
- Consumes: `SectorMapping`, `Region`, `PharmacyTypeLDAPCS`, `HealthcareServiceTypeVS`, `PractitionerQualificationVS`, `PractitionerSpecializationRegex`, `OrganisationSpecializationRegex` (elaborate package), `FindingSeverity`.
- Produces:
  ```kotlin
  data class PreflightFinding(
      val attribute: String, val severity: FindingSeverity, val message: String,
      val suggestion: String? = null, val allowedValues: List<String>? = null,
  )
  fun preflight(base: BaseDirectoryEntry, deliberatelyEmpty: Set<String> = emptySet()): List<PreflightFinding>
  ```
  Rules (message texts are the implementer's, but must contain the attribute name and, where listed, the quoted fragments):
  - displayName null or fewer than 2 alphanumeric chars → ERROR
  - any of street/postalCode/locality/state set and countryCode null → ERROR, suggestion `DE`, message contains "ISO-3166-1 alpha-2"
  - `countryCode == "DE"` and postalCode not `^\d{5}$` → ERROR
  - `countryCode == "DE"` and stateOrProvinceName not in `Region` → ERROR, `allowedValues` = Region codes
  - sector (`SectorMapping.sectorOf(telematikID)`) null → WARNING on attribute `telematikID`, message contains "no sector"
  - each `sector.mandatory` attribute null/empty → ERROR, message contains sector name and `Tabelle <n>`; for `specialization` with `PHARMACY_LDAP` `allowedValues` = `"10 Offizin-Apotheke"` … `"50 Sterilherstellung"`, with PSC suggestion `urn:psc:1.3.6.1.4.1.19376.3.276.1.5.4:ALLG`, with AS suggestion `urn:as:1.2.276.0.76.5.514:011001`
  - specialization values present: PHARMACY_LDAP → each must be a PharmacyTypeLDAPCS code else ERROR with allowedValues; PSC → must match `OrganisationSpecializationRegex` else ERROR, matched but unresolved in `HealthcareServiceTypeVS` → WARNING; AS → same with `PractitionerSpecializationRegex` / `PractitionerQualificationVS`; NONE → no format check
  - a finding on an attribute contained in `deliberatelyEmpty` is downgraded from ERROR to WARNING

- [ ] **Step 1: Write the failing tests**

```kotlin
fun errors(f: List<PreflightFinding>) = f.filter { it.severity == FindingSeverity.ERROR }.map { it.attribute }
fun warnings(f: List<PreflightFinding>) = f.filter { it.severity == FindingSeverity.WARNING }.map { it.attribute }
val pharmacy = BaseDirectoryEntry("3-01.2.2023000", displayName = "Linden Apotheke", streetAddress = "Hauptstr. 1", postalCode = "10117", localityName = "Berlin", countryCode = "DE", specialization = listOf("10"))

scenario("Complete pharmacy has no errors") { errors(preflight(pharmacy)) shouldBe emptyList() }
scenario("Address without countryCode") { errors(preflight(pharmacy.copy(countryCode = null))) shouldBe listOf("countryCode") }
scenario("Four digit PLZ") { errors(preflight(pharmacy.copy(postalCode = "1011"))) shouldBe listOf("postalCode") }
scenario("State not in Region") {
    val f = preflight(pharmacy.copy(stateOrProvinceName = "Schleswig Holstein")).single { it.attribute == "stateOrProvinceName" }
    f.severity shouldBe FindingSeverity.ERROR; f.allowedValues shouldContain "Schleswig-Holstein"
}
scenario("FHIR pharmacy code is rejected for LDAP") {
    preflight(pharmacy.copy(specialization = listOf("offizin-apotheke"))).single { it.attribute == "specialization" }.allowedValues shouldContain "10 Offizin-Apotheke"
}
scenario("Missing pharmacy specialization is ERROR, deliberately empty is WARNING") {
    errors(preflight(pharmacy.copy(specialization = null))) shouldBe listOf("specialization")
    warnings(preflight(pharmacy.copy(specialization = null), setOf("specialization"))) shouldBe listOf("specialization")
}
scenario("KV practice needs domainID, accepts urn:psc") {
    val kv = pharmacy.copy(telematikID = "1-2012345678", specialization = listOf("urn:psc:1.3.6.1.4.1.19376.3.276.1.5.4:ALLG"))
    errors(preflight(kv)) shouldBe listOf("domainID")
}
scenario("HBA urn:as resolved and unresolved") {
    val hba = BaseDirectoryEntry("1-1123456789", displayName = "Fit, Heinrich", specialization = listOf("urn:as:1.2.276.0.76.5.514:011001"))
    preflight(hba) shouldBe emptyList()
    warnings(preflight(hba.copy(specialization = listOf("urn:as:1.2.276.0.76.5.514:999999")))) shouldBe listOf("specialization")
}
scenario("Unknown sector only warns") {
    val f = preflight(BaseDirectoryEntry("3-2-Test01", displayName = "Test"))
    errors(f) shouldBe emptyList(); warnings(f) shouldBe listOf("telematikID")
}
scenario("Short displayName") { errors(preflight(pharmacy.copy(displayName = "-"))) shouldBe listOf("displayName") }
```

- [ ] **Step 2: Run to verify failure.** `./gradlew :directory-lib:test --tests 'de.gematik.ti.directory.admin.TestBaseEntryPreflight'`

- [ ] **Step 3: Implement `BaseEntryPreflight.kt`.**

- [ ] **Step 4: Verify tests pass.**

- [ ] **Step 5: Commit**

```bash
git add directory-lib/src
git commit -m "Validate base entries against gemILF_Pflege_VZD rules before upload"
```

---

### Task 9: Request assembly (`CertificateEntryDraft`)

**Files:**
- Create: `directory-lib/src/main/kotlin/de/gematik/ti/directory/admin/CertificateEntryDraft.kt`
- Test: `directory-lib/src/test/kotlin/de/gematik/ti/directory/admin/TestCertificateEntryDraft.kt`

**Interfaces:**
- Consumes: Tasks 1, 2, 6, 7, 8.
- Produces:
  ```kotlin
  class CertificateSelectionException(message: String) : DirectoryException(message)
  fun selectEncryptionCertificates(certs: List<CertificateDataDER>, now: Instant = Clock.System.now()): List<CertificateDataDER>
      // keeps isEncryptionCertificate() && hasAdmission() && notBefore <= now <= notAfter; sorted by notBefore ascending
      // dropped candidates are logged at WARN with reason ("not an encryption certificate (C.HCI.AUT)", "expired 2024-12-11", "no admission extension")
      // none left → CertificateSelectionException listing every input as "<CN> | <type label or 'unknown type'> | serial <n> | <reason>" and ending with "VZD accepts only C.HCI.ENC or C.HP.ENC"
      // more than one distinct telematikID → CertificateSelectionException listing the IDs
      // selected certificates with differing subjects → one INFO log line
  data class CertificateEntryDraft(
      val base: BaseDirectoryEntry, val certificates: List<CertificateDataDER>,
      val entryType: Int?, val sector: Sector?, val findings: List<PreflightFinding>, val deliberatelyEmpty: Set<String>,
  ) {
      fun toCreateDirectoryEntry(): CreateDirectoryEntry   // base + certificates.map { UserCertificate(userCertificate = it) }
      fun hasErrors(): Boolean
  }
  fun draftFromCertificates(certs: List<CertificateDataDER>, overrides: Map<String, String>, now: Instant = Clock.System.now()): CertificateEntryDraft
      // select → base = fromCertificate(last) → entryType from last cert's professionOids
      // overrides["entryType"] present and != entryType → DirectoryException("Certificate professionOID <describe> yields entryType <n>, but entryType <m> was given"); then entryType removed from overrides
      // applyOverrides → preflight(base, deliberatelyEmpty)
  fun CertificateEntryDraft.withAnswers(answers: Map<String, String?>): CertificateEntryDraft
      // re-applies answers as overrides (null/blank → deliberately empty) and re-runs preflight; used by the interactive prompt loop
  ```

- [ ] **Step 1: Write the failing tests**

```kotlin
scenario("SMC-B DER builds request without derived attributes") {
    val d = draftFromCertificates(listOf(TestCertificates.load(TestCertificates.smcbRsaEnc)), emptyMap(), now = TestCertificates.smcbValidAt)
    val req = d.toCreateDirectoryEntry()
    req.directoryEntryBase!!.entryType shouldBe null
    req.directoryEntryBase!!.professionOID shouldBe null
    req.directoryEntryBase!!.streetAddress shouldBe "Am Markt 1"
    req.userCertificates!!.size shouldBe 1
    d.entryType shouldBe 3
}
scenario("RSA and ECC ENC of the same card are both sent, oldest first") {
    val certs = listOf(TestCertificates.smcbEccEnc, TestCertificates.smcbRsaEnc).map(TestCertificates::load)
    val d = draftFromCertificates(certs, emptyMap(), now = TestCertificates.smcbValidAt)
    d.certificates.size shouldBe 2
    d.certificates.zipWithNext().all { (a, b) -> a.certificateInfo.notBefore <= b.certificateInfo.notBefore } shouldBe true
}
scenario("Expired certificates are dropped") {
    shouldThrow<CertificateSelectionException> { draftFromCertificates(listOf(TestCertificates.load(TestCertificates.smcbRsaEnc)), emptyMap(), now = Instant.parse("2030-01-01T00:00:00Z")) }
}
scenario("Only AUT present") {
    val e = shouldThrow<CertificateSelectionException> { selectEncryptionCertificates(listOf(TestCertificates.toDer(TestCertificates.smcbAut()))) }
    e.message shouldContain "C.HCI.AUT"; e.message shouldContain "C.HCI.ENC"
}
scenario("Two TelematikIDs") {
    val a = TestCertificates.synthetic("CN=A", "1.2.276.0.76.4.76", KeyUsage.keyAgreement, "3-A").first
    val b = TestCertificates.synthetic("CN=B", "1.2.276.0.76.4.76", KeyUsage.keyAgreement, "3-B").first
    shouldThrow<CertificateSelectionException> { selectEncryptionCertificates(listOf(a, b).map(TestCertificates::toDer)) }.message shouldContain "3-B"
}
scenario("entryType override must match mapping") {
    shouldThrow<DirectoryException> { draftFromCertificates(listOf(TestCertificates.toDer(TestCertificates.pharmacyEnc())), mapOf("entryType" to "4")) }.message shouldContain "entryType 3"
}
scenario("Deliberately empty specialization yields warning only") {
    val d = draftFromCertificates(listOf(TestCertificates.toDer(TestCertificates.pharmacyEnc())), mapOf("specialization" to ""))
    d.hasErrors() shouldBe false
    d.findings.map { it.attribute } shouldContain "specialization"
    d.toCreateDirectoryEntry().directoryEntryBase!!.specialization shouldBe null
}
scenario("withAnswers resolves a finding") {
    val d = draftFromCertificates(listOf(TestCertificates.toDer(TestCertificates.pharmacyEnc())), emptyMap())   // missing specialization → error
    d.hasErrors() shouldBe true
    d.withAnswers(mapOf("specialization" to "10")).hasErrors() shouldBe false
}
```

- [ ] **Step 2: Run to verify failure.** `./gradlew :directory-lib:test --tests 'de.gematik.ti.directory.admin.TestCertificateEntryDraft'`

- [ ] **Step 3: Implement `CertificateEntryDraft.kt`.**

- [ ] **Step 4: Verify tests pass.**

- [ ] **Step 5: Commit**

```bash
git add directory-lib/src
git commit -m "Assemble add_Directory_Entry request from ENC certificates"
```

---

### Task 10: Server error hints

**Files:**
- Create: `directory-lib/src/main/kotlin/de/gematik/ti/directory/admin/ServerErrorHints.kt`
- Modify: `vzd-cli/src/main/kotlin/de/gematik/ti/directory/cli/Cli.kt:87-94` (`catching`)
- Test: `directory-lib/src/test/kotlin/de/gematik/ti/directory/admin/TestServerErrorHints.kt`

**Interfaces:**
- Produces:
  ```kotlin
  fun explainServerError(details: String, telematikID: String? = null, professionOIDs: List<String> = emptyList()): String?
      // case-insensitive substring matching; returns an explanation paragraph or null
      // "entrytype mismatch" → if professionOIDs non-empty: "<describe(oid)> → entryType <n>" per OID; else compact table "1 Berufsgruppe (HBA), 3 Leistungserbringerinstitution, 4 Organisation, 5 Krankenkasse, 7 KIM, 8 TIM, 9 DiGA, 10 Ombudsstelle"
      // "healthcarespeciality mismatch" → format rules per sector (sector from telematikID if given); pharmacy line lists PharmacyTypeLDAPCS codes and says FHIR codes like offizin-apotheke are not valid in the LDAP VZD
      // "komlecertpolicies" → "The VZD accepts only encryption certificates (C.HCI.ENC 1.2.276.0.76.4.76 or C.HP.ENC 1.2.276.0.76.4.74). Check with: vzd-cli admin cert-info <file>"
      // "stateorprovincename" → Region values
  ```
- `catching` appends `"\n" + hint` to `e.details` when `explainServerError(e.details)` is non-null. Commands that know the TelematikID/OIDs (Tasks 12, 13) call `explainServerError` themselves with context.

- [ ] **Step 1: Write the failing tests**

```kotlin
scenario("entryType mismatch with OID") { explainServerError("entryType mismatch", professionOIDs = listOf("1.2.276.0.76.4.54"))!! shouldContain "entryType 3" }
scenario("HealthcareSpeciality for pharmacy") { explainServerError("HealthcareSpeciality mismatch", telematikID = "3-01.2.2023000")!! shouldContain "10 Offizin-Apotheke" }
scenario("KOMLE") { explainServerError("Error when KOMLECertPolicies-check!")!! shouldContain "C.HCI.ENC" }
scenario("Unknown error") { explainServerError("Bad response") shouldBe null }
```

- [ ] **Step 2: Run to verify failure.** `./gradlew :directory-lib:test --tests 'de.gematik.ti.directory.admin.TestServerErrorHints'`

- [ ] **Step 3: Implement and wire into `catching`.**

- [ ] **Step 4: Verify** — lib test passes; `./gradlew :vzd-cli:compileKotlin -q`.

- [ ] **Step 5: Commit**

```bash
git add directory-lib/src vzd-cli/src
git commit -m "Explain known VZD server errors with allowed values"
```

---

### Task 11: `--passin` option group and PKCS#12 support in `cert-info`

**Files:**
- Create: `vzd-cli/src/main/kotlin/de/gematik/ti/directory/cli/admin/CertificateInputOptions.kt`
- Modify: `vzd-cli/src/main/kotlin/de/gematik/ti/directory/cli/admin/CertInfoCommand.kt`
- Test: `vzd-cli/src/test/kotlin/de/gematik/ti/directory/admin/TestCertificateInputOptions.kt`

**Interfaces:**
- Produces:
  ```kotlin
  class CertificateInputOptions : OptionGroup(name = "Certificate input") {
      val passin: String? by option("--passin", metavar = "pass:PW|env:VAR|file:PATH", help = "Password source for PKCS#12 files (like openssl)")
      fun passwordProvider(command: CliktCommand, noPrompt: Boolean): () -> CharArray?
          // pass: → literal; env: → System.getenv, missing var → UsageError; file: → first line of file; null → if noPrompt or System.console()==null → null, else command.prompt("PKCS#12 password", hideInput = true)
  }
  fun parsePassin(spec: String, env: (String) -> String?, readFile: (Path) -> String): CharArray   // pure, testable; unknown prefix → UsageError
  ```
- `cert-info` uses `loadCertificates(file, passwordProvider(this, noPrompt = false))` for each argument and prints every certificate found (new fields `certificateType`, `entryType` appear automatically).

- [ ] **Step 1: Write the failing tests**

```kotlin
scenario("pass:") { parsePassin("pass:secret", { null }, { "" }).concatToString() shouldBe "secret" }
scenario("env:") { parsePassin("env:P", { if (it == "P") "fromenv" else null }, { "" }).concatToString() shouldBe "fromenv" }
scenario("file: uses first line") { parsePassin("file:x", { null }, { "line1\nline2" }).concatToString() shouldBe "line1" }
scenario("missing env") { shouldThrow<UsageError> { parsePassin("env:NOPE", { null }, { "" }) } }
scenario("unknown prefix") { shouldThrow<UsageError> { parsePassin("plain", { null }, { "" }) } }
scenario("cert-info prints certificateType") {
    runCLI(listOf("admin", "cert-info", "testdata/1-SMC-B-Testkarte-883110000117035-787024799120720.der")) shouldContain "certificateType: C.HCI.ENC"
}
```

- [ ] **Step 2: Run to verify failure.** `./gradlew :vzd-cli:test --tests 'de.gematik.ti.directory.admin.TestCertificateInputOptions'`

- [ ] **Step 3: Implement.**

- [ ] **Step 4: Verify tests pass.**

- [ ] **Step 5: Commit**

```bash
git add vzd-cli/src
git commit -m "Accept PKCS#12 input and --passin in cert-info"
```

---

### Task 12: `add-cert` preflight and certificate source

**Files:**
- Modify: `vzd-cli/src/main/kotlin/de/gematik/ti/directory/cli/admin/AddCertCommand.kt`
- Test: `vzd-cli/src/test/kotlin/de/gematik/ti/directory/admin/TestAddCertPreflight.kt`

**Interfaces:**
- Consumes: `loadCertificates`, `selectEncryptionCertificates`, `CertificateInputOptions`, `EntryTypeMapping`, `explainServerError`.
- Produces (top-level in `AddCertCommand.kt`, pure for testing):
  ```kotlin
  fun checkCertificateAgainstEntry(cert: CertificateDataDER, entry: BaseDirectoryEntry): String?
      // null when ok; otherwise message:
      // telematikID differs → "Certificate telematikID <a> does not match entry <b>"
      // entry.entryType non-empty and != EntryTypeMapping.entryTypeOf(cert professionOids) → "Certificate yields entryType <n> (<describe>), entry has entryType <m>. Fix with: vzd-cli admin <env> modify-base-attr -t <id> -s entryType=<n> while the entry has no certificates."
  ```
- Behaviour: for each file, `loadCertificates` → `selectEncryptionCertificates` (CertificateSelectionException → CliktError with its message) → read entry → `checkCertificateAgainstEntry` (non-null → CliktError) → upload each selected certificate; on `AdminResponseException` other than 409 rethrow with `explainServerError(details, telematikID, professionOids)` appended.

- [ ] **Step 1: Write the failing tests**

```kotlin
val cert = TestCertificates.load(TestCertificates.smcbRsaEnc)
scenario("entryType mismatch is explained") {
    val entry = BaseDirectoryEntry(cert.certificateInfo.admissionStatement.registrationNumber, entryType = listOf("4"))
    checkCertificateAgainstEntry(cert, entry)!! shouldContain "entryType=3"
}
scenario("matching entry passes") {
    checkCertificateAgainstEntry(cert, BaseDirectoryEntry(cert.certificateInfo.admissionStatement.registrationNumber, entryType = listOf("3"))) shouldBe null
}
scenario("telematikID mismatch") {
    checkCertificateAgainstEntry(cert, BaseDirectoryEntry("3-other"))!! shouldContain "3-other"
}
scenario("add-cert with AUT file fails before network") {
    val autFile = TestCertificates.writeDer(TestCertificates.smcbAut(), tempdir().toPath(), "aut.der")
    shouldThrow<CliktError> { runCLI(listOf("admin", "ru", "add-cert", autFile.toString())) }.message shouldContain "C.HCI.AUT"
}
```

- [ ] **Step 2: Run to verify failure.** `./gradlew :vzd-cli:test --tests 'de.gematik.ti.directory.admin.TestAddCertPreflight'`

- [ ] **Step 3: Implement.** Keep `--ignore` semantics; run the preflight in the `--ignore` path too.

- [ ] **Step 4: Verify tests pass.**

- [ ] **Step 5: Commit**

```bash
git add vzd-cli/src
git commit -m "Check certificate type and entryType locally before add-cert upload"
```

---

### Task 13: `add-base --from-cert`

**Files:**
- Modify: `vzd-cli/src/main/kotlin/de/gematik/ti/directory/cli/admin/AddBaseCommand.kt`
- Create: `vzd-cli/src/main/kotlin/de/gematik/ti/directory/cli/admin/PreflightPrompter.kt`
- Test: `vzd-cli/src/test/kotlin/de/gematik/ti/directory/admin/TestAddBaseFromCert.kt`

**Interfaces:**
- Consumes: `draftFromCertificates`, `CertificateEntryDraft.withAnswers`, `loadCertificates`, `CertificateInputOptions`, `explainServerError`, `toYamlNoDefaults`.
- Produces:
  ```kotlin
  // AddBaseCommand new options
  private val fromCert: Path? by option("--from-cert", metavar = "FILE", help = "Create the entry from an ENC certificate (DER, PEM or PKCS#12) and add the certificate in the same request").path(mustExist = true, canBeDir = false, mustBeReadable = true)
  private val noPrompt: Boolean by option("--no-prompt", help = "Never ask interactively; fail on missing values").flag()
  private val certificateInput by CertificateInputOptions()

  // PreflightPrompter.kt
  fun formatFindingsForAbort(draft: CertificateEntryDraft): String
      // header "Following attributes are missing or invalid" + sector line "(sector: <name>, gemILF_Pflege_VZD Tabelle <n>)" when known,
      // one line per ERROR: "  <attribute>: <message>" + " Allowed: <allowedValues joined ', '>" when present,
      // final line: "Set them with " + ERROR attributes as "-s <attr>=<suggestion or VALUE>" + ", mark deliberately empty with -s <attr>=, or run without --no-prompt."
  fun promptForErrors(draft: CertificateEntryDraft, ask: (PreflightFinding, default: String?) -> String?): CertificateEntryDraft
      // loop: for each ERROR finding ask(finding, finding.suggestion ?: current value); collect answers; draft.withAnswers(answers); repeat while new ERRORs appear for attributes not yet asked; blank answer → deliberately empty
  ```
- Run flow when `fromCert != null`: `inputFile`/`deprecatedFile` present → UsageError "Use either --from-cert or an input file"; load → `draftFromCertificates(certs, attrs)` (DirectoryException → CliktError); `interactive = !noPrompt && System.console() != null`; if interactive `promptForErrors(draft) { f, d -> prompt(text = "${f.attribute} (${f.message})" + allowed, default = d) }` else if `draft.hasErrors()` → `throw CliktError(formatFindingsForAbort(draft))`; WARNINGs → `echo(..., err = true)`; preview `draft.base.toYamlNoDefaults()` on stderr; POST; on 409 with `--ignore`: read entry by telematikID, `addDirectoryEntryCertificate` for each certificate (409 there → warning); other errors → append `explainServerError(details, telematikID, professionOids)`; output as today.

- [ ] **Step 1: Write the failing tests**

```kotlin
val dir = tempdir().toPath()
val pharmacy = TestCertificates.writeDer(TestCertificates.pharmacyEnc(), dir, "pharmacy.der")   // valid now, address in subject, no specialization
val pharmacyDraft = draftFromCertificates(listOf(TestCertificates.toDer(TestCertificates.pharmacyEnc())), emptyMap())

scenario("--from-cert together with file argument is a usage error") {
    shouldThrow<UsageError> { runCLI(listOf("admin", "ru", "add-base", "--from-cert", pharmacy.toString(), "entry.yaml")) }
}
scenario("--no-prompt with removed countryCode aborts before any request and names the fix") {
    val e = shouldThrow<CliktError> { runCLI(listOf("admin", "ru", "add-base", "--from-cert", pharmacy.toString(), "--no-prompt", "-s", "specialization=10", "-s", "countryCode=")) }
    e.message shouldContain "countryCode"; e.message shouldContain "-s countryCode=DE"
}
scenario("--no-prompt with missing specialization lists the pharmacy codes") {
    val e = shouldThrow<CliktError> { runCLI(listOf("admin", "ru", "add-base", "--from-cert", pharmacy.toString(), "--no-prompt")) }
    e.message shouldContain "10 Offizin-Apotheke"; e.message shouldContain "Tabelle 4"
}
scenario("entryType override contradiction") {
    shouldThrow<CliktError> { runCLI(listOf("admin", "ru", "add-base", "--from-cert", pharmacy.toString(), "--no-prompt", "-s", "entryType=4")) }.message shouldContain "entryType 3"
}
scenario("formatFindingsForAbort lists allowed pharmacy codes and the escape hatch") {
    formatFindingsForAbort(pharmacyDraft) shouldContain "10 Offizin-Apotheke"
    formatFindingsForAbort(pharmacyDraft) shouldContain "-s specialization="
}
scenario("promptForErrors applies answers and treats blank as deliberately empty") {
    val draft = draftFromCertificates(listOf(TestCertificates.toDer(TestCertificates.pharmacyEnc())), mapOf("countryCode" to ""))
    val asked = mutableListOf<String>()
    val result = promptForErrors(draft) { f, d -> asked += f.attribute; if (f.attribute == "countryCode") d else "" }
    asked shouldContainExactlyInAnyOrder listOf("countryCode", "specialization")
    result.hasErrors() shouldBe false
    result.base.countryCode shouldBe "DE"; result.base.specialization shouldBe null
}
```

The `--no-prompt` scenarios must fail before `context.client` is touched (no `TEST_ACCESS_TOKEN` needed). `AdminCliEnvironmentContext.client` is lazy, so running load, draft and preflight before the first `context.client` access satisfies this.

- [ ] **Step 2: Run to verify failure.** `./gradlew :vzd-cli:test --tests 'de.gematik.ti.directory.admin.TestAddBaseFromCert'`

- [ ] **Step 3: Implement `PreflightPrompter.kt` and the `--from-cert` branch.** Leave the existing file/`-s telematikID -s entryType` path untouched.

- [ ] **Step 4: Verify tests pass**, then build the jar and run a manual smoke test without server:

Run: `./gradlew :vzd-cli:shadowJar -q && java -jar vzd-cli/build/libs/vzd-cli-all.jar admin ru add-base --from-cert vzd-cli/testdata/1-SMC-B-Testkarte-883110000117035-787024799120720.der --no-prompt`
Expected: exit ≠ 0, output says the certificate expired 2024-12-11 and that the VZD accepts only C.HCI.ENC or C.HP.ENC (the only bundled certificates are expired, so this exercises the selection error path end to end).

- [ ] **Step 5: Commit**

```bash
git add vzd-cli/src
git commit -m "Add add-base --from-cert to create entries directly from ENC certificates"
```

---

### Task 14: Documentation

**Files:**
- Modify: `README.adoc:484-492` (add-base), `README.adoc:523-542` (add-cert), `README.adoc:509-516` (cert-info), `README.adoc:652-663` (fhir login)
- Modify: `ReleaseNotes.adoc` ("Unreleased")

- [ ] **Step 1: README `add-base`**

Replace the example `-s entryType=4` with `-s entryType=3` and explain that `entryType` follows Tab_VZD_Mapping_Eintragstyp_und_ProfessionOID (3 = Leistungserbringerinstitution, e.g. pharmacy `1.2.276.0.76.4.54`; 1 = HBA; 4 = Organisation). Add a subsection for `--from-cert`:

```
# Eintrag direkt aus dem ENC-Zertifikat anlegen (DER, PEM oder .p12)
vzd-cli admin ru add-base --from-cert smcb.p12 --passin env:SMCB_PIN
# Werte überschreiben oder bewusst leer lassen
vzd-cli admin ru add-base --from-cert smcb.der -s specialization=10 -s stateOrProvinceName=
# Skripte: niemals nachfragen
vzd-cli admin ru add-base --from-cert smcb.der --no-prompt -s specialization=10
```

State which attributes come from the certificate, which the VZD sets itself, and that missing sector-mandatory values are prompted. Add a short table of `specialization` formats: Apotheke `10|20|30|40|50` (PharmacyTypeLDAPCS; FHIR codes like `offizin-apotheke` are not valid here), other SMC-B `urn:psc:<OID>:<Code>`, HBA `urn:as:<OID>:<Code>`.

- [ ] **Step 2: README `add-cert` and `cert-info`**

Mention PKCS#12 input, `--passin`, the local type/entryType check, and the new `certificateType`/`entryType` lines in `cert-info` output.

- [ ] **Step 3: README `fhir login`**

The command is `vzd-cli fhir <tu|ru|pu> fdv login` (class `FdvLoginCommand` under `FdvCommands`). Correct the heading and examples accordingly.

- [ ] **Step 4: ReleaseNotes.adoc "Unreleased"**

Add bullets: `add-base --from-cert` (DER/PEM/PKCS#12, prompts, `--no-prompt`), `add-cert` local checks, `cert-info` new fields and PKCS#12, server error hints, pharmacy specialization display fix (PharmacyTypeLDAPCS), `pers extract` no longer hard-codes entryType 1.

- [ ] **Step 5: Commit**

```bash
git add README.adoc ReleaseNotes.adoc
git commit -m "Document add-base --from-cert and certificate checks"
```

---

### Task 15: Whole-branch verification

- [ ] **Step 1: Run all network-free tests**

Run: `./gradlew :directory-lib:test :vzd-cli:test --tests 'de.gematik.ti.directory.admin.TestCertificateInputOptions' --tests 'de.gematik.ti.directory.admin.TestAddCertPreflight' --tests 'de.gematik.ti.directory.admin.TestAddBaseFromCert' --tests 'de.gematik.ti.directory.admin.TestQuickSearchTokenizer' --tests 'de.gematik.ti.directory.admin.TestVault'`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Lint**

Run: `./gradlew ktlintCheck` (if the task exists; otherwise skip and note). Expected: no violations in changed files.

- [ ] **Step 3: Optional RU integration check** (only with credentials): `vzd-cli admin ru add-base --from-cert <real smcb.p12>`; afterwards `vzd-cli admin ru show <telematikID>` shows the entry with the certificate, and `delete` cleans up.
