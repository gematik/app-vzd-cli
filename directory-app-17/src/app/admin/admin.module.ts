// angular imports
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';

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

@NgModule({
  declarations: [
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
  imports: [
    // angular imports
    CommonModule,
    FormsModule,
    HttpClientModule,
    // carbon imports
    CarbonModule,
    // local imports
    AdminRoutingModule,
  ],
  exports: [
    SettingsComponent
  ]
})
export class AdminModule { }
