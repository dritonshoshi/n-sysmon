import {HierarchicalDataForStorage} from "./hierarchical-data-for-storage";
export class AllMeasurementsDirectChildrenRequest {

    constructor(measurement: HierarchicalDataForStorage) {
        this.localIdentifier = measurement.localIdentifier;
        this.parentIdentifier = measurement.parentIdentifier;
        this.idRoot = measurement.idRoot;
        this.idMeasurement = measurement._id;
    }

    idRoot: string;
    idMeasurement: string;
    localIdentifier: string;
    parentIdentifier: string;
}
