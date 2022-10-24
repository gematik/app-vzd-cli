import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { SearchResultsComponent } from './search-results/search-results.component';
import { SearchComponent } from './search/search.component';

const routes: Routes = [
	{
		path: '',
		redirectTo: 'search',
    pathMatch: 'prefix'
	},
	{
		path: 'search',
		component: SearchComponent,
	},
	{
		path: 'search-results',
		component: SearchResultsComponent,
	},
];

@NgModule({
	imports: [RouterModule.forChild(routes)],
	exports: [RouterModule]
})
export class AdminRoutingModule { }