export interface Employee {
  id: number;
  employeeCode: string;
  name: string;
  country: string;
  department: string;
  title?: string;
  currency: string;
  currentSalary: number;
  active: boolean;
  createdAt: string;
}

export interface SalaryHistory {
  id: number;
  employeeId: number;
  amount: number;
  currency: string;
  effectiveDate: string;
  changedAt: string;
  note?: string;
}

export interface ExchangeRate {
  id: number;
  currencyCode: string;
  rateToBase: number;
  baseCurrency: string;
  updatedAt: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface EmployeeFilterParams {
  country?: string;
  department?: string;
  minSalary?: number;
  maxSalary?: number;
  name?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface SalaryByCountry {
  country: string;
  currency: string;
  employeeCount: number;
  avgSalaryUsd: number;
  medianSalaryUsd: number;
  minSalaryUsd: number;
  maxSalaryUsd: number;
}

export interface SalaryByDepartment {
  department: string;
  employeeCount: number;
  avgSalaryUsd: number;
  medianSalaryUsd: number;
  minSalaryUsd: number;
  maxSalaryUsd: number;
}

export interface HeadcountByCountry {
  country: string;
  headcount: number;
  percentage: number;
}

export interface TotalPayroll {
  totalPayrollUsd: number;
  totalEmployees: number;
  countryBreakdown: {
    country: string;
    totalPayrollUsd: number;
    employeeCount: number;
  }[];
}
