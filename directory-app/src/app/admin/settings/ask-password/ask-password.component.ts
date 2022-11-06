import { AfterViewInit, Component, ElementRef, Inject, Input, OnInit, ViewChild } from '@angular/core';
import { BaseModal, Label, ModalService } from 'carbon-components-angular';

@Component({
  selector: 'app-admin-settings-ask-password',
  templateUrl: './ask-password.component.html',
  styleUrls: ['./ask-password.component.scss']
})
export class AskPasswordComponent extends BaseModal implements AfterViewInit {
  protected password: string = ""
  @ViewChild(Label) inputField?: Label

  constructor(
    @Inject("prompt") public prompt: string,
    @Inject("passwordCallback") public passwordCallback: (password: string) => void,
    protected modalService: ModalService
  ) {
    super()
  }

  ngAfterViewInit(): void {
    setTimeout(()=>{
      // doesnt work for some reason
      this.inputField?.wrapper.nativeElement.querySelector("input")?.focus()
    },0);  
  }

  returnPassword() {
    this.passwordCallback(this.password)
    this.closeModal()
  }

}

