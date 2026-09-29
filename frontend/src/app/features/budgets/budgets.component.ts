import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { forkJoin } from 'rxjs';
import { FinanceApiService } from '../../core/api/finance-api.service';
import { apiErrorMessage } from '../../core/api/api-error';
import { Budget, Category } from '../../core/models/finance.models';

function currentMonth(): string {
  const today = new Date();
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`;
}

@Component({
  selector: 'fc-budgets',
  standalone: true,
  imports: [CurrencyPipe, DecimalPipe, ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule,
    MatInputModule, MatSelectModule],
  template: `
    <div class="page-heading">
      <div><span class="eyebrow">PLANEJAMENTO</span><h1>Orçamentos</h1><p>Defina limites mensais para acompanhar seus gastos.</p></div>
      @if (!formOpen() && availableCategories().length) { <button class="button button-primary" type="button" (click)="openCreate()">＋ Definir orçamento</button> }
    </div>

    @if (error()) { <div class="notice notice-error" role="alert">{{ error() }}</div> }
    @if (notice()) { <div class="notice" [class.notice-error]="noticeError()" role="status">{{ notice() }}</div> }

    <section class="panel budget-month-panel">
      <mat-form-field appearance="outline">
        <mat-label>Mês exibido</mat-label>
        <input matInput type="month" [value]="month()" (change)="changeMonth($any($event.target).value)">
      </mat-form-field>
      <span>Os limites são independentes para cada mês.</span>
    </section>

    @if (formOpen()) {
      <section class="panel form-panel">
        <div class="panel-heading"><div><h2>{{ editingId() ? 'Editar orçamento' : 'Novo orçamento' }}</h2><p>Escolha uma categoria de despesa e defina o limite do mês.</p></div></div>
        <form [formGroup]="form" (ngSubmit)="save()" class="form-grid form-grid-3 budget-form">
          <mat-form-field appearance="outline">
            <mat-label>Categoria de despesa</mat-label>
            <mat-select formControlName="categoryId" [disabled]="checkingMonth() || !budgetOptionsReady()">
              @for (category of availableCategories(); track category.id) {
                <mat-option [value]="category.id">{{ category.name }}</mat-option>
              }
            </mat-select>
            @if (checkingMonth()) { <mat-hint>Verificando categorias deste mês…</mat-hint> }
            @else if (!budgetOptionsReady()) { <mat-hint>Não foi possível verificar este mês. Altere o mês para tentar novamente.</mat-hint> }
            @if (form.controls.categoryId.touched && form.controls.categoryId.hasError('required')) {
              <mat-error>Selecione uma categoria.</mat-error>
            }
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Mês do orçamento</mat-label>
            <input matInput type="month" formControlName="month" (change)="checkMonthOptions($any($event.target).value)">
            @if (form.controls.month.touched && form.controls.month.hasError('required')) {
              <mat-error>Selecione um mês.</mat-error>
            }
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Limite mensal</mat-label>
            <span matTextPrefix>R$&nbsp;</span>
            <input matInput type="number" min="0.01" step="0.01" formControlName="limitAmount">
            @if (form.controls.limitAmount.touched && form.controls.limitAmount.hasError('required')) {
              <mat-error>Informe o limite mensal.</mat-error>
            } @else if (form.controls.limitAmount.touched && form.controls.limitAmount.hasError('min')) {
              <mat-error>O limite deve ser maior que zero.</mat-error>
            } @else if (form.controls.limitAmount.touched && form.controls.limitAmount.hasError('pattern')) {
              <mat-error>Use no máximo duas casas decimais.</mat-error>
            }
          </mat-form-field>
          <div class="form-actions">
            <button class="button button-quiet" type="button" (click)="closeForm()">Cancelar</button>
            <button class="button button-primary" type="submit"
              [disabled]="saving() || checkingMonth() || !budgetOptionsReady()">
              {{ saving() ? 'Salvando…' : 'Salvar orçamento' }}
            </button>
          </div>
        </form>
      </section>
    }

    <section class="budget-list" aria-label="Orçamentos do mês">
      @if (loading()) {
        <div class="panel skeleton-panel"></div><div class="panel skeleton-panel"></div>
      } @else if (budgets().length) {
        @for (budget of budgets(); track budget.id) {
          <article class="panel budget-card" [class.budget-card-overrun]="budget.remainingAmount < 0">
            <div class="budget-card-heading">
              <div><span class="eyebrow">{{ monthLabel(budget.month) }}</span><h2>{{ budget.categoryName }}</h2></div>
              <div class="budget-actions">
                <button class="icon-button" type="button" [attr.aria-label]="'Editar orçamento de ' + budget.categoryName" (click)="edit(budget)">Editar</button>
                <button class="icon-button icon-danger" type="button" [attr.aria-label]="'Excluir orçamento de ' + budget.categoryName" (click)="remove(budget)">Excluir</button>
              </div>
            </div>
            <div class="budget-amounts">
              <div><span>Gasto no mês</span><strong>{{ budget.spentAmount | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong></div>
              <div><span>Limite</span><strong>{{ budget.limitAmount | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong></div>
              <div><span>{{ budget.remainingAmount < 0 ? 'Acima do limite' : 'Disponível' }}</span>
                <strong [class.negative-value]="budget.remainingAmount < 0">
                  {{ absolute(budget.remainingAmount) | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}
                </strong>
              </div>
            </div>
            <div class="budget-progress" role="progressbar" [attr.aria-label]="'Uso do orçamento de ' + budget.categoryName"
              aria-valuemin="0" aria-valuemax="100" [attr.aria-valuenow]="progressWidth(budget)">
              <span [class.progress-overrun]="budget.remainingAmount < 0" [style.width.%]="progressWidth(budget)"></span>
            </div>
            <div class="budget-progress-label">
              <span>{{ budget.percentageUsed | number:'1.1-1':'pt-BR' }}% utilizado</span>
              <span>{{ monthLabel(budget.month) }}</span>
            </div>
          </article>
        }
      } @else {
        <div class="panel budget-empty">
          <span class="empty-mark">◎</span><h2>Nenhum orçamento neste mês</h2>
          <p>Defina um limite por categoria para acompanhar seus gastos ao longo do mês.</p>
          @if (categories().length) {
            <button class="button button-primary" type="button" (click)="openCreate()">Definir primeiro orçamento</button>
          } @else {
            <a class="button button-primary" routerLink="/categories">Cadastrar categoria de despesa</a>
          }
        </div>
      }
    </section>
  `,
})
export class BudgetsComponent implements OnInit {
  readonly month = signal(currentMonth());
  readonly budgets = signal<Budget[]>([]);
  readonly budgetOptions = signal<Budget[]>([]);
  readonly budgetOptionsMonth = signal<string | null>(null);
  readonly budgetOptionsReady = signal(false);
  readonly categories = signal<Category[]>([]);
  readonly loading = signal(true);
  readonly formOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly saving = signal(false);
  readonly checkingMonth = signal(false);
  readonly error = signal('');
  readonly notice = signal('');
  readonly noticeError = signal(false);
  readonly form = this.formBuilder.nonNullable.group({
    categoryId: ['', Validators.required],
    month: [currentMonth(), [Validators.required, Validators.pattern(/^\d{4}-(0[1-9]|1[0-2])$/)]],
    limitAmount: ['', [Validators.required, Validators.min(0.01), Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
  });
  private budgetOptionsRequest = 0;

  constructor(private readonly formBuilder: FormBuilder, private readonly api: FinanceApiService) {}

  ngOnInit(): void { this.load(); }

  availableCategories(): Category[] {
    const selectedMonth = this.form.controls.month.value;
    const budgetsForMonth = this.budgetOptionsMonth() === selectedMonth ? this.budgetOptions() : [];
    return this.categories().filter((category) => !budgetsForMonth.some((budget) =>
      budget.categoryId === category.id && budget.id !== this.editingId()));
  }

  openCreate(): void {
    this.editingId.set(null);
    this.form.reset({ categoryId: '', month: this.month(), limitAmount: '' });
    this.setBudgetOptions(this.month(), this.budgets());
    this.formOpen.set(true);
  }

  edit(budget: Budget): void {
    this.editingId.set(budget.id);
    this.form.reset({ categoryId: budget.categoryId, month: budget.month, limitAmount: String(budget.limitAmount) });
    this.setBudgetOptions(budget.month, this.budgets());
    this.formOpen.set(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  closeForm(): void { this.formOpen.set(false); this.editingId.set(null); }

  checkMonthOptions(value: string): void {
    const requestId = ++this.budgetOptionsRequest;
    if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(value)) {
      this.budgetOptionsMonth.set(null);
      this.budgetOptionsReady.set(false);
      this.checkingMonth.set(false);
      return;
    }
    if (value === this.month()) {
      this.setBudgetOptions(value, this.budgets());
      return;
    }
    this.budgetOptionsMonth.set(null);
    this.budgetOptionsReady.set(false);
    this.checkingMonth.set(true);
    this.api.budgets(value).subscribe({
      next: (budgets) => {
        if (requestId !== this.budgetOptionsRequest) return;
        this.setBudgetOptions(value, budgets);
      },
      error: (error: unknown) => {
        if (requestId !== this.budgetOptionsRequest) return;
        this.budgetOptionsMonth.set(null);
        this.budgetOptionsReady.set(false);
        this.checkingMonth.set(false);
        this.showNotice(apiErrorMessage(error, 'Não foi possível verificar as categorias deste mês.'), true);
      },
    });
  }

  changeMonth(value: string): void {
    if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(value)) {
      this.error.set('Selecione um mês válido.');
      return;
    }
    this.month.set(value);
    this.error.set('');
    this.load();
  }

  save(): void {
    this.notice.set('');
    if (this.checkingMonth() || !this.budgetOptionsReady()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const values = this.form.getRawValue();
    const request = { ...values, limitAmount: Number(values.limitAmount) };
    this.api.saveBudget(request, this.editingId() ?? undefined).subscribe({
      next: () => {
        this.saving.set(false);
        this.month.set(request.month);
        this.closeForm();
        this.showNotice('Orçamento salvo com sucesso.');
        this.load();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.showNotice(apiErrorMessage(error, 'Não foi possível salvar o orçamento.'), true);
      },
    });
  }

  remove(budget: Budget): void {
    if (!window.confirm('Excluir o orçamento de ' + budget.categoryName + '?')) return;
    this.api.deleteBudget(budget.id).subscribe({
      next: () => { this.showNotice('Orçamento excluído.'); this.load(); },
      error: (error: unknown) => this.showNotice(apiErrorMessage(error, 'Não foi possível excluir o orçamento.'), true),
    });
  }

  progressWidth(budget: Budget): number { return Math.min(100, Math.max(0, budget.percentageUsed)); }
  absolute(value: number): number { return Math.abs(value); }
  monthLabel(value: string): string {
    const [year, month] = value.split('-').map(Number);
    return new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' })
      .format(new Date(year, month - 1, 1, 12));
  }

  private load(): void {
    this.loading.set(true);
    this.apiErrorClear();
    forkJoin({ budgets: this.api.budgets(this.month()), categories: this.api.categories('EXPENSE') }).subscribe({
      next: (data) => {
        this.budgets.set(data.budgets);
        this.categories.set(data.categories);
        if (this.form.controls.month.value === this.month()) {
          this.setBudgetOptions(this.month(), data.budgets);
        }
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.error.set(apiErrorMessage(error, 'Não foi possível carregar os orçamentos.'));
      },
    });
  }

  private apiErrorClear(): void { this.error.set(''); }
  private setBudgetOptions(month: string, budgets: Budget[]): void {
    this.budgetOptionsRequest++;
    this.budgetOptionsMonth.set(month);
    this.budgetOptions.set(budgets);
    this.budgetOptionsReady.set(true);
    this.checkingMonth.set(false);
    const selectedCategoryId = this.form.controls.categoryId.value;
    if (selectedCategoryId && budgets.some((budget) =>
      budget.categoryId === selectedCategoryId && budget.id !== this.editingId())) {
      this.form.controls.categoryId.setValue('');
    }
  }
  private showNotice(message: string, isError = false): void {
    this.notice.set(message);
    this.noticeError.set(isError);
  }
}
