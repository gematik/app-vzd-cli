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
  NotificationModule,
  InlineLoadingModule,
  CodeSnippetModule,
  StructuredListModule,
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
    NotificationModule,
    InlineLoadingModule,
    CodeSnippetModule,
    StructuredListModule,
  ]
})
export class CarbonModule {}
