import { Component, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';

@Component({
  selector: 'fc-main-layout',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="app-shell">
      <aside class="sidebar" [class.sidebar-open]="menuOpen()">
        <a class="brand" routerLink="/dashboard" aria-label="FinControl, início">
          <span class="brand-mark">f</span>
          <span>fincontrol</span>
        </a>

        <div class="nav-label">VISÃO GERAL</div>
        <nav class="main-nav" aria-label="Navegação principal">
          <a routerLink="/dashboard" routerLinkActive="nav-active" (click)="closeMenu()">
            <span class="nav-icon">OV</span><span>Dashboard</span>
          </a>
          <a routerLink="/transactions" routerLinkActive="nav-active" (click)="closeMenu()">
            <span class="nav-icon">TX</span><span>Transações</span>
          </a>
          <a routerLink="/accounts" routerLinkActive="nav-active" (click)="closeMenu()">
            <span class="nav-icon">CT</span><span>Contas</span>
          </a>
          <a routerLink="/categories" routerLinkActive="nav-active" (click)="closeMenu()">
            <span class="nav-icon">CA</span><span>Categorias</span>
          </a>
          <a routerLink="/budgets" routerLinkActive="nav-active" (click)="closeMenu()">
            <span class="nav-icon">OR</span><span>Orçamentos</span>
          </a>
        </nav>

        <div class="sidebar-bottom">
          <div class="secure-note"><span class="secure-dot"></span><span>Seus dados estão protegidos</span></div>
          <button class="profile-button" type="button" (click)="logout()">
            <span class="avatar">FC</span>
            <span class="profile-copy"><strong>Minha conta</strong><small>Sair da aplicação</small></span>
            <span class="logout-mark">↗</span>
          </button>
        </div>
      </aside>

      <div class="mobile-scrim" [class.scrim-visible]="menuOpen()" (click)="closeMenu()"></div>
      <main class="main-area">
        <header class="topbar">
          <button class="menu-toggle" type="button" aria-label="Abrir menu" (click)="toggleMenu()">☰</button>
          <span class="topbar-caption">FINANÇAS PESSOAIS</span>
          <div class="topbar-right"><span class="status-dot"></span><span>Visão atualizada</span></div>
        </header>
        <section class="page-content"><router-outlet /></section>
      </main>
    </div>
  `,
})
export class MainLayoutComponent {
  readonly menuOpen = signal(false);

  constructor(private readonly auth: AuthService, private readonly router: Router) {}

  logout(): void {
    this.auth.logout();
    void this.router.navigateByUrl('/login');
  }
  toggleMenu(): void { this.menuOpen.update((open) => !open); }
  closeMenu(): void { this.menuOpen.set(false); }
}
