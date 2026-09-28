import { Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../core/auth/auth.service';
import { apiErrorMessage } from '../../core/api/api-error';

@Component({
  selector: 'fc-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <main class="auth-page">
      <section class="auth-aside">
        <a class="auth-brand" routerLink="/login"><span class="brand-mark">f</span> fincontrol</a>
        <div class="auth-pitch">
          <span class="eyebrow">CLAREZA PARA ESCOLHER</span>
          <h1>Seu dinheiro em<br><em>um só lugar.</em></h1>
          <p>Acompanhe o que entra, o que sai e o que realmente fica.</p>
        </div>
        <div class="auth-aside-foot">Gestão financeira feita para a vida real.</div>
      </section>

      <section class="auth-panel">
        <div class="auth-form-wrap">
          <span class="eyebrow mobile-eyebrow">BEM-VINDO DE VOLTA</span>
          <h2>Entre na sua conta</h2>
          <p class="auth-subtitle">Acesse seu espaço financeiro com segurança.</p>
          <div class="form-alert" role="alert" [hidden]="!error">{{ error }}</div>
          <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
            <mat-form-field appearance="outline">
              <mat-label>E-mail</mat-label>
              <input matInput type="email" formControlName="email" autocomplete="email" placeholder="voce@exemplo.com">
              @if (form.controls.email.touched && form.controls.email.hasError('required')) {
                <mat-error>Informe seu e-mail.</mat-error>
              } @else if (form.controls.email.touched && form.controls.email.hasError('email')) {
                <mat-error>Digite um e-mail válido.</mat-error>
              }
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Senha</mat-label>
              <input matInput type="password" formControlName="password" autocomplete="current-password">
              @if (form.controls.password.touched && form.controls.password.hasError('required')) {
                <mat-error>Informe sua senha.</mat-error>
              }
            </mat-form-field>
            <button mat-flat-button class="primary-button auth-submit" type="submit" [disabled]="loading">
              {{ loading ? 'Entrando…' : 'Entrar na minha conta' }}
            </button>
          </form>
          <p class="auth-switch">Ainda não tem conta? <a routerLink="/register">Criar minha conta</a></p>
          <div class="auth-secure">◈ <span>Conexão protegida e dados privados</span></div>
        </div>
      </section>
    </main>
  `,
})
export class LoginComponent {
  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  loading = false;
  error = '';

  constructor(private readonly formBuilder: FormBuilder, private readonly auth: AuthService, private readonly router: Router) {}

  submit(): void {
    this.error = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: () => void this.router.navigateByUrl('/dashboard'),
      error: (error: unknown) => {
        this.error = apiErrorMessage(error, 'Não foi possível entrar. Confira seus dados e tente novamente.');
        this.loading = false;
      },
    });
  }
}
