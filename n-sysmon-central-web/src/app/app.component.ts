import {Component, OnInit, Pipe, PipeTransform} from '@angular/core';
import {Http, Response} from '@angular/http';
import {PartialObserver} from "rxjs/Observer";
import {OverviewResponse} from "./overviewresponse";
import 'rxjs/add/operator/map'

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
    private title: string;
    private data:OverviewResponse = new OverviewResponse;

    constructor(private http: Http) {
    };

    ngOnInit(): void {
        this.http.get("http://localhost:4567/getDataOverview")
            .map(res => res.json())
            .subscribe(data => this.data = data);
        this.title = 'app works!';
    }

    getData():String{
        return JSON.stringify(this.data);
    }

}

@Pipe({name: 'keys'})
export class KeysPipe implements PipeTransform {
    transform(value, args:string[]) : any {
        let keys = [];
        for (let key in value) {
            keys.push({key: key, value: value[key]});
        }
        return keys;
    }
}
