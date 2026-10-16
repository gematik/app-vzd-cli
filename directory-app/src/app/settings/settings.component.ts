/*
Copyright 2026, gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*******

For additional notes and disclaimer from gematik and in case of changes
by gematik, find details in the "Readme" file.
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
