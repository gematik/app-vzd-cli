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

// angular imports
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

// carbon imports
import { CarbonModule } from '../../carbon.module';
// local imports
import { AdminRoutingModule } from './admin-routing.module';
import { SearchResultsComponent } from './search-results/search-results.component';
import { SearchComponent } from './search/search.component';
import { DirectoryEntryComponent } from './directory-entry/directory-entry.component';
import { SettingsComponent } from './settings/settings.component';
import { AskPasswordComponent } from './settings/ask-password/ask-password.component';
import { DirectoryEntrySummaryComponent } from './search-results/directory-entry-summary/directory-entry-summary.component';
import { SmartcardComponent } from './directory-entry/smartcard/smartcard.component';
import { ValidityPeriodComponent } from './directory-entry/validity-period/validity-period.component';
import { ObjectToMapPipe } from '../object-to-map.pipe';

@NgModule({ declarations: [
        SearchResultsComponent,
        SearchComponent,
        DirectoryEntryComponent,
        SettingsComponent,
        AskPasswordComponent,
        DirectoryEntrySummaryComponent,
        SmartcardComponent,
        ValidityPeriodComponent,
        ObjectToMapPipe,
    ],
    exports: [
        SettingsComponent
    ], imports: [
        // angular imports
        CommonModule,
        FormsModule,
        // carbon imports
        CarbonModule,
        // local imports
        AdminRoutingModule], providers: [provideHttpClient(withInterceptorsFromDi())] })
export class AdminModule { }
