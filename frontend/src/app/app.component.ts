import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class App implements OnInit {
  private readonly http = inject(HttpClient);
  
  protected readonly title = signal('ACME Salary Management');
  protected readonly backendStatus = signal<'checking' | 'online' | 'offline'>('checking');
  protected readonly backendUrl = 'http://localhost:8081/api/health';

  ngOnInit() {
    this.checkBackendHealth();
  }

  checkBackendHealth() {
    this.backendStatus.set('checking');
    this.http.get<{ status: string }>(this.backendUrl).subscribe({
      next: (data) => {
        if (data && data.status === 'ok') {
          this.backendStatus.set('online');
        } else {
          this.backendStatus.set('offline');
        }
      },
      error: () => {
        this.backendStatus.set('offline');
      }
    });
  }
}
