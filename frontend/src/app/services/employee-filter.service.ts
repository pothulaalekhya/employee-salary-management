import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { EmployeeFilterParams } from '../models/employee.model';

/**
 * ARCHITECTURAL DECISION NOTE:
 * Why RxJS BehaviorSubject is used instead of a full state management library (NgRx):
 *
 * For an application of this scale (single-domain HR management dashboard with localized search/filtering),
 * a lightweight RxJS BehaviorSubject service provides:
 * 1. Reactive uni-directional data streams with minimal boilerplate (no actions, reducers, effects, or selectors).
 * 2. Predictable, synchronous state access (`getValue()`) and asynchronous subscription via observables (`asObservable()`).
 * 3. Easy cross-component filter sharing between EmployeeListComponent, filter bars, and prospective analytics filters.
 * 4. Excellent testability and zero additional runtime bundle weight.
 */
@Injectable({
  providedIn: 'root'
})
export class EmployeeFilterService {
  private readonly initialFilters: EmployeeFilterParams = {
    page: 0,
    size: 20
  };

  private readonly filterSubject = new BehaviorSubject<EmployeeFilterParams>(this.initialFilters);
  public readonly filters$: Observable<EmployeeFilterParams> = this.filterSubject.asObservable();

  get currentFilters(): EmployeeFilterParams {
    return this.filterSubject.getValue();
  }

  setFilters(filters: EmployeeFilterParams): void {
    this.filterSubject.next({ ...filters });
  }

  updateFilter(partialFilters: Partial<EmployeeFilterParams>): void {
    const current = this.filterSubject.getValue();
    this.filterSubject.next({ ...current, ...partialFilters });
  }

  resetFilters(): void {
    this.filterSubject.next({ ...this.initialFilters });
  }
}
