import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { forkJoin } from 'rxjs';
import { FinanceApiService } from '../../core/api/finance-api.service';
import { apiErrorMessage } from '../../core/api/api-error';
import {
  Account, Category, Transaction, TransactionPage, TransactionType,
} from '../../core/models/finance.models';

@Component({
  selector: 'fc-transactions',
  standalone: true,
  imports: [CurrencyPipe, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <div class="page-heading">
      <div><span class="eyebrow">MOVIMENTAÇÕES</span><h1>Transações</h1><p>Receitas e despesas em um histórico simples de acompanhar.</p></div>
      @if (!formOpen()) { <button class="button button-primary" type="button" (click)="openCreate()">＋ Nova transação</button> }
    </div>
    @if (notice()) { <div class="notice" [class.notice-error]="noticeError()" role="status">{{ notice() }}</div> }

    @if (formOpen()) {
      <section class="panel form-panel">
        <div class="panel-heading"><div><h2>{{ editingId() ? 'Editar transação' : 'Nova transação' }}</h2><p>Preencha os detalhes da movimentação.</p></div></div>
        <form [formGroup]="form" (ngSubmit)="save()" class="form-grid form-grid-3">
          <mat-form-field appearance="outline"><mat-label>Descrição</mat-label><input matInput formControlName="description" placeholder="Ex.: Mercado da semana"><mat-error>Informe uma descrição.</mat-error></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Tipo</mat-label><mat-select formControlName="type"><mat-option value="EXPENSE">Despesa</mat-option><mat-option value="INCOME">Receita</mat-option></mat-select></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Valor</mat-label><span matTextPrefix>R$&nbsp;</span><input matInput type="number" min="0.01" step="0.01" formControlName="amount"><mat-error>O valor deve ser maior que zero.</mat-error></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Conta</mat-label><mat-select formControlName="accountId"><mat-option value="">Selecione uma conta</mat-option>@for (account of accounts(); track account.id) { <mat-option [value]="account.id">{{ account.name }}</mat-option> }</mat-select><mat-error>Selecione uma conta.</mat-error></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Categoria</mat-label><mat-select formControlName="categoryId"><mat-option value="">Selecione uma categoria</mat-option>@for (category of filteredCategories(); track category.id) { <mat-option [value]="category.id">{{ category.name }}</mat-option> }</mat-select><mat-error>Selecione uma categoria compatível.</mat-error></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Data</mat-label><input matInput type="date" formControlName="transactionDate"><mat-error>Informe a data.</mat-error></mat-form-field>
          <div class="form-actions"><button class="button button-quiet" type="button" (click)="closeForm()">Cancelar</button><button class="button button-primary" type="submit" [disabled]="saving()">{{ saving() ? 'Salvando…' : 'Salvar transação' }}</button></div>
        </form>
      </section>
    }

    <section class="panel filters-panel">
      <div class="panel-heading"><div><h2>Filtrar histórico</h2><p>Encontre uma movimentação por período, tipo ou conta.</p></div></div>
      <form [formGroup]="filters" (ngSubmit)="applyFilters()" class="filter-grid">
        <mat-form-field appearance="outline"><mat-label>De</mat-label><input matInput type="date" formControlName="startDate"></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Até</mat-label><input matInput type="date" formControlName="endDate"></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Tipo</mat-label><mat-select formControlName="type"><mat-option value="">Todos</mat-option><mat-option value="INCOME">Receitas</mat-option><mat-option value="EXPENSE">Despesas</mat-option></mat-select></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Conta</mat-label><mat-select formControlName="accountId"><mat-option value="">Todas</mat-option>@for (account of accounts(); track account.id) { <mat-option [value]="account.id">{{ account.name }}</mat-option> }</mat-select></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Categoria</mat-label><mat-select formControlName="categoryId"><mat-option value="">Todas</mat-option>@for (category of categories(); track category.id) { <mat-option [value]="category.id">{{ category.name }}</mat-option> }</mat-select></mat-form-field>
        <div class="filter-actions"><button class="button button-quiet" type="button" (click)="clearFilters()">Limpar</button><button class="button button-primary" type="submit">Aplicar filtros</button></div>
      </form>
    </section>

    <section class="panel table-panel">
      <div class="panel-heading">
        <div><h2>Histórico</h2><p>{{ page()?.totalElements ?? 0 }} movimentações encontradas</p></div>
      </div>
      @if (loading()) {
        <div class="list-skeleton">@for (item of [1,2,3,4,5]; track item) { <div></div> }</div>
      } @else if (page()?.content?.length) {
        <div class="table-wrap"><table class="data-table">
          <thead><tr><th>DESCRIÇÃO</th><th>CATEGORIA</th><th>CONTA</th><th>DATA</th><th>TIPO</th><th class="align-right">VALOR</th><th></th></tr></thead>
          <tbody>@for (item of page()!.content; track item.id) {
            <tr>
              <td><div class="transaction-name"><span class="transaction-symbol" [class.symbol-income]="item.type === 'INCOME'">{{ item.type === 'INCOME' ? '↗' : '↘' }}</span><strong>{{ item.description }}</strong></div></td>
              <td><span class="table-secondary">{{ item.categoryName }}</span></td>
              <td><span class="table-secondary">{{ item.accountName }}</span></td>
              <td><span class="table-secondary">{{ formatDate(item.transactionDate) }}</span></td>
              <td><span class="type-pill" [class.type-income]="item.type === 'INCOME'">{{ item.type === 'INCOME' ? 'Receita' : 'Despesa' }}</span></td>
              <td class="align-right" [class.income-text]="item.type === 'INCOME'"><strong>{{ item.type === 'INCOME' ? '+' : '−' }} {{ item.amount | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong></td>
              <td class="actions-cell"><button type="button" class="icon-button" aria-label="Editar transação" (click)="edit(item)">Editar</button><button type="button" class="icon-button icon-danger" aria-label="Excluir transação" (click)="remove(item)">Excluir</button></td>
            </tr>
          }</tbody>
        </table></div>
        <div class="pagination">
          <span>Página {{ (page()?.page ?? 0) + 1 }} de {{ page()?.totalPages || 1 }}</span>
          <div><button class="button button-quiet" type="button" [disabled]="page()?.first ?? true" (click)="goToPage((page()?.page ?? 0) - 1)">← Anterior</button><button class="button button-quiet" type="button" [disabled]="page()?.last ?? true" (click)="goToPage((page()?.page ?? 0) + 1)">Próxima →</button></div>
        </div>
      } @else {
        <div class="empty-state"><span class="empty-mark">↔</span><h3>Nenhuma transação encontrada</h3><p>Cadastre uma receita ou despesa, ou ajuste os filtros para ampliar a busca.</p><button class="button button-primary" type="button" (click)="openCreate()">Nova transação</button></div>
      }
    </section>
  `,
})
export class TransactionsComponent implements OnInit {
  readonly accounts = signal<Account[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly page = signal<TransactionPage | null>(null);
  readonly loading = signal(true);
  readonly formOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly saving = signal(false);
  readonly notice = signal('');
  readonly noticeError = signal(false);
  readonly form = this.formBuilder.nonNullable.group({
    description: ['', [Validators.required, Validators.maxLength(180)]],
    amount: [0, [Validators.required, Validators.min(0.01)]],
    type: ['EXPENSE' as TransactionType, Validators.required],
    accountId: ['', Validators.required],
    categoryId: ['', Validators.required],
    transactionDate: [this.today(), Validators.required],
  });
  readonly filters = this.formBuilder.nonNullable.group({
    startDate: [''],
    endDate: [''],
    type: [''],
    accountId: [''],
    categoryId: [''],
  });

  constructor(private readonly formBuilder: FormBuilder, private readonly api: FinanceApiService) {}

  ngOnInit(): void {
    this.form.controls.type.valueChanges.subscribe(() => this.form.controls.categoryId.setValue(''));
    forkJoin({ accounts: this.api.accounts(), categories: this.api.categories() }).subscribe({
      next: (data) => {
        this.accounts.set(data.accounts);
        this.categories.set(data.categories);
        this.loadPage(0);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.showNotice(apiErrorMessage(error, 'Não foi possível carregar os dados do formulário.'), true);
      },
    });
  }

  filteredCategories(): Category[] {
    return this.categories().filter((category) => category.type === this.form.controls.type.value);
  }

  openCreate(): void {
    this.editingId.set(null);
    this.form.reset({ description: '', amount: 0, type: 'EXPENSE', accountId: '', categoryId: '', transactionDate: this.today() });
    this.formOpen.set(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  edit(transaction: Transaction): void {
    this.editingId.set(transaction.id);
    this.form.reset({
      description: transaction.description, amount: transaction.amount, type: transaction.type,
      accountId: transaction.accountId, categoryId: transaction.categoryId, transactionDate: transaction.transactionDate,
    });
    this.formOpen.set(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  closeForm(): void { this.formOpen.set(false); this.editingId.set(null); }

  save(): void {
    this.notice.set('');
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    this.api.saveTransaction(this.form.getRawValue(), this.editingId() ?? undefined).subscribe({
      next: () => {
        this.saving.set(false);
        this.closeForm();
        this.showNotice('Transação salva com sucesso.');
        this.loadPage(0);
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.showNotice(apiErrorMessage(error, 'Não foi possível salvar a transação.'), true);
      },
    });
  }

  applyFilters(): void { this.loadPage(0); }
  clearFilters(): void {
    this.filters.reset({ startDate: '', endDate: '', type: '', accountId: '', categoryId: '' });
    this.loadPage(0);
  }
  goToPage(page: number): void { this.loadPage(page); }

  remove(transaction: Transaction): void {
    if (!window.confirm('Excluir a transação “' + transaction.description + '”?')) return;
    this.api.deleteTransaction(transaction.id).subscribe({
      next: () => { this.showNotice('Transação excluída.'); this.loadPage(this.page()?.page ?? 0); },
      error: (error: unknown) => this.showNotice(apiErrorMessage(error, 'Não foi possível excluir a transação.'), true),
    });
  }

  formatDate(value: string): string { return value.split('-').reverse().join('/'); }

  private loadPage(pageNumber: number): void {
    this.loading.set(true);
    const raw = this.filters.getRawValue();
    this.api.transactions({
      startDate: raw.startDate || undefined,
      endDate: raw.endDate || undefined,
      type: (raw.type || undefined) as TransactionType | undefined,
      accountId: raw.accountId || undefined,
      categoryId: raw.categoryId || undefined,
      page: Math.max(0, pageNumber),
      size: 10,
    }).subscribe({
      next: (result) => { this.page.set(result); this.loading.set(false); },
      error: (error: unknown) => {
        this.loading.set(false);
        this.showNotice(apiErrorMessage(error, 'Não foi possível carregar as transações.'), true);
      },
    });
  }

  private today(): string {
    const now = new Date();
    return [now.getFullYear(), String(now.getMonth() + 1).padStart(2, '0'), String(now.getDate()).padStart(2, '0')].join('-');
  }
  private showNotice(message: string, isError = false): void {
    this.notice.set(message);
    this.noticeError.set(isError);
  }
}
