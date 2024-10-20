import { AfterViewInit, Component, ComponentRef, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Search } from 'carbon-components-angular';

@Component({
  selector: 'app-admin-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss']
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
