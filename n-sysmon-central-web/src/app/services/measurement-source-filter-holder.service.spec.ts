/* tslint:disable:no-unused-variable */

import { TestBed, async, inject } from '@angular/core/testing';
import { MeasurementSourceFilterHolderService } from './measurement-source-filter-holder.service';

describe('MeasurementSourceFilterHolderService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [MeasurementSourceFilterHolderService]
    });
  });

  it('should ...', inject([MeasurementSourceFilterHolderService], (service: MeasurementSourceFilterHolderService) => {
    expect(service).toBeTruthy();
  }));
});
