import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { APP_BASE_HREF } from '@angular/common';

@Injectable({
  providedIn: 'root',
})
export class FhirBackendService {
  constructor(
    private http: HttpClient,
    @Inject(APP_BASE_HREF) private baseHref: string,
  ) {}

  loadEntry(env: string, telematikID: string): Promise<string> {
    return firstValueFrom(
      this.http.get(
        `${this.baseHref}api/fhir/${env}/entry/${telematikID}`,
        { responseType: 'text' },
      ),
    );
  }
}
