import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';
import { forkJoin } from 'rxjs';

import { ApiService } from '../../services/api.service';
import { CurrencyFormatPipe } from '../../pipes/currency-format.pipe';
import {
  SalaryByCountry,
  SalaryByDepartment,
  HeadcountByCountry,
  TotalPayroll
} from '../../models/employee.model';

@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatSelectModule,
    MatFormFieldModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatChipsModule,
    MatTooltipModule,
    BaseChartDirective,
    CurrencyFormatPipe
  ],
  templateUrl: './analytics-dashboard.component.html',
  styleUrls: ['./analytics-dashboard.component.css']
})
export class AnalyticsDashboardComponent implements OnInit {
  private readonly apiService = inject(ApiService);

  public isLoading = true;
  public hasError = false;
  public errorMessage = '';

  // KPI Card Values (Org-wide)
  public totalPayrollUsd = 0;
  public totalHeadcount = 0;
  public averageSalaryUsd = 0;
  public medianSalaryUsd = 0;

  // Raw API Datasets
  public rawSalaryByCountry: SalaryByCountry[] = [];
  public rawSalaryByDepartment: SalaryByDepartment[] = [];
  public rawHeadcountByCountry: HeadcountByCountry[] = [];
  public rawTotalPayroll: TotalPayroll | null = null;

  // Filter Models (Client-side interactive filtering)
  public selectedCountry = 'ALL';
  public selectedDepartment = 'ALL';

  public countries: string[] = [];
  public departments: string[] = [];

  // Filtered Country Table Columns
  public readonly tableColumns: string[] = [
    'country',
    'currency',
    'employeeCount',
    'avgSalaryUsd',
    'medianSalaryUsd',
    'minSalaryUsd',
    'maxSalaryUsd'
  ];

  // Chart 1: Salary by Country (Bar Chart)
  public countryChartType: ChartType = 'bar';
  public countryChartData: ChartData<'bar'> = {
    labels: [],
    datasets: []
  };
  public countryChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { position: 'top' },
      tooltip: {
        callbacks: {
          label: (context) => {
            const val = context.parsed.y ?? 0;
            return ` ${context.dataset.label}: $${val.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
          }
        }
      }
    },
    scales: {
      y: {
        beginAtZero: true,
        ticks: {
          callback: (value) => `$${Number(value).toLocaleString()}`
        }
      }
    }
  };

  // Chart 2: Headcount by Country (Doughnut Chart)
  public headcountChartType: ChartType = 'doughnut';
  public headcountChartData: ChartData<'doughnut'> = {
    labels: [],
    datasets: []
  };
  public headcountChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { position: 'right' },
      tooltip: {
        callbacks: {
          label: (context) => {
            const count = context.parsed ?? 0;
            return ` Headcount: ${count.toLocaleString()} employees`;
          }
        }
      }
    }
  };

  // Chart 3: Salary by Department (Bar Chart)
  public departmentChartType: ChartType = 'bar';
  public departmentChartData: ChartData<'bar'> = {
    labels: [],
    datasets: []
  };
  public departmentChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { position: 'top' },
      tooltip: {
        callbacks: {
          label: (context) => {
            const val = context.parsed.y ?? 0;
            return ` ${context.dataset.label}: $${val.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
          }
        }
      }
    },
    scales: {
      y: {
        beginAtZero: true,
        ticks: {
          callback: (value) => `$${Number(value).toLocaleString()}`
        }
      }
    }
  };

  ngOnInit(): void {
    this.loadAnalyticsData();
  }

  public loadAnalyticsData(): void {
    this.isLoading = true;
    this.hasError = false;
    this.errorMessage = '';

    forkJoin({
      salaryByCountry: this.apiService.getSalaryByCountry(),
      salaryByDepartment: this.apiService.getSalaryByDepartment(),
      headcountByCountry: this.apiService.getHeadcountByCountry(),
      totalPayroll: this.apiService.getTotalPayroll()
    }).subscribe({
      next: ({ salaryByCountry, salaryByDepartment, headcountByCountry, totalPayroll }) => {
        this.rawSalaryByCountry = salaryByCountry || [];
        this.rawSalaryByDepartment = salaryByDepartment || [];
        this.rawHeadcountByCountry = headcountByCountry || [];
        this.rawTotalPayroll = totalPayroll || null;

        this.countries = Array.from(new Set(this.rawSalaryByCountry.map(c => c.country))).sort();
        this.departments = Array.from(new Set(this.rawSalaryByDepartment.map(d => d.department))).sort();

        this.computeKpis();
        this.updateCharts();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load analytics data', err);
        this.hasError = true;
        this.errorMessage = 'Unable to fetch analytics data from server. Please verify backend service status.';
        this.isLoading = false;
      }
    });
  }

  /**
   * Computes org-wide KPIs:
   * - Total Payroll (USD)
   * - Total Headcount
   * - Average Salary (USD = Total Payroll / Total Headcount)
   * - Median Salary (USD): Weighted median across country datasets
   */
  public computeKpis(): void {
    if (this.rawTotalPayroll) {
      this.totalPayrollUsd = this.rawTotalPayroll.totalPayrollUsd || 0;
      this.totalHeadcount = this.rawTotalPayroll.totalEmployees || 0;
    } else {
      this.totalHeadcount = this.rawHeadcountByCountry.reduce((acc, curr) => acc + curr.headcount, 0);
      this.totalPayrollUsd = this.rawSalaryByCountry.reduce((acc, curr) => acc + (curr.avgSalaryUsd * curr.employeeCount), 0);
    }

    this.averageSalaryUsd = this.totalHeadcount > 0 ? this.totalPayrollUsd / this.totalHeadcount : 0;

    // Org-wide median estimate computed from weighted country medians
    if (this.rawSalaryByCountry.length > 0 && this.totalHeadcount > 0) {
      const weightedSum = this.rawSalaryByCountry.reduce(
        (sum, item) => sum + (item.medianSalaryUsd * item.employeeCount),
        0
      );
      this.medianSalaryUsd = weightedSum / this.totalHeadcount;
    } else {
      this.medianSalaryUsd = this.averageSalaryUsd;
    }
  }

  /**
   * Updates Chart.js data models according to current filter selections.
   */
  public updateCharts(): void {
    // 1. Filtered Country Data
    const countryData = this.selectedCountry === 'ALL'
      ? this.rawSalaryByCountry
      : this.rawSalaryByCountry.filter(c => c.country === this.selectedCountry);

    this.countryChartData = {
      labels: countryData.map(c => c.country),
      datasets: [
        {
          label: 'Average Salary (USD)',
          data: countryData.map(c => c.avgSalaryUsd),
          backgroundColor: '#3b82f6',
          borderRadius: 6
        },
        {
          label: 'Median Salary (USD)',
          data: countryData.map(c => c.medianSalaryUsd),
          backgroundColor: '#10b981',
          borderRadius: 6
        }
      ]
    };

    // 2. Headcount Chart Data
    const headcountData = this.selectedCountry === 'ALL'
      ? this.rawHeadcountByCountry
      : this.rawHeadcountByCountry.filter(h => h.country === this.selectedCountry);

    const palette = [
      '#3b82f6', '#10b981', '#f59e0b', '#8b5cf6',
      '#ec4899', '#06b6d4', '#6366f1', '#14b8a6'
    ];

    this.headcountChartData = {
      labels: headcountData.map(h => `${h.country} (${h.percentage}%)`),
      datasets: [
        {
          data: headcountData.map(h => h.headcount),
          backgroundColor: palette.slice(0, headcountData.length),
          borderWidth: 2,
          borderColor: '#ffffff'
        }
      ]
    };

    // 3. Filtered Department Data
    const departmentData = this.selectedDepartment === 'ALL'
      ? this.rawSalaryByDepartment
      : this.rawSalaryByDepartment.filter(d => d.department === this.selectedDepartment);

    this.departmentChartData = {
      labels: departmentData.map(d => d.department),
      datasets: [
        {
          label: 'Average Salary (USD)',
          data: departmentData.map(d => d.avgSalaryUsd),
          backgroundColor: '#8b5cf6',
          borderRadius: 6
        },
        {
          label: 'Median Salary (USD)',
          data: departmentData.map(d => d.medianSalaryUsd),
          backgroundColor: '#f59e0b',
          borderRadius: 6
        }
      ]
    };
  }

  public onCountryFilterChange(country: string): void {
    this.selectedCountry = country;
    this.updateCharts();
  }

  public onDepartmentFilterChange(department: string): void {
    this.selectedDepartment = department;
    this.updateCharts();
  }

  public resetFilters(): void {
    this.selectedCountry = 'ALL';
    this.selectedDepartment = 'ALL';
    this.updateCharts();
  }

  public get filteredCountryTableData(): SalaryByCountry[] {
    return this.selectedCountry === 'ALL'
      ? this.rawSalaryByCountry
      : this.rawSalaryByCountry.filter(c => c.country === this.selectedCountry);
  }
}
