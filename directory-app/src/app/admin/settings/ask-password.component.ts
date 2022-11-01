import { Component, Inject, OnInit } from '@angular/core';
import { BaseModal, ModalButtonType, ModalService } from 'carbon-components-angular';
import { Observable } from 'rxjs';

@Component({
  selector: 'admin-settings-ask-password',
  template: `<p>Password, please</p>`,
})
export class AskPasswordComponent extends BaseModal {
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
