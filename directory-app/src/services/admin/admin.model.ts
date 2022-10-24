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

 interface NameInfo {
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
 
export interface BaseDirectoryEntry {
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
  professionOID?: [string] 
  specialization?: [string] 
  entryType?: [string] 

  // System
  holder?: [string] 
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
    professionOID?: [string]
    usage?: [string]
    
    userCertificate?: CertificateInfo
    description?: string
    active?: boolean
}

export interface KomLeData {
    mail: string
    version: string
}

export interface FAD1 {
    dn: DistinguishedName
    mail?: [string]
    "KOM-LE_Version"?: string
    komLeData?: [KomLeData]
}

export interface Fachdaten {   
    dn: DistinguishedName
    "FAD1": [FAD1]
}

export interface DirectoryEntry {
    "DirectoryEntryBase": BaseDirectoryEntry
    userCertificates?: [UserCertificate]
    "Fachdaten"?: [Fachdaten]
}
