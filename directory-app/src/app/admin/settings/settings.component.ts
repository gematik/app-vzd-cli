import { Component, OnInit } from '@angular/core';
import { InlineLoadingState, ModalService, NotificationService } from 'carbon-components-angular';
import { AdminBackendService } from 'src/services/admin/admin-backend.service';
import { AskPasswordComponent } from './ask-password/ask-password.component';

interface EnvironmentStatusModel {
  env: string,
  status: "success" | "error"
  title: string,
  iconName: string,
  iconClass: string
}

@Component({
  selector: 'app-admin-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent implements OnInit {
  protected statusModel: EnvironmentStatusModel[] = []
  protected loadingState = InlineLoadingState.Hidden

  constructor(
    private adminBackend: AdminBackendService,
    private notificationService: NotificationService,
    private modalService: ModalService,
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
              env: envStatus.env,
              status: "success",
              title: self.adminBackend.getEnvLabel(envStatus.env),
              iconName: "checkmark--filled",
              iconClass: "success",
            }
          } else {
            return { 
              env: envStatus.env,
              status: "error",
              title: self.adminBackend.getEnvLabel(envStatus.env),
              iconName: "error--filled",
              iconClass: "error",
            }
          }
        })
      },
      error(err) {
        self.showError(err)
      },
      complete() {
      },
    })
  }

  showError(err: any) {
    this.notificationService.showNotification({
      type: "error",
      title: "Error",
      message: err.message,
      target: ".notification-container",
    })  
  }

  onLogin(env: string) {
    this.modalService.create({
      component: AskPasswordComponent,
      inputs: {
        prompt: `Einloggen in die ${this.adminBackend.getEnvLabel(env)}`,
        passwordCallback: (password: string) => {
          this.loadingState = InlineLoadingState.Active
          this.adminBackend.loginUsingVault(env, password)
            .then( () => {
              this.loadingState = InlineLoadingState.Finished
              const model = this.statusModel.find( (e) => e.env == env)
              model!.iconName = "checkmark--filled"
              model!.iconClass = "success"
              model!.status = "success"
            })
            .catch( err => {
              this.loadingState = InlineLoadingState.Error
              this.showError(err)
            })
			  }      
      }
    })

  }

}
