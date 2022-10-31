import { Component, Input, OnInit } from '@angular/core';
import { ModalService } from 'carbon-components-angular';
import { NavigationService } from '../../services/navigation.service';
import { AddTokenComponent } from './add-token/add-token.component';


@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent implements OnInit {

  constructor(protected modalService: ModalService,
    protected navigationService: NavigationService) { }

  ngOnInit(): void {
    console.log("Init")
  }

  onAddToken() {
    this.modalService.create({
      component: AddTokenComponent,
      inputs: {
        afterAddToken: () => {
          alert()
			  }      
      }
    })
  }

}
