import {HierarchicalDataForStorage} from "./hierarchical-data-for-storage";

export class AllMeasurementsResponse {
    get entries(): Array<HierarchicalDataForStorage> {
        return this._entries;
    }
    private _entries:Array<HierarchicalDataForStorage>=[];
}
