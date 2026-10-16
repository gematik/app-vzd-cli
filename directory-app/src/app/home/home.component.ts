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

import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { first, firstValueFrom } from 'rxjs';
import { NavigationService } from 'src/services/navigation.service';

@Component({
    selector: 'app-home',
    template: '',
    standalone: false
})
export class HomeComponent implements OnInit {

  constructor(
    private navigationService: NavigationService,
    private router: Router,
  ) { }

  ngOnInit() {
    firstValueFrom(this.navigationService.adminMenuLinks$).then(links => {
      if (links.length > 0) {
        this.router.navigate([links[0].route])
      } else {
        this.router.navigate(["settings"])
      }
    })
  }
}
