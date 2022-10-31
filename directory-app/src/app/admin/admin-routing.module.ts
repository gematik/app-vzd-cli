import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { DirectoryEntryComponent } from './directory-entry/directory-entry.component';
import { SearchResultsComponent } from './search-results/search-results.component';
import { SearchComponent } from './search/search.component';

const routes: Routes = [
  {
	path: '',
	redirectTo: 'search',
	pathMatch: 'full'
  },
  {
	path: 'search',
	component: SearchComponent,
  },
  {
	path: 'search-results',
	component: SearchResultsComponent,
  },
  {
	path: 'entry/:id',
	component: DirectoryEntryComponent,
  },		
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AdminRoutingModule { }