import { Component, Input, OnInit } from '@angular/core';
import { CertificateInfo, ElaborateDirectoryEntry, Smartcard, UserCertificate } from 'src/services/admin/admin.model';

@Component({
  selector: 'app-admin-directory-entry-smartcard',
  templateUrl: './smartcard.component.html',
  styleUrls: ['./smartcard.component.scss']
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
