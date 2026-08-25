import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'employees',
    pathMatch: 'full'
  },
  {
    path: 'employees',
    loadComponent: () =>
      import('./components/employee-list/employee-list.component').then(
        m => m.EmployeeListComponent
      )
  },
  {
    path: 'employees/:id',
    loadComponent: () =>
      import('./components/employee-detail/employee-detail.component').then(
        m => m.EmployeeDetailComponent
      )
  },
  {
    path: 'analytics',
    loadComponent: () =>
      import('./components/analytics-dashboard/analytics-dashboard.component').then(
        m => m.AnalyticsDashboardComponent
      )
  }
];
