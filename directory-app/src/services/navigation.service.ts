import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { AdminBackendService } from './admin/admin-backend.service';
import { BackendService } from './backend.service';

export interface NavigationLink {
  title: string,
  route: string
}

@Injectable({
  providedIn: 'root'
})
export class NavigationService {
  public adminMenuLinks$: Observable<NavigationLink[]> 
  public settingsAvailable = false

  constructor(
    private adminBackend: AdminBackendService,
    private backendService: BackendService
  ) { 
    const self = this

    this.adminMenuLinks$ = this.adminBackend.status$.pipe(
      map( adminStatus => {
        return adminStatus.environmentStatus.map((envStatus) => {
          if (envStatus.accessible) {
            return <NavigationLink>{title: this.adminBackend.getEnvLabel(envStatus.env), route: `/admin/${envStatus.env}`}
          } 
          return null
        }).filter( (x): x is NavigationLink => x != null ).reverse()
      }) 
    ) 

    this.backendService.getConfig().subscribe({
      next(config) {
        self.settingsAvailable = true
      }
    })
  }

  update() {
    this.adminBackend.updateStatus()
  }
  
}
