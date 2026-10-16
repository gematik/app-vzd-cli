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
import { Title } from "@angular/platform-browser";
import { Router } from '@angular/router';
import { NavigationLink, NavigationService } from "../services/navigation.service";
import { IconService } from 'carbon-components-angular';
import { Settings24 } from "@carbon/icons";

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    standalone: false
})
export class AppComponent implements OnInit {

  constructor(
    private iconsService: IconService,
    private titleService: Title,
    public router: Router,
    public navigationService: NavigationService
  ) {

  }

  public adminMenuLinks = signal<NavigationLink[]>([])

  public isActive(link: string) {
    return this.router.url.startsWith(link)
  }

  public isSettingsAvailable() {
    return this.navigationService.settingsAvailable()
  }

  public ngOnInit(): void {
    this.navigationService.update()
    this.titleService.setTitle("gematik Directory")
    this.iconsService.register(Settings24)
    this.navigationService.adminMenuLinks$
      .subscribe(links => {
        this.adminMenuLinks.set(links)
      });
  }
  
}
