import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { ApiService } from '../../services/api.service';
import { EmployeeFilterService } from '../../services/employee-filter.service';
import { CurrencyFormatPipe } from '../../pipes/currency-format.pipe';
import { Employee, EmployeeFilterParams, PagedResponse } from '../../models/employee.model';

@Component({
  selector: 'app-employee-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatCardModule,
    MatTooltipModule,
    CurrencyFormatPipe
  ],
  templateUrl: './employee-list.component.html',
  styleUrls: ['./employee-list.component.css']
})
export class EmployeeListComponent implements OnInit, OnDestroy {
  private readonly apiService = inject(ApiService);
  private readonly filterService = inject(EmployeeFilterService);

  public readonly displayedColumns: string[] = [
    'employeeCode',
    'name',
    'country',
    'department',
    'title',
    'currentSalary',
    'active'
  ];

  public readonly countries: string[] = [
    'US',
    'UK',
    'India',
    'Germany',
    'Brazil',
    'Japan',
    'Australia',
    'Canada'
  ];

  public readonly departments: string[] = [
    'Engineering',
    'Sales',
    'Marketing',
    'HR',
    'Finance',
    'Operations'
  ];

  public employees: Employee[] = [];
  public totalElements = 0;
  public pageSize = 20;
  public pageIndex = 0;
  public isLoading = false;

  // Filter form models
  public selectedCountry = '';
  public selectedDepartment = '';
  public minSalary: number | null = null;
  public maxSalary: number | null = null;
  public searchTerm = '';

  private readonly searchSubject = new Subject<string>();
  private filterSubscription?: Subscription;
  private searchSubscription?: Subscription;

  ngOnInit(): void {
    // Setup debounced name search (~300ms)
    this.searchSubscription = this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(name => {
        this.filterService.updateFilter({ name, page: 0 });
      });

    // Subscribe to filter state changes
    this.filterSubscription = this.filterService.filters$.subscribe(filters => {
      this.selectedCountry = filters.country || '';
      this.selectedDepartment = filters.department || '';
      this.minSalary = filters.minSalary ?? null;
      this.maxSalary = filters.maxSalary ?? null;
      this.searchTerm = filters.name || '';
      this.pageIndex = filters.page || 0;
      this.pageSize = filters.size || 20;

      this.loadEmployees(filters);
    });
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
    this.filterSubscription?.unsubscribe();
  }

  public loadEmployees(filters: EmployeeFilterParams): void {
    this.isLoading = true;
    this.apiService.getEmployees(filters).subscribe({
      next: (response: PagedResponse<Employee>) => {
        this.employees = response.content || [];
        this.totalElements = response.totalElements || 0;
        this.pageIndex = response.number || 0;
        this.pageSize = response.size || 20;
        this.isLoading = false;
      },
      error: err => {
        console.error('Failed to fetch employees', err);
        this.employees = [];
        this.totalElements = 0;
        this.isLoading = false;
      }
    });
  }

  public onSearchInput(value: string): void {
    this.searchSubject.next(value);
  }

  public onCountryChange(country: string): void {
    this.filterService.updateFilter({
      country: country || undefined,
      page: 0
    });
  }

  public onDepartmentChange(department: string): void {
    this.filterService.updateFilter({
      department: department || undefined,
      page: 0
    });
  }

  public onSalaryRangeChange(): void {
    this.filterService.updateFilter({
      minSalary: this.minSalary !== null ? this.minSalary : undefined,
      maxSalary: this.maxSalary !== null ? this.maxSalary : undefined,
      page: 0
    });
  }

  public onPageChange(event: PageEvent): void {
    this.filterService.updateFilter({
      page: event.pageIndex,
      size: event.pageSize
    });
  }

  public onSortChange(sort: Sort): void {
    let sortParam: string | undefined;
    if (sort.active && sort.direction) {
      sortParam = `${sort.active},${sort.direction}`;
    }
    this.filterService.updateFilter({
      sort: sortParam,
      page: 0
    });
  }

  public resetFilters(): void {
    this.selectedCountry = '';
    this.selectedDepartment = '';
    this.minSalary = null;
    this.maxSalary = null;
    this.searchTerm = '';
    this.filterService.resetFilters();
  }
}
