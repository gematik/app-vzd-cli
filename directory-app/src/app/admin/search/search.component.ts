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

import { AfterViewInit, Component, ComponentRef, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Search } from 'carbon-components-angular';

@Component({
    selector: 'app-admin-search',
    templateUrl: './search.component.html',
    styleUrls: ['./search.component.scss'],
    standalone: false
})
export class SearchComponent implements OnInit, AfterViewInit {
  queryString = ""
  
  @ViewChild(Search) search?: Search

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    ) { 
  }

  ngOnInit(): void {
    this.route.params.subscribe(param => 
      this.queryString = param['q'] || ""
    )
  }

  ngAfterViewInit(): void {
    this.search?.openSearch()
  }  

  onSearch(): void {
    this.router.navigate(["search-results", { "q": this.queryString}], { relativeTo: this.route.parent })
  }
}
