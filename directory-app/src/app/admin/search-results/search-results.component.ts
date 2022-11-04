import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { Table, TableHeaderItem, TableItem, TableModel } from 'carbon-components-angular';
import { BaseDirectoryEntry } from 'src/services/admin/admin.model';
import { AdminBackendService } from '../../../services/admin/admin-backend.service';

@Component({
  selector: 'app-admin-search-results',
  templateUrl: './search-results.component.html',
  styleUrls: ['./search-results.component.scss']
})
export class SearchResultsComponent implements OnInit {
  env!: string
  queryString = ""
  model = new TableModel()
  rows: TableItem[][] = []
  loading = false
  errorMessage: string | null = null
  skeletonModel = Table.skeletonModel(3, 3);

  @ViewChild("expandedTemplate", { static: false })
  // @ts-ignore
	protected expandedTemplate: TemplateRef<any>;
  @ViewChild("addressTemplate", { static: false })
  // @ts-ignore
	protected addressTemplate: TemplateRef<any>;
  @ViewChild("tagTemplate", { static: false })
  // @ts-ignore
	protected tagTemplate: TemplateRef<any>;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private adminBackend: AdminBackendService
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
    this.adminBackend.search(this.env, this.queryString).then(searchResult => {
      this.rows = searchResult.directoryEntries.map( (entry) => {
        entry.DirectoryEntryBase.displayName = entry.DirectoryEntryBase?.displayName?.replace("TEST-ONLY", "")
        entry.DirectoryEntryBase.displayName = entry.DirectoryEntryBase?.displayName?.replace("NOT-VALID", "")
        return [
          new TableItem({
            data: entry.DirectoryEntryBase.displayName,
            expandedData: entry.DirectoryEntryBase,
            expandedTemplate: this.expandedTemplate,
          }),
          new TableItem({
            data: { color: this.adminBackend.getEntryKindColor(entry), text: this.adminBackend.getEntryKindTitle(entry) },
            template: this.tagTemplate,
          }),
          new TableItem({
            data: entry.DirectoryEntryBase,
            template: this.addressTemplate,
          }),
        ]
      })
      if (this.rows.length == 1) {
        this.router.navigate(
          ["entry", this.rows[0][0].expandedData.telematikID, {"q": this.queryString}],
          { relativeTo: this.route.parent }
        )
      }
      this.model.pageLength = 10
      this.model.totalDataLength = this.rows.length
      this.selectPage(1);
      this.loading = false
    })
    .catch(e => {
      const httpError = e as HttpErrorResponse
      if (httpError?.status == 401) {
        this.router.navigate(["/settings"])
      }
      this.loading = false
      this.errorMessage = e.message
    })
  }

  onSearch() {
    this.router.navigate(["search-results", { "q": this.queryString}], { relativeTo: this.route.parent })
  }

  onClear() {
    this.router.navigate(["search"], { relativeTo: this.route.parent })
  }

  selectPage(page: number) {
    const startIndex = (page-1)*this.model.pageLength
    this.model.data = this.rows.slice(startIndex, startIndex+this.model.pageLength)
    this.model.currentPage = page;
  }

  onRowClick(clickedRow: number) {
    const rowNum = (this.model.currentPage-1)*this.model.pageLength+clickedRow
    const entry = this.rows[clickedRow][0].expandedData as BaseDirectoryEntry
    this.router.navigate(
      ["entry", entry.telematikID, {"q": this.queryString}],
      { relativeTo: this.route.parent }
    )
  }

  onErrorClose() {
    this.router.navigate(["search"], { relativeTo: this.route.parent })
  }

}
