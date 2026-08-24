import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { EmployeeDetailComponent } from './employee-detail.component';
import { ApiService } from '../../services/api.service';
import { Employee, SalaryHistory } from '../../models/employee.model';

describe('EmployeeDetailComponent', () => {
  let component: EmployeeDetailComponent;
  let fixture: ComponentFixture<EmployeeDetailComponent>;
  let mockApiService: {
    getEmployeeById: ReturnType<typeof vi.fn>;
    getSalaryHistory: ReturnType<typeof vi.fn>;
    updateEmployee: ReturnType<typeof vi.fn>;
    updateSalary: ReturnType<typeof vi.fn>;
  };

  const initialEmployee: Employee = {
    id: 1,
    employeeCode: 'EMP-00001',
    name: 'Robin Williamson',
    country: 'Canada',
    department: 'HR',
    title: 'HR Business Partner',
    currency: 'CAD',
    currentSalary: 112613.52,
    active: true,
    createdAt: '2023-07-03T00:00:00Z'
  };

  const initialHistory: SalaryHistory[] = [
    {
      id: 101,
      employeeId: 1,
      amount: 112613.52,
      currency: 'CAD',
      effectiveDate: '2023-07-03',
      changedAt: '2023-07-03T00:00:00Z',
      note: 'Initial salary'
    }
  ];

  beforeEach(async () => {
    mockApiService = {
      getEmployeeById: vi.fn().mockReturnValue(of(initialEmployee)),
      getSalaryHistory: vi.fn().mockReturnValue(of(initialHistory)),
      updateEmployee: vi.fn().mockReturnValue(of(initialEmployee)),
      updateSalary: vi.fn().mockReturnValue(of({ ...initialEmployee, currentSalary: 125000 }))
    };

    await TestBed.configureTestingModule({
      imports: [EmployeeDetailComponent],
      providers: [
        provideRouter([]),
        provideAnimationsAsync(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => (key === 'id' ? '1' : null)
              }
            }
          }
        },
        { provide: ApiService, useValue: mockApiService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(EmployeeDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create component and load initial employee details and salary history', () => {
    expect(component).toBeTruthy();
    expect(mockApiService.getEmployeeById).toHaveBeenCalledWith(1);
    expect(mockApiService.getSalaryHistory).toHaveBeenCalledWith(1);
    expect(component.employee?.name).toBe('Robin Williamson');
    expect(component.salaryHistory.length).toBe(1);
  });

  describe('Edit Profile Form Validation & Submission', () => {
    it('should initialize editForm with employee values', () => {
      expect(component.editForm.get('name')?.value).toBe('Robin Williamson');
      expect(component.editForm.get('country')?.value).toBe('Canada');
      expect(component.editForm.get('department')?.value).toBe('HR');
      expect(component.editForm.get('title')?.value).toBe('HR Business Partner');
    });

    it('should invalidate editForm when required fields are empty', () => {
      component.editForm.patchValue({ name: '', country: '', department: '' });
      expect(component.editForm.invalid).toBe(true);
    });

    it('should call updateEmployee with valid payload on valid save profile', () => {
      component.editForm.patchValue({
        name: 'Robin Williamson-Smith',
        country: 'Canada',
        department: 'HR',
        title: 'Senior HR Director'
      });

      component.onSaveProfile();

      expect(mockApiService.updateEmployee).toHaveBeenCalledWith(1, {
        name: 'Robin Williamson-Smith',
        country: 'Canada',
        department: 'HR',
        title: 'Senior HR Director'
      });
      expect(component.profileFeedbackType).toBe('success');
    });
  });

  describe('Update Salary Form Validation & Re-fetching', () => {
    it('should reject non-positive salaries (<= 0)', () => {
      const salaryControl = component.salaryForm.get('newSalary');
      salaryControl?.setValue(0);
      expect(salaryControl?.hasError('min')).toBe(true);

      salaryControl?.setValue(-500);
      expect(salaryControl?.hasError('min')).toBe(true);
    });

    it('should reject a new salary that equals the current salary', () => {
      component.salaryForm.patchValue({
        newSalary: 112613.52 // identical to currentSalary
      });

      expect(component.salaryForm.hasError('sameSalary')).toBe(true);
    });

    it('should accept a valid different positive salary and submit correct payload', () => {
      component.salaryForm.patchValue({
        newSalary: 125000,
        effectiveDate: '2026-09-01',
        note: 'Annual Promotion'
      });

      expect(component.salaryForm.valid).toBe(true);

      component.onUpdateSalary();

      expect(mockApiService.updateSalary).toHaveBeenCalledWith(1, {
        newSalary: 125000,
        effectiveDate: '2026-09-01',
        note: 'Annual Promotion'
      });
    });

    it('should re-fetch salary history immediately after a successful salary update', () => {
      const updatedHistory: SalaryHistory[] = [
        {
          id: 102,
          employeeId: 1,
          amount: 125000,
          currency: 'CAD',
          effectiveDate: '2026-09-01',
          changedAt: '2026-08-24T18:00:00Z',
          note: 'Annual Promotion'
        },
        ...initialHistory
      ];

      // On second call (re-fetch), return updatedHistory
      mockApiService.getSalaryHistory.mockReturnValue(of(updatedHistory));

      component.salaryForm.patchValue({
        newSalary: 125000,
        effectiveDate: '2026-09-01',
        note: 'Annual Promotion'
      });

      // Clear previous call count from initialization
      mockApiService.getSalaryHistory.mockClear();

      component.onUpdateSalary();

      expect(mockApiService.updateSalary).toHaveBeenCalledTimes(1);
      // Confirms getSalaryHistory re-fetch was triggered
      expect(mockApiService.getSalaryHistory).toHaveBeenCalledWith(1);
      expect(component.salaryHistory.length).toBe(2);
      expect(component.salaryHistory[0].amount).toBe(125000);
      expect(component.salaryFeedbackType).toBe('success');
    });
  });
});
