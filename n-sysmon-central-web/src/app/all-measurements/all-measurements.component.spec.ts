/* tslint:disable:no-unused-variable */
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';

import { AllMeasurementsComponent } from './all-measurements.component';

describe('AllMeasurementsComponent', () => {
  let component: AllMeasurementsComponent;
  let fixture: ComponentFixture<AllMeasurementsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AllMeasurementsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AllMeasurementsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
