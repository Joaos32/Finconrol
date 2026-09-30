import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Account, AccountRequest, Budget, BudgetRequest, Category, CategoryRequest, CreditCard,
  CreditCardInvoice, CreditCardRequest, DashboardSummary,
  ExpenseCategory, MonthlyPoint, Transaction, TransactionFilters,
  TransactionPage, TransactionRequest,
} from '../models/finance.models';

@Injectable({ providedIn: 'root' })
export class FinanceApiService {
  constructor(private readonly http: HttpClient) {}

  accounts(): Observable<Account[]> {
    return this.http.get<Account[]>('/api/accounts');
  }
  saveAccount(request: AccountRequest, id?: string): Observable<Account> {
    return id ? this.http.put<Account>('/api/accounts/' + id, request)
      : this.http.post<Account>('/api/accounts', request);
  }
  deleteAccount(id: string): Observable<void> {
    return this.http.delete<void>('/api/accounts/' + id);
  }
  categories(type?: string): Observable<Category[]> {
    const params = type ? new HttpParams().set('type', type) : undefined;
    return this.http.get<Category[]>('/api/categories', { params });
  }
  saveCategory(request: CategoryRequest, id?: string): Observable<Category> {
    return id ? this.http.put<Category>('/api/categories/' + id, request)
      : this.http.post<Category>('/api/categories', request);
  }
  deleteCategory(id: string): Observable<void> {
    return this.http.delete<void>('/api/categories/' + id);
  }
  budgets(month: string): Observable<Budget[]> {
    return this.http.get<Budget[]>('/api/budgets', { params: new HttpParams().set('month', month) });
  }
  saveBudget(request: BudgetRequest, id?: string): Observable<Budget> {
    return id ? this.http.put<Budget>('/api/budgets/' + id, request)
      : this.http.post<Budget>('/api/budgets', request);
  }
  deleteBudget(id: string): Observable<void> {
    return this.http.delete<void>('/api/budgets/' + id);
  }
  creditCards(): Observable<CreditCard[]> {
    return this.http.get<CreditCard[]>('/api/credit-cards');
  }
  saveCreditCard(request: CreditCardRequest, id?: string): Observable<CreditCard> {
    return id ? this.http.put<CreditCard>('/api/credit-cards/' + id, request)
      : this.http.post<CreditCard>('/api/credit-cards', request);
  }
  deleteCreditCard(id: string): Observable<void> {
    return this.http.delete<void>('/api/credit-cards/' + id);
  }
  creditCardInvoice(cardId: string, month: string): Observable<CreditCardInvoice> {
    return this.http.get<CreditCardInvoice>('/api/credit-cards/' + cardId + '/invoices', {
      params: new HttpParams().set('month', month),
    });
  }
  payCreditCardInvoice(cardId: string, invoiceId: string, accountId: string): Observable<CreditCardInvoice> {
    return this.http.post<CreditCardInvoice>(
      '/api/credit-cards/' + cardId + '/invoices/' + invoiceId + '/payments', { accountId },
    );
  }
  transactions(filters: TransactionFilters = {}): Observable<TransactionPage> {
    let params = new HttpParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    });
    return this.http.get<TransactionPage>('/api/transactions', { params });
  }
  saveTransaction(request: TransactionRequest, id?: string): Observable<Transaction> {
    return id ? this.http.put<Transaction>('/api/transactions/' + id, request)
      : this.http.post<Transaction>('/api/transactions', request);
  }
  deleteTransaction(id: string): Observable<void> {
    return this.http.delete<void>('/api/transactions/' + id);
  }
  dashboardSummary(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>('/api/dashboard/summary');
  }
  expenseCategories(): Observable<ExpenseCategory[]> {
    return this.http.get<ExpenseCategory[]>('/api/dashboard/expenses-by-category');
  }
  monthlyEvolution(): Observable<MonthlyPoint[]> {
    return this.http.get<MonthlyPoint[]>('/api/dashboard/monthly-evolution');
  }
  recentTransactions(): Observable<Transaction[]> {
    return this.http.get<Transaction[]>('/api/dashboard/recent-transactions');
  }
}
