import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { GlobalConfig } from './global.model';
import { firstValueFrom } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class BackendService {

  constructor(
    private http: HttpClient    
  ) { }

  getConfig() {
    return this.http.get<GlobalConfig>('/api/config')
  }

  updateConfig(config: GlobalConfig) {
    return firstValueFrom(this.http.post<GlobalConfig>('/api/config', config))
  }
}
