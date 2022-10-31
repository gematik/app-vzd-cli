import { Injectable } from '@angular/core';
import {  map, Observable, of } from 'rxjs';
import { AdminBackendService } from './admin/admin-backend.service';

export interface NavigationLink {
  title: string,
  route: string
}

// TODO: how does one provide labels in Angular?
const labels: Record<string, string> = {
  "ru": "Referenzumgebung",
  "tu": "Testumgebung",
  "pu": "Produktivumgebung"
}

@Injectable({
  providedIn: 'root'
})
export class NavigationService {

  constructor(
    private adminBackend: AdminBackendService
  ) { 
    
  }

  getAdminMenuLinks(): Observable<NavigationLink[]> {
    return this.adminBackend.getStatus().pipe(map((status) => {
      return status.environmentStatus.map((envStatus) => {
        if (envStatus.accessTokenClaims != null) {
          return <NavigationLink>{title: labels[envStatus.env], route: `/admin/${envStatus.env}`}
        } 
        return null
      }).filter( (x): x is NavigationLink => x != null ).reverse()
    }))    
  }
  
}
