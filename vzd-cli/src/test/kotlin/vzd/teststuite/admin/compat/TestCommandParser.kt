package vzd.teststuite.admin.compat

import io.kotest.core.spec.style.FeatureSpec
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.core.Persister
import vzd.admin.cli.compat.CommandListDescriptor
import vzd.admin.client.Client
import vzd.teststuite.admin.createClient

class TestCommandParser : FeatureSpec({

    beforeSpec {
    }

    feature("XML Parsing") {
        scenario("Parse commands and their DNs") {
            val xml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <CommandList>
                    <Command>
                        <commandId>1</commandId>
                        <name>readDirectoryEntries</name>
                        <dn>
                            <uid>469fd210-b2f8-4d8e-b7ef-c0434303c26d</uid>
                        </dn>
                    </Command>
                    <Command>
                        <commandId>2</commandId>
                        <name>readDirectoryEntries</name>
                        <dn>
                            <uid>failUid</uid>
                            <dc>dc1</dc>
                            <dc>dc2</dc>
                            <ou>ou1</ou>
                            <ou>ou2</ou>
                        </dn>
                    </Command>
                    <Command>
                        <commandId>3</commandId>
                        <name>readDirectoryEntries</name>
                        <UserCertificate>
                            <telematikID>SomeNotExistingTelematikID</telematikID>
                        </UserCertificate>
                    </Command>
                </CommandList>
            """.trimIndent()

            val serializer: Serializer = Persister()
            val cmds = serializer.read(CommandListDescriptor::class.java, xml)

            cmds.commandList.map { it.dn }.map { it?.ou }
        }
    }
})
