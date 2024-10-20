import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { AdminBackendService } from './admin/admin-backend.service';

export interface NavigationLink {
  title: string,
  route: string
}

@Injectable({
  providedIn: 'root'
})
export class NavigationService {
  //public adminMenuLinks$: Observable<NavigationLink[]> = new Observable<NavigationLink[]>()

  constructor(
    private adminBackend: AdminBackendService
  ) { 
    const self = this

    /*
    this.adminMenuLinks$ = this.adminBackend.status$.pipe(
      map( adminStatus => {
        return adminStatus.environmentStatus.map((envStatus) => {
          if (envStatus.accessTokenClaims != null) {
            return <NavigationLink>{title: this.adminBackend.getEnvLabel(envStatus.env), route: `/admin/${envStatus.env}`}
          } 
          return null
        }).filter( (x): x is NavigationLink => x != null ).reverse()
      })
    )
      */
  }

  get adminMenuLinks$(): Observable<NavigationLink[]> {
    return new Observable<NavigationLink[]>(subscriber => {
        subscriber.next([])
    })
  }
  
}
