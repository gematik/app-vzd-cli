import { Component, Inject, OnInit } from '@angular/core';
import { BaseModal, ModalButtonType, ModalService } from 'carbon-components-angular';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-add-token',
  templateUrl: './add-token.component.html',
  styleUrls: ['./add-token.component.scss']
})
export class AddTokenComponent extends BaseModal {
  public token: string = ""

  constructor(
    @Inject("afterAddToken") public afterAddToken: () => void,
    protected modalService: ModalService
  ) {
    super()
  }

  addToken() {
    this.afterAddToken()
    this.closeModal()
  }

}
