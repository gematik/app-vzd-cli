import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, firstValueFrom, Observable, of, tap } from 'rxjs';
import { MessageService } from '../message.service';
import { AdminStatus, DirectoryEntry, SearchResults } from './admin.model';

@Injectable({
  providedIn: 'root'
})
export class AdminBackendService {

  constructor(
    private http: HttpClient,
    private messagingService: MessageService,   
  ) { 
  }

  getStatus() {
    return this.http.get<AdminStatus>('/api/admin/status')
  }

  search(env: string, queryString: string) : Promise<SearchResults> {
    return firstValueFrom(
      this.http.get<SearchResults> (
        `/api/admin/${env}/search`, 
        { params: {"q": queryString} }
      )
    )
  }

  loadEntry(env: string, telematikID: string) : Promise<DirectoryEntry> {
    return firstValueFrom(
      this.http.get<DirectoryEntry>(
        `/api/admin/${env}/entry/${telematikID}`
      )
    )
  }
}
