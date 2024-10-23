import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DirectoryEntryEditComponent } from './directory-entry-edit.component';

describe('DirectoryEntryEditComponent', () => {
  let component: DirectoryEntryEditComponent;
  let fixture: ComponentFixture<DirectoryEntryEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DirectoryEntryEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DirectoryEntryEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
