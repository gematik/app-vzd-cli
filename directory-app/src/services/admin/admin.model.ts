export interface Config {

}

export interface InfoObject {
  title: string
  version: string
  description?: string
}

export interface AdminEnvironmentStatus {
  env: string
  accessTokenClaims?: Map<String, any>,
  backendInfo?: InfoObject | null
}

export interface AdminStatus {
  config: Config,
  environmentStatus: [AdminEnvironmentStatus]

}

export interface DistinguishedName {
  uid: string
  dc?: [string]
  ou?: [string]
  cn?: string
}

export interface NameInfo {
     cn?: string
     givenName?: string
     sn?: string
     title?: string
     serialNumber?: string
     streetAddress?: string
     postalCode?: string
     localityName?: string
     stateOrProvinceName?: string
     countryCode?: string
 }
 
export interface AdmissionStatementInfo {
     admissionAuthority: string
     professionItems: [string]
     professionOids: [string]
     registrationNumber: String
}

enum OCSPResponseCertificateStatus {
  GOOD,
  REVOKED,
  UNKNOWN,
  CERT_HASH_ERROR,
  ERROR
}

export interface OCSPResponse {
  status: OCSPResponseCertificateStatus
  message?: string
}

export interface CertificateInfo {
     subject: string
     subjectInfo: NameInfo,
     issuer: string
     signatureAlgorithm: string
     publicKeyAlgorithm: string
     serialNumber: string
     keyUsage: [string]
     notBefore: string
     notAfter: string
     admissionStatement: AdmissionStatementInfo,
     certData: string
     ocspReponderURL?: string
     ocspResponse?: OCSPResponse
}

export interface Coding {
  code: string,
  display: string,
  system?: string,
}

export enum DirectoryEntryKind {
  Arzt = "Arzt",
  Arztpraxis = "Arztpraxis",
  Zahnarzt = "Zahnarzt",
  Zahnarztpraxis = "Zahnarztpraxis",
  Apotheke = "Apotheke",
  Apotheker = "Apotheker",
  Psychotherapeut = "Psychotherapeut",
  Krankenhaus = "Krankenhaus",
  Krankenkasse = "Krankenkasse",
  Krankenkasse_ePA = "Krankenkasse_ePA",
  HBAGematik = "HBAGematik",
  SMCBGematik = "SMCBGematik",
  HBAeGBR = "HBAeGBR",
  SMCBeGBR = "SMCBeGBR",
  Weitere = "Weitere"
}

export enum DirectoryEntryFHIRResourceType {
  Practitioner = "Practitioner",
  Organization = "Organization",
}
export interface ElaborateBaseDirectoryEntry {
  // types
  kind: DirectoryEntryKind
  fhirResourceType: DirectoryEntryFHIRResourceType
  // Identifier
  telematikID: string
  domainID?: [string] 
  dn?: DistinguishedName 

  // Names
  displayName?: string 
  cn?: string 
  otherName?: string 
  organization?: string 
  givenName?: string 
  sn?: string 
  title?: string 

  // Addresses
  streetAddress?: string 
  postalCode?: string 
  localityName?: string 
  stateOrProvinceName?: string 
  countryCode?: string 

  // Professional
  professionOID?: [Coding] 
  specialization?: [Coding] 
  entryType?: [string] 

  // System
  holder?: [Coding] 
  dataFromAuthority?: boolean
  personalEntry?: boolean
  changeDateTime?: string 

  // Internal
  maxKOMLEadr?: number

}

export interface UserCertificate {
  dn?: DistinguishedName
  entryType?: string
  telematikID?: string
  professionOID?: string[]
  usage?: string[]
  
  userCertificate?: CertificateInfo
  description?: string
  active?: boolean
}

export interface ElaborateDirectoryEntry {
  base: ElaborateBaseDirectoryEntry
  userCertificates?: UserCertificate[]
  kind: DirectoryEntryKind
  kimAddresses?: ElaborateKIMAddress[]
  smartcards?: Smartcard[]
  validationResult?: any
  logs: Array<ElaborateLogEntry> | undefined
}

export interface ElaborateKIMAddress {
    mail: string,
    version: string,
    provider?: Coding,
}

export interface ElaborateSearchResults {
  queryString: string
  directoryEntries: [ElaborateDirectoryEntry]
}

export interface Outcome {
  code: string
  message: string
}

export enum  SmartcardType {
  HBA = "HBA",
  HBA2_1 = "HBA2_1",
  SMCB = "SMCB",
  SMCB2_1 = "SMCB2_1",
}

export interface Smartcard {
    type: SmartcardType,
    notBefore: string,
    notAfter: string,
    active: boolean,
    certificateSerialNumbers: [string],
}

export interface BaseDirectoryEntry {
    // Identifier
    telematikID: string,
    domainID: Array<string> | undefined,
    dn: DistinguishedName | undefined,
    // Names
    displayName: string | undefined,
    cn: string | undefined,
    otherName: string | undefined,
    organization: string | undefined,
    givenName: string | undefined,
    sn: string | undefined,
    title: string | undefined,
    // Addresses
    streetAddress: string | undefined,
    postalCode: string | undefined,
    localityName: string | undefined,
    stateOrProvinceName: string | undefined,
    countryCode: string | undefined,
    // Professional
    professionOID: Array<string> | undefined,
    specialization: Array<string> | undefined,
    entryType: Array<string> | undefined,
    // System
    holder: Array<string> | undefined,
    dataFromAuthority: boolean | undefined,
    personalEntry: boolean | undefined,
    changeDateTime: string | undefined,
    // Internal
    maxKOMLEadr: number | undefined,
    // Misc
    active: boolean,
    meta: Array<string> | undefined,  
}

export interface ElaborateLogEntry {
  clientID: string
  logTime: string
  operation: string
  noDataChange: boolean
}
