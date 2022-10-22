package de.gematik.ti.directory.util

import mu.KotlinLogging
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.IOException
import java.nio.file.Path
import java.security.KeyStore
import javax.crypto.spec.SecretKeySpec
import kotlin.io.path.*

private val logger = KotlinLogging.logger {}

data class Secret(var variant: String, var name: String, var secret: String)

private const val DEFAULT_SERVICE_NAME = "urn:gematik:directory:admin"

class VaultException(message: String) : Exception(message)

class KeyStoreVaultProvider(val customVaultPath: Path? = null) {

    private val defaultVaultPath = Path(System.getProperty("user.home"), ".telematik", "directory-vault.keystore")
    private val vaultPath get() = customVaultPath ?: defaultVaultPath

    init {
        if (!vaultPath.toFile().exists()) {
            vaultPath.absolute().parent.toFile().mkdirs()
        }
    }

    fun exists(): Boolean {
        return vaultPath.exists()
    }

    fun purge() {
        logger.debug { "Purging the Vault at $vaultPath" }
        vaultPath.deleteIfExists()
    }

    fun open(password: String, serviceName: String = DEFAULT_SERVICE_NAME): KeyStoreVault {
        return KeyStoreVault(password, vaultPath, serviceName)
    }
}

class KeyStoreVault(private val password: String, private val keystorePath: Path, private val serviceName: String) {
    private val pattern = "^$serviceName:([\\w\\p{L}\\-_]+):([\\w\\p{L}\\-_]+)".toRegex()
    fun clear() {
        keyStore.aliases().asSequence().filter {
            pattern.matches(it)
        }.forEach {
            keyStore.deleteEntry(it)
        }
        saveKeystore()
    }

    private val keystorePassword: CharArray
        get() = this.password.toCharArray()

    fun list(): Sequence<Secret> {
        return keyStore.aliases().asSequence().mapNotNull { alias ->
            pattern.matchEntire(alias)?.let {
                Secret(
                    it.groups[1]!!.value,
                    it.groups[2]!!.value,
                    getSecret(alias)!!
                )
            }
        }
    }

    fun store(environment: String, name: String, secret: String) {
        val protection = KeyStore.PasswordProtection(keystorePassword)
        val secretSpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "AES")
        keyStore.aliases().asSequence().filter {
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
        val alias = keyStore.aliases().asSequence().find { it.startsWith("$serviceName:$environment") }
            ?: return null

        logger.debug { "Found vault entry: $alias" }

        return pattern.matchEntire(alias)?.let {
            Secret(
                it.groups[1]!!.value,
                it.groups[2]!!.value,
                getSecret(alias)!!
            )
        }
    }

    fun delete(environment: String) {
        keyStore.aliases().asSequence().filter {
            it.startsWith("$serviceName:$environment")
        }.forEach {
            keyStore.deleteEntry(it)
        }
        saveKeystore()
    }

    private val keyStore: KeyStore by lazy {
        val keyStore = KeyStore.getInstance("BCFKS", BouncyCastleProvider())

        if (keystorePath.toFile().exists()) {
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
