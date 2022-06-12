package vzd.ldif

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.types.path
import vzd.admin.cli.*

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