import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';

import { ApiService } from '../../services/api.service';
import { CurrencyFormatPipe } from '../../pipes/currency-format.pipe';
import { Employee, SalaryHistory } from '../../models/employee.model';

@Component({
  selector: 'app-employee-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatDividerModule,
    MatTooltipModule,
    CurrencyFormatPipe
  ],
  templateUrl: './employee-detail.component.html',
  styleUrls: ['./employee-detail.component.css']
})
export class EmployeeDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly apiService = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  public employeeId!: number;
  public employee: Employee | null = null;
  public salaryHistory: SalaryHistory[] = [];

  public isLoading = true;
  public isSavingProfile = false;
  public isSavingSalary = false;

  public profileFeedbackMessage: string | null = null;
  public profileFeedbackType: 'success' | 'error' | null = null;

  public salaryFeedbackMessage: string | null = null;
  public salaryFeedbackType: 'success' | 'error' | null = null;

  public readonly displayedHistoryColumns: string[] = [
    'effectiveDate',
    'amount',
    'changedAt',
    'note'
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

  public editForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    country: ['', [Validators.required]],
    department: ['', [Validators.required]],
    title: ['', [Validators.maxLength(150)]]
  });

  public salaryForm: FormGroup = this.fb.group({
    newSalary: [null, [Validators.required, Validators.min(0.01)]],
    effectiveDate: [new Date().toISOString().substring(0, 10), [Validators.required]],
    note: ['', [Validators.maxLength(255)]]
  }, {
    validators: (group: AbstractControl): ValidationErrors | null => {
      const newSalary = group.get('newSalary')?.value;
      if (this.employee && newSalary !== null && newSalary !== undefined) {
        if (Number(newSalary) === Number(this.employee.currentSalary)) {
          return { sameSalary: true };
        }
      }
      return null;
    }
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.employeeId = Number(idParam);
      this.loadEmployeeData();
    }
  }

  public loadEmployeeData(): void {
    this.isLoading = true;
    this.apiService.getEmployeeById(this.employeeId).subscribe({
      next: emp => {
        this.employee = emp;
        this.editForm.patchValue({
          name: emp.name,
          country: emp.country,
          department: emp.department,
          title: emp.title || ''
        });
        this.salaryForm.patchValue({
          newSalary: null,
          effectiveDate: new Date().toISOString().substring(0, 10),
          note: ''
        });
        this.salaryForm.markAsPristine();
        this.salaryForm.markAsUntouched();
        this.loadSalaryHistory();
      },
      error: err => {
        console.error('Failed to load employee', err);
        this.isLoading = false;
        this.profileFeedbackMessage = 'Employee not found or failed to load.';
        this.profileFeedbackType = 'error';
      }
    });
  }

  public loadSalaryHistory(): void {
    this.apiService.getSalaryHistory(this.employeeId).subscribe({
      next: history => {
        this.salaryHistory = history || [];
        this.isLoading = false;
      },
      error: err => {
        console.error('Failed to load salary history', err);
        this.isLoading = false;
      }
    });
  }

  public onSaveProfile(): void {
    if (this.editForm.invalid || !this.employee) {
      this.editForm.markAllAsTouched();
      return;
    }

    this.isSavingProfile = true;
    this.profileFeedbackMessage = null;

    const payload = {
      name: this.editForm.value.name.trim(),
      country: this.editForm.value.country,
      department: this.editForm.value.department,
      title: this.editForm.value.title ? this.editForm.value.title.trim() : null
    };

    this.apiService.updateEmployee(this.employeeId, payload).subscribe({
      next: updated => {
        this.employee = updated;
        this.isSavingProfile = false;
        this.profileFeedbackMessage = 'Employee profile successfully updated.';
        this.profileFeedbackType = 'success';
        this.editForm.markAsPristine();
      },
      error: err => {
        this.isSavingProfile = false;
        this.profileFeedbackMessage = err?.error?.message || 'Failed to update employee profile.';
        this.profileFeedbackType = 'error';
      }
    });
  }

  public onUpdateSalary(): void {
    if (this.salaryForm.invalid || !this.employee) {
      this.salaryForm.markAllAsTouched();
      return;
    }

    const newSalaryValue = Number(this.salaryForm.value.newSalary);
    if (newSalaryValue === Number(this.employee.currentSalary)) {
      this.salaryFeedbackMessage = 'New salary must be different from current salary.';
      this.salaryFeedbackType = 'error';
      return;
    }

    this.isSavingSalary = true;
    this.salaryFeedbackMessage = null;

    const payload = {
      newSalary: newSalaryValue,
      effectiveDate: this.salaryForm.value.effectiveDate || undefined,
      note: this.salaryForm.value.note ? this.salaryForm.value.note.trim() : undefined
    };

    this.apiService.updateSalary(this.employeeId, payload).subscribe({
      next: updated => {
        this.employee = updated;
        this.isSavingSalary = false;
        this.salaryFeedbackMessage = `Salary updated successfully to ${updated.currentSalary} ${updated.currency}.`;
        this.salaryFeedbackType = 'success';
        this.salaryForm.reset({
          newSalary: null,
          effectiveDate: new Date().toISOString().substring(0, 10),
          note: ''
        });
        this.salaryForm.markAsPristine();
        this.salaryForm.markAsUntouched();
        // Immediately refresh salary history table without full reload
        this.loadSalaryHistory();
      },
      error: err => {
        this.isSavingSalary = false;
        this.salaryFeedbackMessage = err?.error?.message || 'Failed to update employee salary.';
        this.salaryFeedbackType = 'error';
      }
    });
  }
}
