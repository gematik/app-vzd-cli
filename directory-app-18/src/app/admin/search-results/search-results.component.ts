import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { InlineLoadingState, TableHeaderItem, TableItem, TableModel } from 'carbon-components-angular';
import { ElaborateDirectoryEntry } from 'src/services/admin/admin.model';
import { AdminBackendService } from '../../../services/admin/admin-backend.service';
import { IconService } from 'carbon-components-angular';
import { Hospital16, User16 } from "@carbon/icons";

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
  errorMessage: string | null = null
  loadingState = InlineLoadingState.Hidden
  searchReport = ""

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
    private adminBackend: AdminBackendService,
    private iconService: IconService,
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

    this.iconService.register(Hospital16)
    this.iconService.register(User16)
  }

  search() {
    this.loadingState = InlineLoadingState.Active
    this.adminBackend.search(this.env, this.queryString).then(searchResult => {
      this.rows = searchResult.directoryEntries.map( (entry) => {
        entry.base.displayName = entry.base?.displayName?.replace("TEST-ONLY", "")
        entry.base.displayName = entry.base?.displayName?.replace("NOT-VALID", "")
        return [
          new TableItem({
            data: entry.base.displayName,
            expandedData: entry,
            expandedTemplate: this.expandedTemplate,
          }),
          new TableItem({
            data: { 
              color: this.adminBackend.getEntryKindColor(entry), 
              text: this.adminBackend.getEntryKindTitle(entry),
              icon: this.adminBackend.getEntryKindIcon(entry),
            },
            template: this.tagTemplate,
          }),
          new TableItem({
            data: entry.base,
            template: this.addressTemplate,
          }),
        ]
      })
      /*
      if (this.rows.length == 1) {
        this.loadingState = InlineLoadingState.Finished
        this.router.navigate(
          ["entry", this.rows[0][0].expandedData.DirectoryEntryBase.telematikID, {"q": this.queryString}],
          { relativeTo: this.route.parent }
        )
      }
      */
      this.model.pageLength = 25
      this.model.totalDataLength = Math.ceil(this.rows.length / 25)
      this.selectPage(1);
      if (this.rows.length == 0) {
        this.loadingState = InlineLoadingState.Error
      } else {
        this.loadingState = InlineLoadingState.Finished
        if (this.rows.length >= 100) {
          this.searchReport = `Über 100 Einträge gefunden`
        } else {
          this.searchReport = `${this.rows.length} Einträge gefunden`
        }
      }
    })
    .catch(e => {
      const httpError = e as HttpErrorResponse
      if (httpError?.status == 401) {
        this.router.navigate(["/settings"])
      }
      this.loadingState = InlineLoadingState.Error
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
    window.scroll({ 
      top: 0, 
      left: 0, 
      behavior: 'smooth' 
    });    
  }

  onRowClick(clickedRow: number) {
    const rowNum = (this.model.currentPage-1)*this.model.pageLength+clickedRow
    const entry = this.rows[rowNum][0].expandedData as ElaborateDirectoryEntry
    this.router.navigate(
      ["entry", entry.base.telematikID, {"q": this.queryString}],
      { relativeTo: this.route.parent }
    )
  }

  onErrorClose() {
    this.router.navigate(["search"], { relativeTo: this.route.parent })
  }

}
