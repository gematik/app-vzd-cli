/*
 * Copyright (Change Date see Readme), gematik GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 */

import { ApplicationRef, Component, OnInit, signal } from '@angular/core';
import {
  InlineLoadingState,
  ModalService,
  NotificationService,
} from 'carbon-components-angular';
import { BackendService } from 'src/services/backend.service';
import { AdminBackendService } from 'src/services/admin/admin-backend.service';
import { AskPasswordComponent } from './ask-password/ask-password.component';
import { IconService } from 'carbon-components-angular';
import { Login16, Api16 } from '@carbon/icons';
interface EnvironmentStatusModel {
  env: string;
  status: 'success' | 'error';
  title: string;
  iconName: string;
  iconClass: string;
}

@Component({
  selector: 'app-admin-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss'],
  providers: [ModalService, NotificationService],
  standalone: false,
})
export class SettingsComponent implements OnInit {
  protected statusModel = signal<EnvironmentStatusModel[]>([]);
  protected loadingState = signal(InlineLoadingState.Hidden);

  constructor(
    private iconService: IconService,
    private backendService: BackendService,
    private adminBackend: AdminBackendService,
    private notificationService: NotificationService,
    private modalService: ModalService,
    private appRef: ApplicationRef,
  ) {}

  ngOnInit(): void {
    this.iconService.register(Login16);
    this.iconService.register(Api16);
    const self = this;
    this.adminBackend.updateStatus();
    this.adminBackend.status$.subscribe({
      next(adminStatus) {
        self.statusModel.set(adminStatus.environmentStatus.map((envStatus) => {
          if (envStatus.accessible) {
            return {
              env: envStatus.env,
              status: 'success',
              title: self.adminBackend.getEnvLabel(envStatus.env),
              iconName: 'checkmark--filled',
              iconClass: 'success',
            };
          } else {
            return {
              env: envStatus.env,
              status: 'error',
              title: self.adminBackend.getEnvLabel(envStatus.env),
              iconName: 'error--filled',
              iconClass: 'error',
            };
          }
        }));
      },
      error(err) {
        self.showError(err);
      },
      complete() {},
    });
  }

  showError(err: any) {
    this.notificationService.showNotification({
      type: 'error',
      title: 'Error',
      message: err.error?.message || err.message,
      target: '.notification-container',
    });
  }

  onLogin(env: string) {
    this.modalService.create({
      component: AskPasswordComponent,
      inputs: {
        prompt: `Einloggen in die ${this.adminBackend.getEnvLabel(env)}`,
        passwordCallback: (password: string) => {
          this.loadingState.set(InlineLoadingState.Active);
          this.backendService
            .loginUsingVault(env, password)
            .then(() => {
              this.adminBackend.updateStatus();
              this.loadingState.set(InlineLoadingState.Finished);
              this.statusModel.update((list) =>
                list.map((e): EnvironmentStatusModel =>
                  e.env == env
                    ? {
                        ...e,
                        iconName: 'checkmark--filled',
                        iconClass: 'success',
                        status: 'success',
                      }
                    : e,
                ),
              );
            })
            .catch((err) => {
              this.loadingState.set(InlineLoadingState.Error);
              this.showError(err);
            });
        },
      },
    });
    // Carbon opens the modal via an internal setTimeout; force a render so it appears.
    setTimeout(() => this.appRef.tick());
  }
}
