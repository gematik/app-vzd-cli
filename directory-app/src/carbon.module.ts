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
  ]
})
export class CarbonModule {}
