import { Component, Input, OnInit } from '@angular/core';
import { NavigationService } from '../../services/navigation.service';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent implements OnInit {

  constructor(
    protected navigationService: NavigationService) { }

  ngOnInit(): void {
    console.log("Init")
  }

}
