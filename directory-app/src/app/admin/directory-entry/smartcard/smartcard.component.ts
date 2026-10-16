/*
Copyright 2026, gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*******

For additional notes and disclaimer from gematik and in case of changes
by gematik, find details in the "Readme" file.
*/

import { Component, Input, OnInit, Pipe, PipeTransform } from '@angular/core';
import { CertificateInfo, ElaborateDirectoryEntry, Smartcard, UserCertificate } from 'src/services/admin/admin.model';

@Component({
    selector: 'app-admin-directory-entry-smartcard',
    templateUrl: './smartcard.component.html',
    styleUrls: ['./smartcard.component.scss'],
    standalone: false
})
export class SmartcardComponent implements OnInit {
  @Input() entry!: ElaborateDirectoryEntry
  @Input() smartcard!: Smartcard
  protected certificateInfoList: CertificateInfo[] = [] 

  constructor() { }

  ngOnInit(): void {
    this.certificateInfoList = this.entry.userCertificates
      ?.filter(cert => this.smartcard.certificateSerialNumbers.includes(cert.userCertificate!!.serialNumber))
      .map(cert => cert.userCertificate) as CertificateInfo[] || []
  }

}
