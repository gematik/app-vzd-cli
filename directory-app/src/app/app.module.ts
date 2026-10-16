/*
 * Copyright (Change Date see Readme), gematik GmbH
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
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
