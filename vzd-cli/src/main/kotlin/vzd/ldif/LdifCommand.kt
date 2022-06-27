package vzd.ldif

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

const val HIDDEN_VALUE = "<hidden>"

class LdifCommand :
    CliktCommand(name = "ldif", help = """Command for operating with directory LDIF/LDAP dumps""".trimMargin()) {

    override fun run() = Unit

    init {
        subcommands(
            ToDumpCommand()
        )
    }
}
