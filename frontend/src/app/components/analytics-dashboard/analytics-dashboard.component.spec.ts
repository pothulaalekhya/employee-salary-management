import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AnalyticsDashboardComponent } from './analytics-dashboard.component';
import { ApiService } from '../../services/api.service';
import {
  SalaryByCountry,
  SalaryByDepartment,
  HeadcountByCountry,
  TotalPayroll
} from '../../models/employee.model';

describe('AnalyticsDashboardComponent', () => {
  let component: AnalyticsDashboardComponent;
  let fixture: ComponentFixture<AnalyticsDashboardComponent>;
  let mockApiService: Partial<ApiService>;

  const mockSalaryByCountry: SalaryByCountry[] = [
    {
      country: 'US',
      currency: 'USD',
      employeeCount: 1000,
      avgSalaryUsd: 120000,
      medianSalaryUsd: 115000,
      minSalaryUsd: 60000,
      maxSalaryUsd: 250000
    },
    {
      country: 'UK',
      currency: 'GBP',
      employeeCount: 500,
      avgSalaryUsd: 90000,
      medianSalaryUsd: 85000,
      minSalaryUsd: 45000,
      maxSalaryUsd: 180000
    },
    {
      country: 'India',
      currency: 'INR',
      employeeCount: 1500,
      avgSalaryUsd: 30000,
      medianSalaryUsd: 28000,
      minSalaryUsd: 10000,
      maxSalaryUsd: 60000
    }
  ];

  const mockSalaryByDepartment: SalaryByDepartment[] = [
    {
      department: 'Engineering',
      employeeCount: 1200,
      avgSalaryUsd: 110000,
      medianSalaryUsd: 105000,
      minSalaryUsd: 50000,
      maxSalaryUsd: 250000
    },
    {
      department: 'Sales',
      employeeCount: 800,
      avgSalaryUsd: 85000,
      medianSalaryUsd: 82000,
      minSalaryUsd: 40000,
      maxSalaryUsd: 200000
    }
  ];

  const mockHeadcountByCountry: HeadcountByCountry[] = [
    { country: 'India', headcount: 1500, percentage: 50.0 },
    { country: 'US', headcount: 1000, percentage: 33.33 },
    { country: 'UK', headcount: 500, percentage: 16.67 }
  ];

  const mockTotalPayroll: TotalPayroll = {
    totalPayrollUsd: 210000000,
    totalEmployees: 3000,
    countryBreakdown: [
      { country: 'US', totalPayrollUsd: 120000000, employeeCount: 1000 },
      { country: 'UK', totalPayrollUsd: 45000000, employeeCount: 500 },
      { country: 'India', totalPayrollUsd: 45000000, employeeCount: 1500 }
    ]
  };

  beforeEach(async () => {
    mockApiService = {
      getSalaryByCountry: vi.fn().mockReturnValue(of(mockSalaryByCountry)),
      getSalaryByDepartment: vi.fn().mockReturnValue(of(mockSalaryByDepartment)),
      getHeadcountByCountry: vi.fn().mockReturnValue(of(mockHeadcountByCountry)),
      getTotalPayroll: vi.fn().mockReturnValue(of(mockTotalPayroll))
    };

    await TestBed.configureTestingModule({
      imports: [AnalyticsDashboardComponent],
      providers: [
        provideAnimationsAsync(),
        provideCharts(withDefaultRegisterables()),
        { provide: ApiService, useValue: mockApiService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AnalyticsDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and load initial analytics data', () => {
    expect(component).toBeTruthy();
    expect(component.isLoading).toBe(false);
    expect(component.hasError).toBe(false);
    expect(component.rawSalaryByCountry.length).toBe(3);
    expect(component.rawSalaryByDepartment.length).toBe(2);
  });

  it('should compute KPI values correctly from total payroll response', () => {
    expect(component.totalPayrollUsd).toBe(210000000);
    expect(component.totalHeadcount).toBe(3000);
    // Average salary = 210,000,000 / 3,000 = 70,000
    expect(component.averageSalaryUsd).toBe(70000);
    // Weighted median = (115000*1000 + 85000*500 + 28000*1500) / 3000 = (115M + 42.5M + 42M) / 3000 = 199.5M / 3000 = 66500
    expect(component.medianSalaryUsd).toBe(66500);
  });

  it('should construct country chart data correctly', () => {
    expect(component.countryChartData.labels).toEqual(['US', 'UK', 'India']);
    expect(component.countryChartData.datasets.length).toBe(2);
    expect(component.countryChartData.datasets[0].label).toBe('Average Salary (USD)');
    expect(component.countryChartData.datasets[0].data).toEqual([120000, 90000, 30000]);
    expect(component.countryChartData.datasets[1].label).toBe('Median Salary (USD)');
    expect(component.countryChartData.datasets[1].data).toEqual([115000, 85000, 28000]);
  });

  it('should construct department chart data correctly', () => {
    expect(component.departmentChartData.labels).toEqual(['Engineering', 'Sales']);
    expect(component.departmentChartData.datasets.length).toBe(2);
    expect(component.departmentChartData.datasets[0].data).toEqual([110000, 85000]);
    expect(component.departmentChartData.datasets[1].data).toEqual([105000, 82000]);
  });

  it('should filter chart data when country filter is changed', () => {
    component.onCountryFilterChange('US');
    expect(component.selectedCountry).toBe('US');
    expect(component.countryChartData.labels).toEqual(['US']);
    expect(component.countryChartData.datasets[0].data).toEqual([120000]);
  });

  it('should filter chart data when department filter is changed', () => {
    component.onDepartmentFilterChange('Engineering');
    expect(component.selectedDepartment).toBe('Engineering');
    expect(component.departmentChartData.labels).toEqual(['Engineering']);
    expect(component.departmentChartData.datasets[0].data).toEqual([110000]);
  });

  it('should reset filters back to ALL', () => {
    component.onCountryFilterChange('India');
    component.onDepartmentFilterChange('Sales');
    component.resetFilters();

    expect(component.selectedCountry).toBe('ALL');
    expect(component.selectedDepartment).toBe('ALL');
    expect(component.countryChartData.labels?.length).toBe(3);
  });

  it('should show error state if API fails', () => {
    mockApiService.getSalaryByCountry = vi.fn().mockReturnValue(throwError(() => new Error('Server Error')));
    component.loadAnalyticsData();

    expect(component.hasError).toBe(true);
    expect(component.isLoading).toBe(false);
    expect(component.errorMessage).toContain('Unable to fetch analytics data');
  });
});
