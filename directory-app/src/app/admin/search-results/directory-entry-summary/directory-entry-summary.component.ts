import { Component, Input, OnInit } from '@angular/core';
import { DirectoryEntry } from 'src/services/admin/admin.model';

@Component({
  selector: 'app-admin-directory-entry-summary',
  templateUrl: './directory-entry-summary.component.html',
  styleUrls: ['./directory-entry-summary.component.scss']
})
export class DirectoryEntrySummaryComponent implements OnInit {
  @Input() model!: DirectoryEntry
  idLabel = ""
  idValue = ""
  showTelematikID = true

  constructor() { }

  ngOnInit(): void {
    this.idLabel = "TelematikID"
    this.idValue = this.model.DirectoryEntryBase.telematikID
  }

  onIDClick() {
    this.showTelematikID = !this.showTelematikID
    if (this.showTelematikID) {
      this.idLabel = "TelematikID"
      this.idValue = this.model.DirectoryEntryBase.telematikID  
    } else {
      this.idLabel = "UUID"
      this.idValue = this.model.DirectoryEntryBase.dn?.uid || ""
    }
  }


}
