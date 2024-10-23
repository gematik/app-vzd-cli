import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { NavigationService } from 'src/services/navigation.service';

@Component({
  selector: 'app-main',
  template: '',
})
export class MainComponent implements OnInit {

  constructor(
    private navigationService: NavigationService,
    private router: Router,
  ) { }

  ngOnInit(): void {
    this.navigationService.adminMenuLinks$.subscribe(links => {
      if (links.length > 0) {
        this.router.navigate([links[0].route])
      } else {
        this.router.navigate(["settings"])
      }
    })
  }
}
