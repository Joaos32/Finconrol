import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AuthResponse } from '../models/finance.models';

const TOKEN_KEY = 'fincontrol.access-token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly token = signal<string | null>(sessionStorage.getItem(TOKEN_KEY));
  readonly isAuthenticated = signal(this.token() !== null);

  constructor(private readonly http: HttpClient) {}

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', { email, password }).pipe(
      tap((response) => this.saveToken(response.accessToken)),
    );
  }

  register(name: string, email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/register', { name, email, password }).pipe(
      tap((response) => this.saveToken(response.accessToken)),
    );
  }

  logout(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    this.token.set(null);
    this.isAuthenticated.set(false);
  }

  private saveToken(token: string): void {
    sessionStorage.setItem(TOKEN_KEY, token);
    this.token.set(token);
    this.isAuthenticated.set(true);
  }
}
