import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ValidityPeriodComponent } from './validity-period.component';

describe('ValidityPeriodComponent', () => {
  let component: ValidityPeriodComponent;
  let fixture: ComponentFixture<ValidityPeriodComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ValidityPeriodComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ValidityPeriodComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
