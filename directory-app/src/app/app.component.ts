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
