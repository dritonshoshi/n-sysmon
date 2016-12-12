import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {HttpModule} from '@angular/http';

import {AppComponent} from './app.component';
import {AllMeasurementsComponent} from './all-measurements/all-measurements.component';
import {KeysPipe} from "./pipes/keys.pipe";
import {MeasurementSourceFilterHolderService} from "./services/measurement-source-filter-holder.service";
import {MeasurementSourceFilterComponent} from "./components/measurement-source-filter/measurement-source-filter.component";
import {MeasurementEntryComponent} from "./components/measurement-entry/measurement-entry.component";

@NgModule({
    declarations: [
        AppComponent,
        KeysPipe,
        AllMeasurementsComponent,
        MeasurementSourceFilterComponent,
        MeasurementEntryComponent
    ],
    imports: [
        BrowserModule,
        FormsModule,
        HttpModule
    ],
    providers: [
        MeasurementSourceFilterHolderService
    ],
    bootstrap: [AppComponent]
})
export class AppModule {
}
