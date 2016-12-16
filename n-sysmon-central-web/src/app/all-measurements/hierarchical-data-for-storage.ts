export class HierarchicalDataForStorage {
    public _id:string;
    public idRoot:string;
    public identifier:string;
    public startTimeMillis:number;
    public durationNanos:number;
    //TODO public Map<String, String> parameters;
    public hasChildren:boolean;
    public level:number;
    public localIdentifier:string;
    public parentIdentifier:string;
    public children:Array<HierarchicalDataForStorage> = [];

}
