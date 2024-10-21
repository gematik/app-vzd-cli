import { Component, OnInit, ViewContainerRef } from '@angular/core';
import { Title } from "@angular/platform-browser";
import { Router } from '@angular/router';
import { NavigationLink, NavigationService } from "../services/navigation.service";
import { IconService } from 'carbon-components-angular';
import { Settings24 } from "@carbon/icons";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {

  constructor(
    private iconsService: IconService,
    private titleService: Title,
    private router: Router,
    public navigationService: NavigationService
  ) {

  }

  public adminMenuLinks: NavigationLink[] = []

  public isActive(link: string) {
    return this.router.url.startsWith(link)
  }

  public ngOnInit(): void {
    this.titleService.setTitle("gematik Directory")
    this.iconsService.register(Settings24)
    /*
    this.navigationService.adminMenuLinks$
      .subscribe(links => {
        this.adminMenuLinks = links
      });
      */
  }

}