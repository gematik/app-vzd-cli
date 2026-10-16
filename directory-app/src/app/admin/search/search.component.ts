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
