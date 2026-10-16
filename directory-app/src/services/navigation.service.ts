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

import { Injectable, signal } from '@angular/core';
import { map, Observable } from 'rxjs';
import { AdminBackendService } from './admin/admin-backend.service';
import { BackendService } from './backend.service';

export interface NavigationLink {
  title: string,
  route: string
}

@Injectable({
  providedIn: 'root'
})
export class NavigationService {
  public adminMenuLinks$: Observable<NavigationLink[]>
  public settingsAvailable = signal(false)

  constructor(
    private adminBackend: AdminBackendService,
    private backendService: BackendService
  ) { 
    const self = this

    this.adminMenuLinks$ = this.adminBackend.status$.pipe(
      map( adminStatus => {
        return adminStatus.environmentStatus.map((envStatus) => {
          if (envStatus.accessible) {
            return <NavigationLink>{title: this.adminBackend.getEnvLabel(envStatus.env), route: `/admin/${envStatus.env}`}
          } 
          return null
        }).filter( (x): x is NavigationLink => x != null ).reverse()
      }) 
    ) 

    this.backendService.getConfig().subscribe({
      next(config) {
        self.settingsAvailable.set(true)
      }
    })
  }

  update() {
    this.adminBackend.updateStatus()
  }
  
}
