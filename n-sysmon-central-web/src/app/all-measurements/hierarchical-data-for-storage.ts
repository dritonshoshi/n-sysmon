export class HierarchicalDataForStorage {
    public _id:string;
    public idRoot:string;
    public identifier:string;
    public startTimeMillis:number;
    //TODO public Map<String, String> parameters;
    public empty:boolean;
    public level:number;
    public localIdentifier:string;
    public parentIdentifier:string;
    public children:Array<HierarchicalDataForStorage> = [];

}
