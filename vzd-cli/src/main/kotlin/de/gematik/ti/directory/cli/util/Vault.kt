/*
 * Copyright (Change Date see Readme), gematik GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 */

package de.gematik.ti.directory.cli.util

import mu.KotlinLogging
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.IOException
import java.nio.file.Path
import java.security.KeyStore
import javax.crypto.spec.SecretKeySpec
import kotlin.io.path.*

private val logger = KotlinLogging.logger {}

data class Secret(
    var variant: String,
    var name: String,
    var secret: String
)

class VaultException(
    message: String
) : Exception(message)

class KeyStoreVaultProvider(
    val customVaultPath: Path? = null
) {
    private val defaultVaultPath = Path(System.getProperty("user.home"), ".telematik", "directory-vault.keystore")
    private val vaultPath get() = customVaultPath ?: defaultVaultPath

    init {
        if (!vaultPath.toFile().exists()) {
            vaultPath
                .absolute()
                .parent
                .toFile()
                .mkdirs()
        }
    }

    fun exists(): Boolean = vaultPath.exists()

    fun purge() {
        logger.debug { "Purging the Vault at $vaultPath" }
        vaultPath.deleteIfExists()
    }

    fun open(
        password: String,
        serviceName: String,
    ): KeyStoreVault = KeyStoreVault(password, vaultPath, serviceName)
}

class KeyStoreVault(
    private val password: String,
    private val keystorePath: Path,
    private val serviceName: String
) {
    private val pattern = "^$serviceName:([\\w\\p{L}\\-_]+):([\\w\\p{L}\\-_]+)".toRegex()

    fun clear() {
        keyStore
            .aliases()
            .asSequence()
            .filter {
                pattern.matches(it)
            }.forEach {
                keyStore.deleteEntry(it)
            }
        saveKeystore()
    }

    private val keystorePassword: CharArray
        get() = this.password.toCharArray()

    fun list(): Sequence<Secret> =
        keyStore.aliases().asSequence().mapNotNull { alias ->
            pattern.matchEntire(alias)?.let {
                Secret(
                    it.groups[1]!!.value,
                    it.groups[2]!!.value,
                    getSecret(alias)!!,
                )
            }
        }

    fun store(
        environment: String,
        name: String,
        secret: String,
    ) {
        val protection = KeyStore.PasswordProtection(keystorePassword)
        val secretSpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "AES")
        keyStore
            .aliases()
            .asSequence()
            .filter {
                it.startsWith("$serviceName:$environment:")
            }.forEach {
                keyStore.deleteEntry(it)
            }
        keyStore.setEntry("$serviceName:$environment:$name", KeyStore.SecretKeyEntry(secretSpec), protection)
        saveKeystore()
    }

    private fun getSecret(alias: String): String? {
        val protection = KeyStore.PasswordProtection(keystorePassword)
        val secret = keyStore.getEntry(alias, protection)

        return if (secret != null) {
            String((secret as KeyStore.SecretKeyEntry).secretKey.encoded)
        } else {
            null
        }
    }

    fun get(environment: String): Secret? {
        val alias =
            keyStore.aliases().asSequence().find { it.startsWith("$serviceName:$environment") }
                ?: return null

        logger.debug { "Found vault entry: $alias" }

        return pattern.matchEntire(alias)?.let {
            Secret(
                it.groups[1]!!.value,
                it.groups[2]!!.value,
                getSecret(alias)!!,
            )
        }
    }

    fun delete(environment: String) {
        keyStore
            .aliases()
            .asSequence()
            .filter {
                it.startsWith("$serviceName:$environment")
            }.forEach {
                keyStore.deleteEntry(it)
            }
        saveKeystore()
    }

    private val keyStore: KeyStore by lazy {
        val keyStore = KeyStore.getInstance("BCFKS", BouncyCastleProvider())

        if (keystorePath.toFile().exists() && keystorePath.toFile().length() > 0) {
            try {
                keyStore.load(keystorePath.inputStream(), keystorePassword)
            } catch (e: IOException) {
                throw VaultException("Unable to open the Vault. Invalid password.")
            }
        } else {
            keyStore.load(null, password.toCharArray())
            keyStore.store(keystorePath.outputStream(), keystorePassword)
        }

        keyStore
    }

    private fun saveKeystore() = keyStore.store(keystorePath.outputStream(), keystorePassword)
}
