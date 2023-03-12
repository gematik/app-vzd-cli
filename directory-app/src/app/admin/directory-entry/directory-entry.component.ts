import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SnippetType } from 'carbon-components-angular/code-snippet/code-snippet.component';
import { AdminBackendService } from 'src/services/admin/admin-backend.service';
import { Coding, ElaborateDirectoryEntry } from 'src/services/admin/admin.model';

interface KIMAddressInfo {
  mail: string
  provider: string | undefined
}

interface UserCertificateInfo {
  iconName: string,
  iconClass: string,
  serialNumber: string
  issuer: string
  notBefore: string
  notAfter: string
  algorithm: string
}

@Component({
  selector: 'app-admin-directory-entry',
  templateUrl: './directory-entry.component.html',
  styleUrls: ['./directory-entry.component.scss']
})
export class DirectoryEntryComponent implements OnInit {
  env!: string
  queryString: string | null = null
  entry?: ElaborateDirectoryEntry
  get rawData(): string { return JSON.stringify(this.entry, null, 2)}
  kimAddressList: KIMAddressInfo[] = []
  userCertificateList: UserCertificateInfo[] = []
  snippetDisplay = "multi" as SnippetType

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private adminBackend: AdminBackendService,
    private changeDetector: ChangeDetectorRef,
  ) { }

  ngOnInit(): void {
    this.route.params.subscribe(param => {
      this.queryString = param['q']
      const telematikID = param['id']
      this.env = param['env']
      this.adminBackend.loadEntry(this.env, telematikID).then(
        value => {
          this.entry = value
          this.userCertificateList = this.createUserCertificateList(value)
          this.changeDetector.detectChanges()
        }
      )
    })
  }

  protected get domainIDText() {
    var text = ""
    this.entry?.base.domainID?.forEach(id => text += `${id}\n`)
    return text
  }

  protected codeIconName(code: Coding): string {
    if (code.display != code.code) {
     return "checkmark--filled"
    } else {
      return "error--filled"
    }
  }

  protected codeIconClass(code: Coding): string {
    if (code.display != code.code) {
     return "success"
    } else {
      return "error"
    }
  }

  private createUserCertificateList(entry: ElaborateDirectoryEntry): UserCertificateInfo[] {
    return entry.userCertificates?.map( certBlock => {
      const cert = certBlock.userCertificate 
      if (cert === undefined) {
        return undefined
      }
      const regex = /O=([^,]*)/;
      var issuer = ""
      const found = cert.issuer.match(regex);
      if (found?.length == 2) {
        issuer = found[1]
      }

      return {
        iconName: certBlock.active ? "checkmark--filled" : "warning--filled",
        iconClass: certBlock.active ? "success" : "warning",
        serialNumber: cert.serialNumber,
        issuer: issuer,
        notBefore: cert.notBefore.substring(0, 10),
        notAfter: cert.notAfter.substring(0, 10),
        algorithm: cert.publicKeyAlgorithm
      }
    }).filter( x => x !== undefined) as UserCertificateInfo[] || []
  }
}
