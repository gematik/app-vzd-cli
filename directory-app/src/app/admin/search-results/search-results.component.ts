import { Component, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { TableHeaderItem, TableItem, TableModel } from 'carbon-components-angular';
import { AdminBackendService } from '../../../services/admin/admin-backend.service';

@Component({
  selector: 'app-search-results',
  templateUrl: './search-results.component.html',
  styleUrls: ['./search-results.component.scss']
})
export class SearchResultsComponent implements OnInit {
  env?: string
  queryString = ""
  model = new TableModel()
  rows: TableItem[][] = []
  loading = false

  @ViewChild("expandedTemplate", { static: false })
  // @ts-ignore
	protected expandedTemplate: TemplateRef<any>;
  @ViewChild("addressTemplate", { static: false })
  // @ts-ignore
	protected addressTemplate: TemplateRef<any>;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private backend: AdminBackendService
  ) { 
  }

  ngOnInit(): void {
    this.model.header = [
			new TableHeaderItem({ data: "Name" }),
			new TableHeaderItem({ data: "Art" }),
			new TableHeaderItem({ data: "Adresse" }),
		];

    this.route.params.subscribe( (params) => {
      this.env = params['env'] 
      this.queryString = params['q'] || ""
      this.search()
    })
  }

  search() {
    this.loading = true
    this.backend.search(this.env!, this.queryString).subscribe(entries => {
      this.rows = entries?.map( (entry) => {
        entry.DirectoryEntryBase.displayName = entry.DirectoryEntryBase?.displayName?.replace("TEST-ONLY", "")
        entry.DirectoryEntryBase.displayName = entry.DirectoryEntryBase?.displayName?.replace("NOT-VALID", "")
        return [
          new TableItem({
            data: entry.DirectoryEntryBase.displayName,
            expandedData: entry.DirectoryEntryBase,
            expandedTemplate: this.expandedTemplate,
          }),
          new TableItem({
            data: "Arzt",
          }),
          new TableItem({
            data: entry.DirectoryEntryBase,
            template: this.addressTemplate,
          }),
        ]
      })
      this.model.pageLength = 10
      this.model.totalDataLength = this.rows.length
      this.selectPage(1);
      this.loading = false
    })
  }

  onSearch() {
    this.router.navigate(["search-results", { "q": this.queryString}], { relativeTo: this.route.parent })
  }

  onClear() {
    this.router.navigate(["search"], { relativeTo: this.route.parent })
  }

  selectPage(page: number) {
    const startIndex= (page-1)*this.model.pageLength
    this.model.data = this.rows.slice(startIndex, startIndex+this.model.pageLength)
    this.model.currentPage = page;
  }

  link(entry: any) {
    return ""
  }
}
