import { Component, OnInit, signal } from '@angular/core';
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

interface FhirDaySchedule {
  day: string
  closed: boolean
  slots: { startTime?: string; endTime?: string; allDay: boolean }[]
}

interface FhirNotAvailable {
  description: string
  from?: string
  to?: string
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
  activeTab = 0
  entry = signal<ElaborateDirectoryEntry | undefined>(undefined)
  get rawData(): string { return JSON.stringify(this.entry(), null, 2)}
  rawFhirData: string = ''
  fhirError: NotificationContent | null = null
  fhirDaySchedule = signal<FhirDaySchedule[]>([])
  fhirNotAvailable: FhirNotAvailable[] = []
  kimAddressList: KIMAddressInfo[] = []
  userCertificateList: UserCertificateInfo[] = []
  snippetDisplay = "multi" as SnippetType
  globalNotification = signal<NotificationContent | undefined>(undefined)
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
    private iconService: IconService,
  ) { }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['modified'] == 'true') {
        this.globalNotification.set({
          lowContrast: true,
          type: "success",
          title: "Erfolg",
          message: "Der Eintrag wurde erfolgreich geändert.",
        })
      }
    })
    this.route.params.subscribe(param => {
      this.queryString = param['q']
      const telematikID = param['id']
      this.env = param['env']
      this.adminBackend.loadEntry(this.env, telematikID).then(
        value => {
          this.entry.set(value)
          this.userCertificateList = this.createUserCertificateList(value)
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
          this.parseFhirOpeningTimes(value)
        }
      ).catch((err) => {
        this.rawFhirData = ''
        this.fhirDaySchedule.set([])
        this.fhirNotAvailable = []
        const status = err?.status
        const message = status === 404
          ? 'Eintrag nicht im FHIR-Verzeichnis gefunden.'
          : `Fehler beim Laden der FHIR-Daten${status ? ` (HTTP ${status})` : ''}.`
        this.fhirError = { type: 'error', title: 'Fehler', message, lowContrast: true }
      })
    })
    this.iconService.register(Edit16)
    this.iconService.register(Fire16)
  }

  validateBaseField(field: string): boolean {
    if (this.entry()?.validationResult?.base[field] != undefined) {
      return true
    }
    return false
  }

  protected get domainIDText() {
    var text = ""
    this.entry()?.base.domainID?.forEach(id => text += `${id}\n`)
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
    const entry = this.entry()
    if (entry == undefined) {
      return ""
    }
    return this.adminBackend.getEntryKindTitle(entry)
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
      ["entry", this.entry()?.base.telematikID, "edit", {"q": this.queryString}],
      { relativeTo: this.route.parent }
    )
  }

  operationLabel(operation: string): string {
    return this.adminBackend.getOperationLabel(operation)
  }

  formatDate(dateStr: string) {
    return this.dateFormat.format(new Date(dateStr))
  }

  private parseFhirOpeningTimes(json: string): void {
    const dayOrder = ['mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun']
    const dayLabels: Record<string, string> = {
      mon: 'Montag', tue: 'Dienstag', wed: 'Mittwoch', thu: 'Donnerstag',
      fri: 'Freitag', sat: 'Samstag', sun: 'Sonntag',
    }
    try {
      const bundle = JSON.parse(json)
      const entries: any[] = bundle.entry || []
      const availableTimes: any[] = []
      const notAvailable: any[] = []

      for (const e of entries) {
        const r = e.resource
        if (!r) continue
        if (r.resourceType === 'PractitionerRole' || r.resourceType === 'HealthcareService') {
          if (r.availableTime) availableTimes.push(...r.availableTime)
          if (r.notAvailable) notAvailable.push(...r.notAvailable)
        } else if (r.resourceType === 'Location') {
          for (const h of r.hoursOfOperation || []) {
            availableTimes.push({
              daysOfWeek: h.daysOfWeek,
              allDay: h.allDay,
              availableStartTime: h.openingTime,
              availableEndTime: h.closingTime,
            })
          }
        }
      }

      // Only build the full week view when there is at least some data
      if (availableTimes.length > 0) {
        const daySlots: Record<string, { allDay: boolean; startTime?: string; endTime?: string }[]> = {}
        for (const t of availableTimes) {
          for (const d of (t.daysOfWeek as string[] || [])) {
            if (!daySlots[d]) daySlots[d] = []
            daySlots[d].push({
              allDay: !!t.allDay,
              startTime: (t.availableStartTime as string | undefined)?.substring(0, 5),
              endTime: (t.availableEndTime as string | undefined)?.substring(0, 5),
            })
          }
        }
        this.fhirDaySchedule.set(dayOrder.map(d => ({
          day: dayLabels[d],
          closed: !daySlots[d] || daySlots[d].length === 0,
          slots: daySlots[d] || [],
        })))
      } else {
        this.fhirDaySchedule.set([])
      }

      this.fhirNotAvailable = notAvailable.map(n => ({
        description: n.description as string,
        from: n.during?.start as string | undefined,
        to: n.during?.end as string | undefined,
      }))
    } catch {
      this.fhirDaySchedule.set([])
      this.fhirNotAvailable = []
    }
  }
}
