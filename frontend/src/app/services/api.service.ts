import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Employee,
  SalaryHistory,
  PagedResponse,
  EmployeeFilterParams,
  SalaryByCountry,
  SalaryByDepartment,
  HeadcountByCountry,
  TotalPayroll
} from '../models/employee.model';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly http = inject(HttpClient);

  /**
   * Resolves the backend base API URL:
   * 1. Window runtime injection (__env.API_URL) if set.
   * 2. When running in containerized production (via Nginx proxy): relative '/api'.
   * 3. Default for local standalone development: 'http://localhost:8081/api'.
   */
  private get baseUrl(): string {
    if (typeof window !== 'undefined') {
      const windowEnv = (window as unknown as { __env?: { API_URL?: string } })?.__env;
      if (windowEnv?.API_URL) {
        return windowEnv.API_URL.replace(/\/+$/, '');
      }
      // If served via Nginx on standard port or container port other than dev 4200
      if (window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1') {
        return '/api';
      }
    }
    return 'http://localhost:8081/api';
  }

  getHealth(): Observable<{ status: string }> {
    return this.http.get<{ status: string }>(`${this.baseUrl}/health`);
  }

  getEmployees(params: EmployeeFilterParams = {}): Observable<PagedResponse<Employee>> {
    let httpParams = new HttpParams();

    if (params.country && params.country.trim()) {
      httpParams = httpParams.set('country', params.country.trim());
    }
    if (params.department && params.department.trim()) {
      httpParams = httpParams.set('department', params.department.trim());
    }
    if (params.minSalary !== undefined && params.minSalary !== null) {
      httpParams = httpParams.set('minSalary', params.minSalary.toString());
    }
    if (params.maxSalary !== undefined && params.maxSalary !== null) {
      httpParams = httpParams.set('maxSalary', params.maxSalary.toString());
    }
    if (params.name && params.name.trim()) {
      httpParams = httpParams.set('name', params.name.trim());
    }
    if (params.page !== undefined && params.page !== null) {
      httpParams = httpParams.set('page', params.page.toString());
    }
    if (params.size !== undefined && params.size !== null) {
      httpParams = httpParams.set('size', params.size.toString());
    }
    if (params.sort && params.sort.trim()) {
      httpParams = httpParams.set('sort', params.sort.trim());
    }

    return this.http.get<PagedResponse<Employee>>(`${this.baseUrl}/employees`, { params: httpParams });
  }

  getEmployeeById(id: number): Observable<Employee> {
    return this.http.get<Employee>(`${this.baseUrl}/employees/${id}`);
  }

  getSalaryHistory(employeeId: number): Observable<SalaryHistory[]> {
    return this.http.get<SalaryHistory[]>(`${this.baseUrl}/employees/${employeeId}/salary-history`);
  }

  createEmployee(employee: Partial<Employee> & { initialSalary: number; effectiveDate?: string; note?: string }): Observable<Employee> {
    return this.http.post<Employee>(`${this.baseUrl}/employees`, employee);
  }

  updateEmployee(id: number, employee: Partial<Employee>): Observable<Employee> {
    return this.http.put<Employee>(`${this.baseUrl}/employees/${id}`, employee);
  }

  updateSalary(id: number, salaryUpdate: { newSalary: number; effectiveDate?: string; note?: string }): Observable<Employee> {
    return this.http.patch<Employee>(`${this.baseUrl}/employees/${id}/salary`, salaryUpdate);
  }

  deleteEmployee(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/employees/${id}`);
  }

  getSalaryByCountry(): Observable<SalaryByCountry[]> {
    return this.http.get<SalaryByCountry[]>(`${this.baseUrl}/analytics/salary-by-country`);
  }

  getSalaryByDepartment(): Observable<SalaryByDepartment[]> {
    return this.http.get<SalaryByDepartment[]>(`${this.baseUrl}/analytics/salary-by-department`);
  }

  getHeadcountByCountry(): Observable<HeadcountByCountry[]> {
    return this.http.get<HeadcountByCountry[]>(`${this.baseUrl}/analytics/headcount-by-country`);
  }

  getTotalPayroll(): Observable<TotalPayroll> {
    return this.http.get<TotalPayroll>(`${this.baseUrl}/analytics/total-payroll`);
  }
}
