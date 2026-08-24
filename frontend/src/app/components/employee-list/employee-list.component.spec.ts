import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { EmployeeListComponent } from './employee-list.component';
import { ApiService } from '../../services/api.service';
import { EmployeeFilterService } from '../../services/employee-filter.service';
import { Employee, PagedResponse } from '../../models/employee.model';

describe('EmployeeListComponent', () => {
  let component: EmployeeListComponent;
  let fixture: ComponentFixture<EmployeeListComponent>;
  let mockApiService: Partial<ApiService>;

  const mockEmployee: Employee = {
    id: 1,
    employeeCode: 'EMP-00001',
    name: 'Sarah Connor',
    country: 'US',
    department: 'Engineering',
    title: 'Senior Engineer',
    currency: 'USD',
    currentSalary: 120000,
    active: true,
    createdAt: '2024-01-01T00:00:00Z'
  };

  const mockPage: PagedResponse<Employee> = {
    content: [mockEmployee],
    totalElements: 1,
    totalPages: 1,
    size: 20,
    number: 0,
    numberOfElements: 1,
    first: true,
    last: true,
    empty: false
  };

  beforeEach(async () => {
    mockApiService = {
      getEmployees: vi.fn().mockReturnValue(of(mockPage))
    };

    await TestBed.configureTestingModule({
      imports: [EmployeeListComponent],
      providers: [
        provideRouter([]),
        provideAnimationsAsync(),
        { provide: ApiService, useValue: mockApiService },
        EmployeeFilterService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EmployeeListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create employee list component and load initial employee list', () => {
    expect(component).toBeTruthy();
    expect(mockApiService.getEmployees).toHaveBeenCalled();
    expect(component.employees.length).toBe(1);
    expect(component.employees[0].name).toBe('Sarah Connor');
    expect(component.totalElements).toBe(1);
  });

  it('should update filter service on country selection change', () => {
    const filterService = TestBed.inject(EmployeeFilterService);
    const spy = vi.spyOn(filterService, 'updateFilter');

    component.onCountryChange('India');
    expect(spy).toHaveBeenCalledWith({ country: 'India', page: 0 });
  });

  it('should update filter service on department selection change', () => {
    const filterService = TestBed.inject(EmployeeFilterService);
    const spy = vi.spyOn(filterService, 'updateFilter');

    component.onDepartmentChange('Engineering');
    expect(spy).toHaveBeenCalledWith({ department: 'Engineering', page: 0 });
  });
});
