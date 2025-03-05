import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { GlobalConfig } from './global.model';
import { firstValueFrom } from 'rxjs';
import { APP_BASE_HREF } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class BackendService {

  constructor(
    private http: HttpClient,
    @Inject(APP_BASE_HREF) private baseHref: string    
  ) { }

  getConfig() {
    return this.http.get<GlobalConfig>(`${this.baseHref}api/config`)
  }

  updateConfig(config: GlobalConfig) {
    return firstValueFrom(this.http.post<GlobalConfig>(`${this.baseHref}api/config`, config))
  }
}
