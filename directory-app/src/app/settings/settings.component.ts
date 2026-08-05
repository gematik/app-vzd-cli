import { Component, OnInit, signal } from '@angular/core';
import { InlineLoadingState } from 'carbon-components-angular';
import { BackendService } from 'src/services/backend.service';
import { GlobalConfig } from 'src/services/global.model';
import { NavigationService } from '../../services/navigation.service';
import { Router } from '@angular/router';
@Component({
    selector: 'app-settings',
    templateUrl: './settings.component.html',
    styleUrls: ['./settings.component.scss'],
    standalone: false
})
export class SettingsComponent implements OnInit {
  protected config = signal<GlobalConfig | undefined>(undefined)
  protected configState = signal(InlineLoadingState.Hidden)

  constructor(
    protected backendService: BackendService,
    protected navigationService: NavigationService,
    protected router: Router,
    ) { }

  ngOnInit(): void {
    this.backendService.getConfig().subscribe( it => {
      this.config.set(it)
    })
  }

  updateConfig() {
    this.backendService.updateConfig(this.config()!)
      .then( config => {
        this.config.set(config)
        this.configState.set(InlineLoadingState.Finished)
      })
      .catch(err => {
        this.configState.set(InlineLoadingState.Error)
        console.error(err)
      })
  }

  tabSelected() {
    this.configState.set(InlineLoadingState.Hidden)
  }
}
