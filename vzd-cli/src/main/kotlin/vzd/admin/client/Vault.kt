package vzd.admin.client

import mu.KotlinLogging
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.IOException
import java.nio.file.Path
import java.security.KeyStore
import javax.crypto.spec.SecretKeySpec
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

private val logger = KotlinLogging.logger {}

data class Secret(var environment: String, var clientID: String, var secret: String)

private const val SERVICE_NAME = "urn:gematik:directory:admin"
private val REGEX = "^$SERVICE_NAME:([\\w\\p{L}\\-_]+):([\\w\\p{L}\\-_]+)".toRegex()

class VaultException(message: String): Exception(message)

class KeyStoreVaultProvider(val password: String,
                            val customVaultPath: Path? = null,
                            val reset: Boolean = false) {

    private val defaultVaultPath = Path(System.getProperty("user.home"), ".telematik", "directory-vault.keystore")

    val vaultPath get() = customVaultPath ?: defaultVaultPath

    init {
        if (!vaultPath.toFile().exists()) {
            vaultPath.parent.toFile().mkdirs()
        }
        if (reset) {
            logger.debug { "Resetting $vaultPath" }
            vaultPath.deleteIfExists()
        }
    }

    fun clear() {
        keyStore.aliases().asSequence().filter {
            REGEX.matches(it)
        }.forEach {
            keyStore.deleteEntry(it)
        }
        saveKeystore()
    }

    val keystorePassword: CharArray
        get() = this.password.toCharArray()

    fun list(): Sequence<Secret> {
        return keyStore.aliases().asSequence().mapNotNull { alias ->
            REGEX.matchEntire(alias)?.let {
                Secret(it.groups.get(1)!!.value, it.groups.get(2)!!.value,
                    getSecret(alias)!!)
            }
        }
    }

    fun store(environment: String, name: String, secret: String) {
        val protection = KeyStore.PasswordProtection(keystorePassword)
        val secretSpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "AES")
        keyStore.aliases().asSequence().filter {
            it.startsWith("$SERVICE_NAME:$environment:")
        }.forEach {
            keyStore.deleteEntry(it)
        }
        keyStore.setEntry("$SERVICE_NAME:$environment:$name", KeyStore.SecretKeyEntry(secretSpec), protection)
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

        val alias = keyStore.aliases().asSequence().find { it.startsWith("$SERVICE_NAME:$environment") }
            ?: return null

        logger.debug { "Found vault entry: $alias" }

        return REGEX.matchEntire(alias)?.let {
            Secret(it.groups.get(1)!!.value, it.groups.get(2)!!.value,
                getSecret(alias)!!)
        }
    }

    fun delete(environment: String) {
        keyStore.aliases().asSequence().filter {
            it.startsWith("$SERVICE_NAME:$environment")
        }.forEach {
            keyStore.deleteEntry(it)
        }
        saveKeystore()
    }

    private val keyStore: KeyStore by lazy {
        val keyStore = KeyStore.getInstance("BCFKS", BouncyCastleProvider())

        if (vaultPath.toFile().exists()) {
            try {
                keyStore.load(vaultPath.inputStream(), keystorePassword)
            } catch (e: IOException) {
                throw VaultException("Unable to open the Vault. Invalid password.")
            }
        } else {
            keyStore.load(null, password.toCharArray())
            keyStore.store(vaultPath.outputStream(), keystorePassword)
        }

        keyStore
    }

    private fun saveKeystore() = keyStore.store(vaultPath.outputStream(), keystorePassword)

}
