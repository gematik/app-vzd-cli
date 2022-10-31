import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AdminBackendService } from 'src/services/admin/admin-backend.service';
import { DirectoryEntry } from 'src/services/admin/admin.model';

@Component({
  selector: 'app-directory-entry',
  templateUrl: './directory-entry.component.html',
  styleUrls: ['./directory-entry.component.scss']
})
export class DirectoryEntryComponent implements OnInit {
  env!: string
  queryString: string | null = null
  entry?: DirectoryEntry

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private adminBackend: AdminBackendService
  ) { }

  ngOnInit(): void {
    this.route.params.subscribe(param => {
      this.queryString = param['q']
      const telematikID = param['id']
      this.env = param['env']
      this.adminBackend.loadEntry(this.env, telematikID).then(
        value => this.entry = value
      )
    })
  }
}
