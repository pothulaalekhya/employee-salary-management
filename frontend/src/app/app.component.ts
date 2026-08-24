import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { ApiService } from './services/api.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class App implements OnInit {
  private readonly apiService = inject(ApiService);

  public backendOnline = false;
  public checkingHealth = true;

  ngOnInit(): void {
    this.checkBackendHealth();
  }

  public checkBackendHealth(): void {
    this.checkingHealth = true;
    this.apiService.getHealth().subscribe({
      next: res => {
        this.backendOnline = res && res.status === 'ok';
        this.checkingHealth = false;
      },
      error: () => {
        this.backendOnline = false;
        this.checkingHealth = false;
      }
    });
  }
}
