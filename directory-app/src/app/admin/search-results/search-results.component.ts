import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, TemplateRef, ViewChild, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { InlineLoadingState, TableHeaderItem, TableItem, TableModel } from 'carbon-components-angular';
import { ElaborateDirectoryEntry } from 'src/services/admin/admin.model';
import { AdminBackendService } from '../../../services/admin/admin-backend.service';
import { IconService } from 'carbon-components-angular';
import { Hospital16, User16 } from "@carbon/icons";

@Component({
    selector: 'app-admin-search-results',
    templateUrl: './search-results.component.html',
    styleUrls: ['./search-results.component.scss'],
    standalone: false
})
export class SearchResultsComponent implements OnInit {
  env!: string
  queryString = ""
  model = new TableModel()
  rows = signal<TableItem[][]>([])
  errorMessage = signal<string | undefined>(undefined)
  loadingState = signal(InlineLoadingState.Hidden)
  searchReport = signal("")

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
    this.loadingState.set(InlineLoadingState.Active)
    this.adminBackend.search(this.env, this.queryString).then(searchResult => {
      const mappedRows: TableItem[][] = searchResult.directoryEntries.map( (entry) => {
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
      this.rows.set(mappedRows)
      this.model.pageLength = 25
      this.model.totalDataLength = Math.ceil(mappedRows.length / 25)
      this.selectPage(1);
      if (mappedRows.length == 0) {
        this.loadingState.set(InlineLoadingState.Error)
      } else {
        this.loadingState.set(InlineLoadingState.Finished)
        if (mappedRows.length >= 100) {
          this.searchReport.set(`Über 100 Einträge gefunden`)
        } else {
          this.searchReport.set(`${mappedRows.length} Einträge gefunden`)
        }
      }
    })
    .catch(e => {
      const httpError = e as HttpErrorResponse
      if (httpError?.status == 401) {
        this.router.navigate(["/settings"])
      }
      this.loadingState.set(InlineLoadingState.Error)
      this.errorMessage.set(e.message)
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
    this.model.data = this.rows().slice(startIndex, startIndex+this.model.pageLength)
    this.model.currentPage = page;
    window.scroll({
      top: 0,
      left: 0,
      behavior: 'smooth'
    });
  }

  onRowClick(clickedRow: number) {
    const rowNum = (this.model.currentPage-1)*this.model.pageLength+clickedRow
    const entry = this.rows()[rowNum][0].expandedData as ElaborateDirectoryEntry
    this.router.navigate(
      ["entry", entry.base.telematikID, {"q": this.queryString}],
      { relativeTo: this.route.parent }
    )
  }

  onErrorClose() {
    this.router.navigate(["search"], { relativeTo: this.route.parent })
  }

}
