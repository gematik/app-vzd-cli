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

@NgModule({
  declarations: [
    SearchResultsComponent,
    SearchComponent,
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
  ]
})
export class AdminModule { }
