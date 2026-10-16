/*
Copyright 2026, gematik GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*******

For additional notes and disclaimer from gematik and in case of changes
by gematik, find details in the "Readme" file.
*/

declare global {
    interface Window {
        __baseHref?: string;
    }
}

import { NgModule, provideZoneChangeDetection } from '@angular/core';
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
import { APP_BASE_HREF } from '@angular/common';

@NgModule({
    declarations: [
        AppComponent,
        SettingsComponent,
        HomeComponent,
    ],
    bootstrap: [AppComponent], 
    imports: [
        // angular imports
        BrowserModule,
        BrowserAnimationsModule,
        FormsModule,
        // carbon imports
        CarbonModule,
        // local imports
        AppRoutingModule,
        AdminModule
    ], 
    providers: [
        provideZoneChangeDetection(),
        provideHttpClient(withInterceptorsFromDi()),
        { 
            provide: APP_BASE_HREF,
            useValue: window.__baseHref || '/'
        }
    ] 
})
export class AppModule { }
