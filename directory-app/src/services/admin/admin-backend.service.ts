import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';
import { AdminStatus, ElaborateDirectoryEntry, DirectoryEntryFHIRResourceType, DirectoryEntryKind, Outcome, ElaborateSearchResults, BaseDirectoryEntry } from './admin.model';

// TODO: how does one provide labels in Angular?
const labels: Record<string, string> = {
  "ru": "Referenzumgebung",
  "tu": "Testumgebung",
  "pu": "Produktivumgebung"
}

@Injectable({
  providedIn: 'root'
})
export class AdminBackendService {
  public adminStatus: AdminStatus | undefined

  constructor(
    private http: HttpClient,
  ) { 
    const self = this
  }

  updateStatus() {
    firstValueFrom(
      this.http.get<AdminStatus>('/api/admin/status')
    ).then( adminStatus => {
      this.adminStatus = adminStatus
    })
  }

  get status$(): Observable<AdminStatus> {
    return new Observable<AdminStatus>(subscriber => {
      if (this.adminStatus == undefined) {
        this.updateStatus()
      }
      var lastStatus = this.adminStatus
      if (lastStatus != undefined) {
        subscriber.next(lastStatus)
      }
      setInterval(() => {
        if (lastStatus !== this.adminStatus) {
          lastStatus = this.adminStatus
          subscriber.next(this.adminStatus!)
        }
      }, 500)
    })
  }

  getEnvLabel(env: string) {
    return labels[env]
  }

  search(env: string, queryString: string) {
    return firstValueFrom(
      this.http.get<ElaborateSearchResults> (
        `/api/admin/${env}/search`, 
        { params: {"q": queryString} }
      )
    )
  }

  loginUsingVault(env: string, vaultPassword: string) {
    return firstValueFrom(
      this.http.post<Outcome>(
        `/api/admin/login`,
        {
          env: env,
          vaultPassword: vaultPassword
        }
      )
    )
  }

  loadEntry(env: string, telematikID: string) : Promise<ElaborateDirectoryEntry> {
    return firstValueFrom(
      this.http.get<ElaborateDirectoryEntry>(
        `/api/admin/${env}/entry/${telematikID}`
      )
    )
  }

  getEntryKindColor(entry: ElaborateDirectoryEntry) : string {
    switch(entry.kind) {

      case DirectoryEntryKind.Arzt:
        return "magenta"
      case DirectoryEntryKind.Arztpraxis:
          return "blue"
      case DirectoryEntryKind.Zahnarzt:
        return "red"
      case DirectoryEntryKind.Zahnarztpraxis:
          return "teal"
      case DirectoryEntryKind.Psychotherapeut:
        return "magenta"
      case DirectoryEntryKind.Krankenhaus:
          return "cyan"
      case DirectoryEntryKind.Apotheke: 
        return "purple"
      case DirectoryEntryKind.Krankenkasse: 
        return "outline"
      default:
        return "warm-gray"
    }
  }

  getEntryKindTitle(entry: ElaborateDirectoryEntry) : string {
    // replace _ with space
    return entry.kind.replace("_", " ")
  }

  getEntryKindIcon(entry: ElaborateDirectoryEntry) : string {
    if (entry.base.fhirResourceType == DirectoryEntryFHIRResourceType.Practitioner) {
      return "user"
    } else {
      return "hospital"
    }
  }

  loadBaseEntry(env: string, telematikID: string) : Promise<BaseDirectoryEntry> {
    return new Promise<BaseDirectoryEntry>((resolve, reject) => {
      resolve({
        telematikID: telematikID,
        domainID: ["domainID #1", "domainID #2"],
        dn: {
          cn: "cn",
          ou: ["ou"],
          dc: ["dc"],
          uid: "uid",
        },
        displayName: "SMB Test Betriebsstätte gematik "+telematikID,
        cn: "cn",
        otherName: "otherName",
        organization: "organization",
        givenName: "givenName",
        sn: "sn",
        title: "title",
        streetAddress: "streetAddress",
        postalCode: "postalCode",
        localityName: "localityName",
        stateOrProvinceName: "stateOrProvinceName",
        countryCode: "countryCode",
        professionOID: ["professionOID"],
        specialization: ["specialization"],
        entryType: ["1"],
        holder: ["holder"],
        dataFromAuthority: true,
        personalEntry: true,
        changeDateTime: "changeDateTime",
        maxKOMLEadr: 1,
        active: true,
        meta: ["meta"],
      })
    })
  }

  modifyBaseEntry(env: string, baseEntry: BaseDirectoryEntry): Promise<BaseDirectoryEntry> {
    return new Promise<BaseDirectoryEntry>((resolve, reject) => {
      // clone the object
      var modifiedEntry = JSON.parse(JSON.stringify(baseEntry))
      modifiedEntry.cn = "cn"
      resolve(modifiedEntry)
    })
  }

  deleteEntry(env: string, telematikID: string): Promise<Outcome> {
    return new Promise<Outcome>((resolve, reject) => {
      resolve({
        code: "true",
        message: "Entry deleted"
      })
    })
  }

  deactivateEntry(env: string, telematikID: string): Promise<Outcome> {
    return new Promise<Outcome>((resolve, reject) => {
      resolve({
        code: "true",
        message: "Entry deactivated"
      })
    })
  }

  activateEntry(env: string, telematikID: string): Promise<Outcome> {
    return new Promise<Outcome>((resolve, reject) => {
      resolve({
        code: "true",
        message: "Entry activated"
      })
    })
  }
}
