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
