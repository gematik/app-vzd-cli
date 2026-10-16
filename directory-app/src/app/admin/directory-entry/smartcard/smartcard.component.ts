/*
 * Copyright (Change Date see Readme), gematik GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
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
