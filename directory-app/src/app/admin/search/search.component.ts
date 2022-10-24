import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { map, Observable } from 'rxjs';

@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.scss']
})
export class SearchComponent implements OnInit {
  env: Observable<string>
  queryString = ""

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    ) { 
    this.env = route.params.pipe(map(p => p["env"]));
  }

  ngOnInit(): void {
    this.route.params.subscribe(param => 
      this.queryString = param['q'] || ""
    )
  }

  onSearch(): void {
    this.router.navigate(["search-results", { "q": this.queryString}], { relativeTo: this.route.parent })
  }
}
