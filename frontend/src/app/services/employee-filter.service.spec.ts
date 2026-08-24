import { TestBed } from '@angular/core/testing';
import { EmployeeFilterService } from './employee-filter.service';
import { firstValueFrom } from 'rxjs';

describe('EmployeeFilterService', () => {
  let service: EmployeeFilterService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(EmployeeFilterService);
  });

  it('should be created with default pagination filters', () => {
    expect(service).toBeTruthy();
    expect(service.currentFilters.page).toBe(0);
    expect(service.currentFilters.size).toBe(20);
  });

  it('should update partial filter fields correctly', async () => {
    service.updateFilter({ country: 'India', minSalary: 50000 });

    const filters = await firstValueFrom(service.filters$);
    expect(filters.country).toBe('India');
    expect(filters.minSalary).toBe(50000);
    expect(filters.size).toBe(20);
  });

  it('should reset filters back to initial defaults', () => {
    service.updateFilter({ country: 'Germany', department: 'Engineering', name: 'Alice' });
    expect(service.currentFilters.country).toBe('Germany');

    service.resetFilters();
    expect(service.currentFilters.country).toBeUndefined();
    expect(service.currentFilters.department).toBeUndefined();
    expect(service.currentFilters.name).toBeUndefined();
    expect(service.currentFilters.page).toBe(0);
    expect(service.currentFilters.size).toBe(20);
  });
});
