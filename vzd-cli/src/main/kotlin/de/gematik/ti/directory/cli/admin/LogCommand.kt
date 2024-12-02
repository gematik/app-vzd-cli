package de.gematik.ti.directory.cli.admin

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.path
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import de.gematik.ti.directory.admin.LogEntry
import de.gematik.ti.directory.admin.Operation
import de.gematik.ti.directory.cli.catching
import de.gematik.ti.directory.cli.toJsonPretty
import de.gematik.ti.directory.cli.toYamlNoDefaults
import de.gematik.ti.directory.util.escape
import hu.vissy.texttable.dsl.tableFormatter
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import java.io.PrintStream
import kotlin.io.path.outputStream

private val operations = Operation.values().joinToString("\n") { it.toString() }

class LogCommand : CliktCommand(name = "log", help = "Show logs") {
    private val context by requireObject<AdminCliEnvironmentContext>()
    private val outputFormat by option()
        .switch(
            "--table" to RepresentationFormat.TABLE,
            "--yaml" to RepresentationFormat.YAML,
            "--json" to RepresentationFormat.JSON,
            "--csv" to RepresentationFormat.CSV,
        ).default(RepresentationFormat.TABLE)
    private val outfile by option(
        "-o",
        "--outfile",
        help = "Write output to file",
    ).path(mustExist = false, canBeDir = false, canBeFile = true)

    private val primaryParam by mutuallyExclusiveOptions<Pair<String, String>>(
        option("-u", "--uid", help = "UID of an entry").convert { Pair("uid", it) },
        option("-t", "--telematikID", help = "TelematikID of an entry").convert { Pair("telematikID", it) },
        option("-c", "--clientID", help = "ClientID of an organisation who performed a change").convert { Pair("clientID", it) },
        option(
            "--operation",
            help = "Name of an operation performed on an entry. Possible values: $operations",
        ).choice(*Operation.values().map { it.toString() }.toTypedArray())
            .convert { Pair("operation", it.toString()) },
        option("--noDataChanged").convert { Pair("noDataChanged", it) },
    ).required()

    private val logTimeFrom by option("--logTimeFrom").convert { Instant.parse(it) }
    private val logTimeTo by option("--logTimeTo").convert { Instant.parse(it) }

    override fun run() =
        catching {
            val params =
                buildMap {
                    put(primaryParam.first, primaryParam.second)
                    logTimeFrom?.let { put("logTimeFrom", it.toString()) }
                    logTimeTo?.let { put("logTimeTo", it.toString()) }
                }

            val logEntries = runBlocking { context.client.readLog(params) }

            val stdout = System.`out`
            try {
                outfile?.let { System.setOut(PrintStream(it.outputStream())) }
                echo(logEntries.toStringRepresentation(outputFormat))
            } finally {
                System.setOut(stdout)
            }
        }
}

fun List<LogEntry>.toStringRepresentation(format: RepresentationFormat): String =
    when (format) {
        RepresentationFormat.YAML -> this.toYamlNoDefaults()
        RepresentationFormat.JSON -> this.toJsonPretty()
        RepresentationFormat.CSV -> this.toCsv()
        else -> this.toTable()
    }

fun List<LogEntry>.toTable(): String {
    val formatter =
        tableFormatter<LogEntry> {
            labeled<String>("TelematikID", "Gesamt") {
                extractor { logEntry ->
                    logEntry.telematikID?.escape()
                }
            }

            class State(
                var count: Int = 0
            )
            stateful<String, State>("ClientID") {
                initState { State() }
                extractor { logEntry, state ->
                    state.count += 1
                    logEntry.clientID
                }
                aggregator { _, state ->
                    state.count.toString()
                }
            }

            stateless("Operation") {
                extractor {
                    it.operation
                }
            }

            stateless("Timestamp") {
                extractor {
                    it.logTime
                }
            }

            stateless("") {
                extractor {
                    it.noDataChanged
                }
            }

            showAggregation = true
        }

    return formatter.apply(this)
}

fun List<LogEntry>.toCsv(): String {
    val csvWriter =
        csvWriter {
            delimiter = ';'
        }
    val list = this
    return buildString {
        append('\uFEFF')
        append(
            listToCsvLine(
                csvWriter,
                listOf(
                    "logTime",
                    "operation",
                    "clientID",
                    "telematikID",
                    "uid",
                    "noDataChanged",
                ),
            ),
        )
        list.forEach {
            append(
                listToCsvLine(
                    csvWriter,
                    listOf(
                        it.logTime,
                        it.operation,
                        it.clientID,
                        it.telematikID?.escape(),
                        it.uid,
                        it.noDataChanged,
                    ),
                ),
            )
        }
    }
}
