import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { StatusRepresentation } from './global.model';

@Injectable({
  providedIn: 'root'
})
export class BackendService {

  constructor(
    private http: HttpClient    
  ) { }

  getStatus() {
    return this.http.get<StatusRepresentation>('/api/status')    
  }
}
