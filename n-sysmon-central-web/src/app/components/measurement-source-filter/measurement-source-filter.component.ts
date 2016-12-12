import { Component, OnInit } from '@angular/core';
import {MeasurementSourceFilterHolderService} from "../../services/measurement-source-filter-holder.service";

@Component({
  selector: 'app-measurement-source-filter',
  templateUrl: './measurement-source-filter.component.html',
  styleUrls: ['./measurement-source-filter.component.css']
})
export class MeasurementSourceFilterComponent implements OnInit {

  constructor(private measurementSourceFilterHolderService:MeasurementSourceFilterHolderService) { }

  ngOnInit() {
  }

}
