import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DirectoryEntrySummaryComponent } from './directory-entry-summary.component';

describe('DirectoryEntrySummaryComponent', () => {
  let component: DirectoryEntrySummaryComponent;
  let fixture: ComponentFixture<DirectoryEntrySummaryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DirectoryEntrySummaryComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DirectoryEntrySummaryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
