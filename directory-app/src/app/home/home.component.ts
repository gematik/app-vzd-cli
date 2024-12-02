import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { first, firstValueFrom } from 'rxjs';
import { NavigationService } from 'src/services/navigation.service';

@Component({
  selector: 'app-home',
  template: '',
})
export class HomeComponent implements OnInit {

  constructor(
    private navigationService: NavigationService,
    private router: Router,
  ) { }

  ngOnInit() {
    firstValueFrom(this.navigationService.adminMenuLinks$).then(links => {
      if (links.length > 0) {
        this.router.navigate([links[0].route])
      } else {
        this.router.navigate(["settings"])
      }
    })
  }
}
