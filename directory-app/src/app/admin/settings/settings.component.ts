import { Component, OnInit } from '@angular/core';
import { NotificationService } from 'carbon-components-angular';
import { AdminBackendService } from 'src/services/admin/admin-backend.service';
import { AdminEnvironmentStatus, AdminStatus } from 'src/services/admin/admin.model';

interface EnvironmentStatusModel {
  title: string,
  iconName: string,
  iconClass: string
}

@Component({
  selector: 'admin-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent implements OnInit {
  protected statusModel: EnvironmentStatusModel[] = []

  constructor(
    protected adminBackend: AdminBackendService,
    private notificationService: NotificationService,
  ) { }

  ngOnInit(): void {
    const self = this
    this.adminBackend.status$.subscribe({
      next(adminStatus) {
        self.statusModel = adminStatus.environmentStatus
          .filter ( envStatus => envStatus.env != "tu")
          .map( envStatus => {
          if (envStatus.accessTokenClaims != null) {
            return { 
              title: self.adminBackend.getEnvLabel(envStatus.env),
              iconName: "checkmark--filled",
              iconClass: "success",
            }
          } else {
            return { 
              title: self.adminBackend.getEnvLabel(envStatus.env),
              iconName: "error--filled",
              iconClass: "error",
            }
          }
        })
      },
      error(err) {
        self.notificationService.showNotification({
          type: "error",
          title: "Error",
          message: err.message,
          target: ".notification-container",
        })  
        },
      complete() {
      },
    })
  }


}
