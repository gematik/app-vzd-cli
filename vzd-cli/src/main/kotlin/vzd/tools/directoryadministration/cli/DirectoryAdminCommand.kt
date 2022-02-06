package vzd.tools.directoryadministration.cli

import com.github.ajalt.clikt.core.CliktCommand

abstract class DirectoryAdminCommand(
    help: String,
    epilog: String,
    name: String?,
    invokeWithoutSubcommand: Boolean,
    printHelpOnEmptyArgs: Boolean,
    helpTags: Map<String, String>,
    autoCompleteEnvvar: String?,
    allowMultipleSubcommands: Boolean,
    treatUnknownOptionsAsArgs: Boolean
) : CliktCommand(
    help,
    epilog,
    name,
    invokeWithoutSubcommand,
    printHelpOnEmptyArgs,
    helpTags,
    autoCompleteEnvvar,
    allowMultipleSubcommands,
    treatUnknownOptionsAsArgs
) {
    override fun run() {
        TODO("Not yet implemented")
    }
}