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

import { Component, Input, OnInit } from '@angular/core';
import { Coding, ElaborateDirectoryEntry } from 'src/services/admin/admin.model';

@Component({
    selector: 'app-admin-directory-entry-summary',
    templateUrl: './directory-entry-summary.component.html',
    styleUrls: ['./directory-entry-summary.component.scss'],
    standalone: false
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

  codeListDisplay(codeList: [Coding] | undefined) {
    return codeList?.map(code => code.display).join(", ")
  }

}
