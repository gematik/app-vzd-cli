import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, firstValueFrom, Observable } from 'rxjs';
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
  }

  parseError(error: any): Error {
    if (error instanceof HttpErrorResponse && error.error != undefined && error.error.message != undefined) {
      return new Error(error.error.message)
    }
    return new Error(error.message)
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
    return firstValueFrom(
      this.http.get<BaseDirectoryEntry>(
        `/api/admin/${env}/base-entry/${telematikID}`
      )
    )
  }

  modifyBaseEntry(env: string, baseEntry: BaseDirectoryEntry): Promise<BaseDirectoryEntry> {
    return firstValueFrom(
      this.http.put<BaseDirectoryEntry>(
        `/api/admin/${env}/base-entry/${baseEntry.telematikID}`,
        baseEntry
      ).pipe(catchError((error) => {
        throw this.parseError(error)
      })
      )
    )
  }

  deleteEntry(env: string, telematikID: string): Promise<Outcome> {
    return new Promise<Outcome>((resolve, reject) => {
      reject({
        code: "error",
        message: "Löschen der Einträge über GUI ist derzeit nicht möglich"
      })
    })
  }

  deactivateEntry(env: string, telematikID: string): Promise<Outcome> {
    return firstValueFrom(
      this.http.put<Outcome>(
        `/api/admin/${env}/entry/${telematikID}/activation`,
        {active: false}
      ).pipe(catchError((error) => {
        throw this.parseError(error)
      })
      )
    )
  }

  activateEntry(env: string, telematikID: string): Promise<Outcome> {
    return firstValueFrom(
      this.http.put<Outcome>(
        `/api/admin/${env}/entry/${telematikID}/activation`,
        {active: true}
      ).pipe(catchError((error) => {
        throw this.parseError(error)
      })
      )
    )
  }

  getOperationLabel(operation: string) {
    switch(operation) {
      case "add_Directory_Entry":
        return "Eintrag hinzufügt"
      case "modify_Directory_Entry":
        return "Eintrag geändert"
      case "delete_Directory_Entry":
        return "Eintrag gelöscht"
      case "stateSwitch_Directory_Entry":
        return "Eintrag aktiviert/deaktiviert"
      case "add_Directory_Entry_Certificate":
        return "Zertifikat hinzugefügt"
      case "delete_Directory_Entry_Certificate":
        return "Zertifikat gelöscht"
      case "add_Directory_FA-Attributes":
        return "Anwendungsdaten hinzugefügt"
      case "modify_Directory_FA-Attributes":
        return "Anwendungsdaten geändert"
      case "delete_Directory_FA-Attribute":
        return "Anwendungsdaten gelöscht"
      default:
        return operation
    }
  }

}
