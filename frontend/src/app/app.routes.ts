import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { LoginComponent } from './features/auth/login.component';
import { RegisterComponent } from './features/auth/register.component';
import { MainLayoutComponent } from './layout/main-layout.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, title: 'Entrar · FinControl' },
  { path: 'register', component: RegisterComponent, title: 'Criar conta · FinControl' },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
        title: 'Visão geral · FinControl',
      },
      {
        path: 'accounts',
        loadComponent: () => import('./features/accounts/accounts.component').then((m) => m.AccountsComponent),
        title: 'Contas · FinControl',
      },
      {
        path: 'transactions',
        loadComponent: () => import('./features/transactions/transactions.component').then((m) => m.TransactionsComponent),
        title: 'Transações · FinControl',
      },
      {
        path: 'categories',
        loadComponent: () => import('./features/categories/categories.component').then((m) => m.CategoriesComponent),
        title: 'Categorias · FinControl',
      },
      {
        path: 'budgets',
        loadComponent: () => import('./features/budgets/budgets.component').then((m) => m.BudgetsComponent),
        title: 'Orçamentos · FinControl',
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
