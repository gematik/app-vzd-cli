package vzd.admin.cli.compat

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root
class CommandListDescriptor {
    @field:ElementList(name = "Command", required = false, inline = true)
    lateinit var commandList: List<CommandDescriptor>
}

@Root
class DistinguishedName() {
    @field:Element(required = true)
    var uid: String? = null

    @field:ElementList(entry = "dc", required = false, inline = true)
    var dc: List<String>? = null

    @field:ElementList(entry = "ou", required = false, inline = true)
    var ou: List<String>? = null

    @field:Element(required = false)
    var cn: String? = null
}

@Root(name = "Command", strict = false)
class CommandDescriptor() {
    @field:Element(required = false)
    var commandId: String? = null

    @field:Element(required = true)
    var name: String? = null

    @field:Element(required = false)
    var dn: DistinguishedName? = null
    var givenName: String? = null
    var sn: String? = null
    var cn: String? = null
    var displayName: String? = null
    var streetAddress: String? = null
    var postalCode: String? = null
    var countryCode: String? = null
    var localityName: String? = null
    var stateOrProvinceName: String? = null
    var title: String? = null
    var organization: String? = null
    var otherName: String? = null
    var telematikID: String? = null
    var telematikIDSubStr: String? = null
    var specialization: String? = null
    var domainID: String? = null
    var holder: String? = null
    var maxKOMLEadr: String? = null
    var personalEntry: String? = null
    var dataFromAuthority: String? = null
    var professionOID: String? = null
    var entryType: String? = null
    var changeDateTime: String? = null
    var baseEntryOnly: String? = null
    var userCertificate: String? = null
    // var fachdaten: Fachdaten? = null
}

class UserCertificate {
    var dn: DistinguishedName? = null
    var entryType: String? = null
    var telematikID: String? = null
    var professionOID: String? = null
    var usage: List<String>? = null
    var userCertificate: String? = null
    var description: String? = null
}
