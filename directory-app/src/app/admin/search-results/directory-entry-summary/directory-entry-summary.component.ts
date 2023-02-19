import { Component, Input, OnInit } from '@angular/core';
import { CodeableConcept, ElaborateDirectoryEntry } from 'src/services/admin/admin.model';

@Component({
  selector: 'app-admin-directory-entry-summary',
  templateUrl: './directory-entry-summary.component.html',
  styleUrls: ['./directory-entry-summary.component.scss']
})
export class DirectoryEntrySummaryComponent implements OnInit {
  @Input() model!: ElaborateDirectoryEntry
  idLabel = ""
  idValue = ""
  showTelematikID = true

  constructor() { }

  ngOnInit(): void {
    this.idLabel = "TelematikID"
    this.idValue = this.model.base.telematikID
  }

  onIDClick() {
    this.showTelematikID = !this.showTelematikID
    if (this.showTelematikID) {
      this.idLabel = "TelematikID"
      this.idValue = this.model.base.telematikID  
    } else {
      this.idLabel = "UUID"
      this.idValue = this.model.base.dn?.uid || ""
    }
  }

  codeListDisplay(codeList: [CodeableConcept] | undefined) {
    return codeList?.map(code => code.display).join(", ")
  }

}
