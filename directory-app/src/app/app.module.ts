import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

import { CarbonModule } from '../carbon.module';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { SettingsComponent } from './settings/settings.component';
import { MainComponent } from './main/main.component';
import { AdminModule } from './admin/admin.module';

@NgModule({
  declarations: [
    AppComponent,
    SettingsComponent,
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
    AdminModule,
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
