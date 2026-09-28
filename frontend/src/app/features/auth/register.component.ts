import { Component } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../core/auth/auth.service';
import { apiErrorMessage } from '../../core/api/api-error';

function matchingPasswords(control: AbstractControl): ValidationErrors | null {
  return control.get('password')?.value === control.get('confirmPassword')?.value ? null : { passwordMismatch: true };
}

@Component({
  selector: 'fc-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <main class="auth-page">
      <section class="auth-aside">
        <a class="auth-brand" routerLink="/login"><span class="brand-mark">f</span> fincontrol</a>
        <div class="auth-pitch">
          <span class="eyebrow">UM BOM COMEÇO</span>
          <h1>Mais leveza<br><em>nas suas escolhas.</em></h1>
          <p>Organize sua vida financeira com clareza e sem complicação.</p>
        </div>
        <div class="auth-aside-foot">Seu próximo passo começa aqui.</div>
      </section>
      <section class="auth-panel">
        <div class="auth-form-wrap">
          <span class="eyebrow mobile-eyebrow">COMECE POR AQUI</span>
          <h2>Crie sua conta</h2>
          <p class="auth-subtitle">Leva menos de um minuto para começar.</p>
          <div class="form-alert" role="alert" [hidden]="!error">{{ error }}</div>
          <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
            <mat-form-field appearance="outline">
              <mat-label>Nome</mat-label>
              <input matInput formControlName="name" autocomplete="name" placeholder="Como podemos chamar você?">
              @if (form.controls.name.touched && form.controls.name.hasError('required')) {
                <mat-error>Informe seu nome.</mat-error>
              }
            </mat-form-field>
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
              <input matInput type="password" formControlName="password" autocomplete="new-password">
              @if (form.controls.password.touched && form.controls.password.hasError('minlength')) {
                <mat-error>Use pelo menos 8 caracteres.</mat-error>
              } @else if (form.controls.password.touched && form.controls.password.hasError('required')) {
                <mat-error>Informe uma senha.</mat-error>
              }
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Confirme sua senha</mat-label>
              <input matInput type="password" formControlName="confirmPassword" autocomplete="new-password">
              @if (form.controls.confirmPassword.touched && form.hasError('passwordMismatch')) {
                <mat-error>As senhas precisam ser iguais.</mat-error>
              }
            </mat-form-field>
            <button mat-flat-button class="primary-button auth-submit" type="submit" [disabled]="loading">
              {{ loading ? 'Criando sua conta…' : 'Criar minha conta' }}
            </button>
          </form>
          <p class="auth-switch">Já tem uma conta? <a routerLink="/login">Entrar</a></p>
          <div class="auth-secure">◈ <span>Seus dados ficam privados e protegidos</span></div>
        </div>
      </section>
    </main>
  `,
})
export class RegisterComponent {
  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    confirmPassword: ['', Validators.required],
  }, { validators: matchingPasswords });
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
    const { name, email, password } = this.form.getRawValue();
    this.auth.register(name, email, password).subscribe({
      next: () => void this.router.navigateByUrl('/dashboard'),
      error: (error: unknown) => {
        this.error = apiErrorMessage(error, 'Não foi possível criar sua conta. Tente novamente.');
        this.loading = false;
      },
    });
  }
}
