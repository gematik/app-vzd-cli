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

import { Component, OnInit, signal } from '@angular/core';
import { InlineLoadingState } from 'carbon-components-angular';
import { BackendService } from 'src/services/backend.service';
import { GlobalConfig } from 'src/services/global.model';
import { NavigationService } from '../../services/navigation.service';
import { Router } from '@angular/router';
@Component({
    selector: 'app-settings',
    templateUrl: './settings.component.html',
    styleUrls: ['./settings.component.scss'],
    standalone: false
})
export class SettingsComponent implements OnInit {
  protected config = signal<GlobalConfig | undefined>(undefined)
  protected configState = signal(InlineLoadingState.Hidden)

  constructor(
    protected backendService: BackendService,
    protected navigationService: NavigationService,
    protected router: Router,
    ) { }

  ngOnInit(): void {
    this.backendService.getConfig().subscribe( it => {
      this.config.set(it)
    })
  }

  updateConfig() {
    this.backendService.updateConfig(this.config()!)
      .then( config => {
        this.config.set(config)
        this.configState.set(InlineLoadingState.Finished)
      })
      .catch(err => {
        this.configState.set(InlineLoadingState.Error)
        console.error(err)
      })
  }

  tabSelected() {
    this.configState.set(InlineLoadingState.Hidden)
  }
}
