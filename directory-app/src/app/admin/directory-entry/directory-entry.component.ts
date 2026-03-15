import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SnippetType } from 'carbon-components-angular/code-snippet';
import { AdminBackendService } from 'src/services/admin/admin-backend.service';
import { FhirBackendService } from 'src/services/fhir/fhir-backend.service';
import { Coding, ElaborateDirectoryEntry } from 'src/services/admin/admin.model';
import { IconService, NotificationContent } from 'carbon-components-angular';
import { Edit16, Fire16 } from "@carbon/icons";

interface KIMAddressInfo {
  mail: string
  provider: string | undefined
}

interface UserCertificateInfo {
  iconName: string,
  iconClass: string,
  serialNumber: string
  issuer: string
  notBefore: string
  notAfter: string
  algorithm: string
}

@Component({
    selector: 'app-admin-directory-entry',
    templateUrl: './directory-entry.component.html',
    styleUrls: ['./directory-entry.component.scss'],
    standalone: false
})
export class DirectoryEntryComponent implements OnInit {
  env!: string
  queryString: string | null = null
  entry?: ElaborateDirectoryEntry
  get rawData(): string { return JSON.stringify(this.entry, null, 2)}
  rawFhirData: string = ''
  fhirError: NotificationContent | null = null
  kimAddressList: KIMAddressInfo[] = []
  userCertificateList: UserCertificateInfo[] = []
  snippetDisplay = "multi" as SnippetType
  globalNotification: NotificationContent | null = null
  dateFormat = Intl.DateTimeFormat('de-DE', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,  // 24-Stunden-Format
  })

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private adminBackend: AdminBackendService,
    private fhirBackend: FhirBackendService,
    private changeDetector: ChangeDetectorRef,
    private iconService: IconService,
  ) { }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['modified'] == 'true') {
        this.globalNotification = {
          lowContrast: true,
          type: "success",
          title: "Erfolg",
          message: "Der Eintrag wurde erfolgreich geändert.",
        }
      }
    })
    this.route.params.subscribe(param => {
      this.queryString = param['q']
      const telematikID = param['id']
      this.env = param['env']
      this.adminBackend.loadEntry(this.env, telematikID).then(
        value => {
          this.entry = value
          this.userCertificateList = this.createUserCertificateList(value)
          this.changeDetector.detectChanges()
        }
      )
      this.fhirBackend.loadEntry(this.env, telematikID).then(
        value => {
          this.fhirError = null
          try {
            this.rawFhirData = JSON.stringify(JSON.parse(value), null, 2)
          } catch {
            this.rawFhirData = value
          }
          this.changeDetector.detectChanges()
        }
      ).catch((err) => {
        this.rawFhirData = ''
        const status = err?.status
        const message = status === 404
          ? 'Eintrag nicht im FHIR-Verzeichnis gefunden.'
          : `Fehler beim Laden der FHIR-Daten${status ? ` (HTTP ${status})` : ''}.`
        this.fhirError = { type: 'error', title: 'Fehler', message, lowContrast: true }
        this.changeDetector.detectChanges()
      })
    })
    this.iconService.register(Edit16)
    this.iconService.register(Fire16)
  }

  validateBaseField(field: string): boolean {
    if (this.entry?.validationResult?.base[field] != undefined) {
      return true
    }
    return false
  }

  protected get domainIDText() {
    var text = ""
    this.entry?.base.domainID?.forEach(id => text += `${id}\n`)
    return text
  }

  protected codeIconName(code: Coding): string {
    if (code.display != code.code) {
     return "checkmark--filled"
    } else {
      return "error--filled"
    }
  }

  protected codeIconClass(code: Coding): string {
    if (code.display != code.code) {
     return "success"
    } else {
      return "error"
    }
  }

  protected get entryKindTitle() {
    if (this.entry == undefined) {
      return ""
    }
    return this.adminBackend.getEntryKindTitle(this.entry)
  }

  private createUserCertificateList(entry: ElaborateDirectoryEntry): UserCertificateInfo[] {
    return entry.userCertificates?.map( certBlock => {
      const cert = certBlock.userCertificate 
      if (cert === undefined) {
        return undefined
      }
      const regex = /O=([^,]*)/;
      var issuer = ""
      const found = cert.issuer.match(regex);
      if (found?.length == 2) {
        issuer = found[1]
      }

      return {
        iconName: certBlock.active ? "checkmark--filled" : "warning--filled",
        iconClass: certBlock.active ? "success" : "warning",
        serialNumber: cert.serialNumber,
        issuer: issuer,
        notBefore: cert.notBefore.substring(0, 10),
        notAfter: cert.notAfter.substring(0, 10),
        algorithm: cert.publicKeyAlgorithm
      }
    }).filter( x => x !== undefined) as UserCertificateInfo[] || []
  }

  onEdit() {
    this.router.navigate(
      ["entry", this.entry?.base.telematikID, "edit", {"q": this.queryString}],
      { relativeTo: this.route.parent }
    )
  }

  operationLabel(operation: string): string {
    return this.adminBackend.getOperationLabel(operation)
  }

  formatDate(dateStr: string) {
    return this.dateFormat.format(new Date(dateStr))
  }
}
