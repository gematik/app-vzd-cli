import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AskPasswordComponent } from './ask-password.component';

describe('AskPasswordComponent', () => {
  let component: AskPasswordComponent;
  let fixture: ComponentFixture<AskPasswordComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ AskPasswordComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AskPasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
