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

import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { GlobalConfig } from './global.model';
import { firstValueFrom } from 'rxjs';
import { APP_BASE_HREF } from '@angular/common';
import { Outcome } from './global.model';

@Injectable({
  providedIn: 'root',
})
export class BackendService {
  constructor(
    private http: HttpClient,
    @Inject(APP_BASE_HREF) private baseHref: string,
  ) {}

  getConfig() {
    return this.http.get<GlobalConfig>(`${this.baseHref}api/config`);
  }

  updateConfig(config: GlobalConfig) {
    return firstValueFrom(
      this.http.post<GlobalConfig>(`${this.baseHref}api/config`, config),
    );
  }

  loginUsingVault(env: string, vaultPassword: string) {
    return firstValueFrom(
      this.http.post<Outcome>(`${this.baseHref}api/admin/login`, {
        env: env,
        vaultPassword: vaultPassword,
      }),
    );
  }
}
