import { Component, OnInit } from '@angular/core';
import { Title } from "@angular/platform-browser";
import { Router } from '@angular/router';
import { NavigationLink, NavigationService } from "../services/navigation.service";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {

  constructor(
    private titleService: Title,
    private router: Router,
    public navigationService: NavigationService
  ) {

  }

  public adminMenuLinks: NavigationLink[] = [
  ]

  public isActive(link: string) {
    return this.router.url.startsWith(link)
  }

  public ngOnInit(): void {
    this.titleService.setTitle("Directory")
    this.navigationService.getAdminMenuLinks()
      .subscribe(links => {
        this.adminMenuLinks = links
      });
  }

}
