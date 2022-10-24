import { Component, OnInit } from '@angular/core';
import { Title } from "@angular/platform-browser";
import { NavigationLink, NavigationService } from "../services/navigation.service";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {

  constructor(
    private titleService: Title,
    public navigationService: NavigationService
  ) {

  }

  public adminMenuLinks: NavigationLink[] = [
  ]

  public ngOnInit(): void {
    this.titleService.setTitle("Directory")
    this.navigationService.getAdminMenuLinks()
      .subscribe(links => this.adminMenuLinks = links);
  }

}
