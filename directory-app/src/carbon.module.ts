import {NgModule} from '@angular/core';
import { 
  UIShellModule, 
  BreadcrumbModule, 
  ButtonModule, 
  InputModule, 
  GridModule,
  TabsModule,
  SearchModule,
  IconModule,
  ModalModule,
  TilesModule,
  PlaceholderModule,
  LoadingModule,
  TableModule,
  PaginationModule,
} from 'carbon-components-angular';

@NgModule({
  exports: [
    UIShellModule,
    BreadcrumbModule, 
    ButtonModule, 
    InputModule, 
    GridModule,
    TabsModule,
    SearchModule,
    IconModule,
    ModalModule,
    TilesModule,
    PlaceholderModule,
    LoadingModule,
    TableModule,
    PaginationModule,
  ]
})
export class CarbonModule {}
