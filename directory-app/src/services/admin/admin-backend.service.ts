import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AdminStatus, DirectoryEntry } from './admin.model';

@Injectable({
  providedIn: 'root'
})
export class AdminBackendService {

  constructor(
    private http: HttpClient    
  ) { 
  }

  getStatus() {
    return this.http.get<AdminStatus>('/api/admin/status')
  }

  search(env: string, queryString: string) {
    return this.http.get<DirectoryEntry[]>(`/api/admin/${env}/search`, { params: {"q": queryString}})
  }

}
