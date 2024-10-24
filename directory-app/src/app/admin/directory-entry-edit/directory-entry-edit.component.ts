import { Component, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import {
  BreadcrumbModule,
  GridModule,
  TabsModule,
  ButtonModule,
  InputModule,
  NotificationContent,
  IconModule,
  NotificationModule,
  ModalService,
  ModalModule,
  AlertModalType,
  ModalButtonType,
  PlaceholderModule,
} from 'carbon-components-angular';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AdminBackendService } from 'src/services/admin/admin-backend.service';
import { BaseDirectoryEntry } from 'src/services/admin/admin.model';
import { FormsModule } from '@angular/forms';
@Component({
  selector: 'app-directory-entry-edit',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    GridModule,
    BreadcrumbModule,
    TabsModule,
    ButtonModule,
    InputModule,
    FormsModule,
    IconModule,
    NotificationModule,
    ModalModule,
    PlaceholderModule,
  ],
  templateUrl: './directory-entry-edit.component.html',
  styleUrl: './directory-entry-edit.component.scss',
  providers: [ModalService],
})
export class DirectoryEntryEditComponent implements OnInit {
  constructor(
    private router: Router,
    private route: ActivatedRoute,
    protected adminBackend: AdminBackendService,
    protected modalService: ModalService,
  ) { }

  env: string | undefined
  baseEntry: BaseDirectoryEntry | undefined
  queryString: string | null = null
  toggleActive: boolean = true
  domainIDFlat: string | undefined
  holderFlat: string | undefined
  metaFlat: string | undefined

  globalNotification: NotificationContent | null = null
  modifyNotification: NotificationContent | null = null
  dangerNotification: NotificationContent | null = null

  ngOnInit(): void {
    this.route.params.subscribe(param => {
      this.queryString = param['q']
      const telematikID = param['id']
      this.env = param['env']

      // mock base entry
      this.adminBackend.loadBaseEntry(this.env!, telematikID).then(
        value => {
          this.baseEntry = value
          this.domainIDFlat = this.baseEntry?.domainID?.join("\n")
          this.holderFlat = this.baseEntry?.holder?.join("\n")
          this.metaFlat = this.baseEntry?.meta?.join("\n")
        }
      ).catch(
        error => {
          this.globalNotification = {
            lowContrast: true,
            type: "error",
            title: "Fehler",
            message: error.message,
          }
        }
      )
    })
  }

  onSave(): void {
    // split textareay to arrays by comma or new line, trimming the whitespaces
    this.baseEntry!.domainID = this.domainIDFlat?.split(/,|\n/).map((item) => item.trim())
    this.baseEntry!.holder = this.holderFlat?.split(/,|\n/).map((item) => item.trim())
    this.baseEntry!.meta =this.metaFlat?.split(/,|\n/).map((item) => item.trim())

    this.adminBackend.modifyBaseEntry(this.env!, this.baseEntry!).then(
      value => {
        this.router.navigate(
          ['entry', this.baseEntry?.telematikID, {"q": this.queryString}],
          { relativeTo: this.route.parent, queryParams: {modified: 'true'} }
        )    
      }
    ).catch(
      error => {
        this.modifyNotification = {
          lowContrast: true,
          type: "error",
          title: "Fehler",
          message: error.message,
        }
      }
    )
    
  }
  
  onCancel(): void {
    this.router.navigate(
      ['entry', this.baseEntry?.telematikID, {"q": this.queryString}],
      { relativeTo: this.route.parent }
    )
  }

  onDelete(): void {
    const ref = this.modalService.show({
      type: AlertModalType.danger,
			title: "Löschen bestätigen",
			content: `<p>${this.baseEntry?.telematikID}</p><p>${this.baseEntry?.displayName}</p>`,      
			size: "md",
			buttons: [{
				text: "Nein, abbrechen",
				type: ModalButtonType.secondary
			}, {
				text: "Ja, löschen",
				type: ModalButtonType.danger,
				click: () => this.doDelete()
			}]
		})
  }

  doDelete() {
    this.adminBackend.deleteEntry(this.env!, this.baseEntry!.telematikID).then(
      value => {
        this.router.navigate(
          [''],
          { relativeTo: this.route.parent }
        )
      }
    ).catch(
      error => {
        this.dangerNotification = {
          lowContrast: true,
          type: "error",
          title: "Fehler",
          message: error.message,
        }
      }
    )
  }

  onDeactivate(): void {
    const ref = this.modalService.show({
      type: AlertModalType.danger,
			title: "Deaktivieren bestätigen",
			content: `<p>${this.baseEntry?.telematikID}</p><p>${this.baseEntry?.displayName}</p>`,      
			size: "md",
			buttons: [{
				text: "Nein, abbrechen",
				type: ModalButtonType.secondary
			}, {
				text: "Ja, deaktivieren",
				type: ModalButtonType.danger,
				click: () => this.doDeactivate()
			}]
		})
  }

  doDeactivate() {
    this.adminBackend.deactivateEntry(this.env!, this.baseEntry!.telematikID).then(
      value => {
        this.baseEntry!.active = false
        this.dangerNotification = {
          lowContrast: true,
          type: "success",
          title: "Erfolg",
          message: "Der Eintrag wurde erfolgreich deaktiviert.",
        }
      }
    ).catch(
      error => {
        this.dangerNotification = {
          lowContrast: true,
          type: "error",
          title: "Fehler",
          message: error.message,
        }
      }
    )
  }

  onActivate(): void {
    const ref = this.modalService.show({
      type: AlertModalType.danger,
      title: "Aktivieren bestätigen",
      content: `<p>${this.baseEntry?.telematikID}</p><p>${this.baseEntry?.displayName}</p>`,      
      size: "md",
      buttons: [{
        text: "Nein, abbrechen",
        type: ModalButtonType.secondary
      }, {
        text: "Ja, aktivieren",
        type: ModalButtonType.danger,
        click: () => this.doActivate()
      }]
    })
  }

  doActivate(): void {
    this.adminBackend.activateEntry(this.env!, this.baseEntry!.telematikID).then(
      value => {
        this.baseEntry!.active = true
        this.dangerNotification = {
          lowContrast: true,
          type: "success",
          title: "Erfolg",
          message: "Der Eintrag wurde erfolgreich aktiviert.",
        }
      }
    ).catch(
      error => {
        this.dangerNotification = {
          type: "error",
          title: "Fehler",
          message: error.message,
          target: ".notification-container-danger",
        }
      }
    )
  }

  get entryType(): string {
    return this.baseEntry?.entryType?.join(",") ?? ""
  }

  set entryType(value: string) {
    this.baseEntry!.entryType = value.split(",").map((item) => item.trim())
  }
}
