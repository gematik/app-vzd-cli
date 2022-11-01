import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, firstValueFrom, interval, map, Observable, of, tap } from 'rxjs';
import { MessageService } from '../message.service';
import { AdminStatus, DirectoryEntry, SearchResults } from './admin.model';

// TODO: how does one provide labels in Angular?
const labels: Record<string, string> = {
  "ru": "Referenzumgebung",
  "tu": "Testumgebung",
  "pu": "Produktivumgebung"
}

@Injectable({
  providedIn: 'root'
})
export class AdminBackendService {
  private adminStatus: AdminStatus | undefined

  constructor(
    private http: HttpClient,
  ) { 
    const self = this

    this.getStatus().then( adminStatus => {
      this.adminStatus = adminStatus
      setInterval( () => {
        this.getStatus().then( adminStatus => this.adminStatus = adminStatus)
      }, 1000)
    })
  }

  get status$() : Observable<AdminStatus> {
    const self = this
    return new Observable(function subscribe(subscriber) {
      if (self.adminStatus != undefined) {
        subscriber.next(self.adminStatus)
      }
      const id = setInterval(() => {
        if (self.adminStatus != undefined) {
          subscriber.next(self.adminStatus)
        }
      }, 500);
    });
  }

  getStatus() {
    return firstValueFrom(
      this.http.get<AdminStatus>('/api/admin/status')
    )
  }

  getEnvLabel(env: string) {
    return labels[env]
  }

  search(env: string, queryString: string) {
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
