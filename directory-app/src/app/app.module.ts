import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

import { CarbonModule } from '../carbon.module';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { SettingsComponent } from './settings/settings.component';
import { AddTokenComponent } from './settings/add-token/add-token.component';
import { MainComponent } from './main/main.component';

@NgModule({
  declarations: [
    AppComponent,
    SettingsComponent,
    AddTokenComponent,
    MainComponent,
  ],
  imports: [
    // angular imports
    BrowserModule,
    HttpClientModule,    
    BrowserAnimationsModule,
    FormsModule,
    // carbon imports
    CarbonModule,
    // local imports
    AppRoutingModule,
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
