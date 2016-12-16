import {Component, OnInit, Input, AfterViewInit} from '@angular/core';
import {HierarchicalDataForStorage} from "../../all-measurements/hierarchical-data-for-storage";
import {MeasurementSourceFilterHolderService} from "../../services/measurement-source-filter-holder.service";
import {Http, URLSearchParams} from "@angular/http";
import 'rxjs/add/operator/map'
import {AllMeasurementsDirectChildrenRequest} from "../../all-measurements/all-measurements-direct-children-request";

@Component({
    selector: 'app-measurement-entry',
    templateUrl: './measurement-entry.component.html',
    styleUrls: ['./measurement-entry.component.css']
})
export class MeasurementEntryComponent implements OnInit, AfterViewInit {
    @Input()
    measurement: HierarchicalDataForStorage;

    constructor(private http: Http, private measurementSourceFilterHolderService: MeasurementSourceFilterHolderService) {
    }

    ngOnInit() {
    }

    ngAfterViewInit(): void {
        // console.log("measurement" + JSON.stringify(this.measurement));
    }

    public loadChildren(selectedMeasurement: HierarchicalDataForStorage): void {
        console.log("loadChildren called");
        console.log("selectedMeasurement:" + JSON.stringify(selectedMeasurement));

        if (selectedMeasurement.children != undefined && selectedMeasurement.children.length > 0) {
            selectedMeasurement.children = [];
        } else {
            if (!selectedMeasurement.hasChildren){
                //this entry has no children, so why send a request?
            }
            let toSend: AllMeasurementsDirectChildrenRequest = new AllMeasurementsDirectChildrenRequest(selectedMeasurement);

            console.log("toSend:" + JSON.stringify(toSend));
            this.http.post("http://localhost:4567/getDirectChildrenForMeasurement", JSON.stringify(toSend))
                .map(res => res.json())
                .subscribe(data => {
                    console.log("loadChildren...callback");
                    console.log("   " + JSON.stringify(data.children));
                    selectedMeasurement.children = data.children;
                });
        }
    }

}
