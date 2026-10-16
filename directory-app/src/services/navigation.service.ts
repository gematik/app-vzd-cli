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
