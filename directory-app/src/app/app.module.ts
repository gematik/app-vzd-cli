import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

import { CarbonModule } from '../carbon.module';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { SettingsComponent } from './settings/settings.component';
import { HomeComponent } from './home/home.component';
import { AdminModule } from './admin/admin.module';

@NgModule({ declarations: [
        AppComponent,
        SettingsComponent,
        HomeComponent,
    ],
    bootstrap: [AppComponent], imports: [
        // angular imports
        BrowserModule,
        BrowserAnimationsModule,
        FormsModule,
        // carbon imports
        CarbonModule,
        // local imports
        AppRoutingModule,
        AdminModule], providers: [provideHttpClient(withInterceptorsFromDi())] })
export class AppModule { }
