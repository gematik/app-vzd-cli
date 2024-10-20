import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SmartcardComponent } from './smartcard.component';

describe('SmartcardComponent', () => {
  let component: SmartcardComponent;
  let fixture: ComponentFixture<SmartcardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SmartcardComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SmartcardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
