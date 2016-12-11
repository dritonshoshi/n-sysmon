import {Component, OnInit} from '@angular/core';
import {MeasurementSourceFilterHolderService} from "../services/measurement-source-filter-holder.service";
import {Http} from '@angular/http';
import 'rxjs/add/operator/map'
import {AllMeasurementsResponse} from "./all-measurements-response";

@Component({
    selector: 'app-all-measurements',
    templateUrl: './all-measurements.component.html',
    styleUrls: ['./all-measurements.component.css']
})
export class AllMeasurementsComponent implements OnInit {
    private _data:AllMeasurementsResponse = new AllMeasurementsResponse();

    constructor(private http: Http, private measurementSourceFilterHolderService: MeasurementSourceFilterHolderService) {
    }

    ngOnInit() {
    }

    public reloadDataFromServer(): void {
        //TODO move url to config-parameter
        console.log("reloadDataFromServer called");
        this.http.get("http://localhost:4567/getAllMeasurements")
            .map(res => res.json())
            .subscribe(data => this._data = data);
    }

    public dataAsJSON(): string {
        return JSON.stringify(this._data);
    }

}
