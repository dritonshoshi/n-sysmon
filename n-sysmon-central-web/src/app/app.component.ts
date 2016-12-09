import {Component, OnInit} from '@angular/core';
import {Http, Response} from '@angular/http';
import {PartialObserver} from "rxjs/Observer";
import 'rxjs/add/operator/map'

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
    private title: string;
    private data:any;

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
