import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { FinanceApiService } from '../../core/api/finance-api.service';
import { apiErrorMessage } from '../../core/api/api-error';
import {
  Account, DashboardSummary, ExpenseCategory, MonthlyPoint, Transaction,
} from '../../core/models/finance.models';

@Component({
  selector: 'fc-dashboard',
  standalone: true,
  imports: [CurrencyPipe, DecimalPipe, RouterLink],
  template: `
    <div class="page-heading dashboard-heading">
      <div>
        <span class="eyebrow">VISÃO GERAL</span>
        <h1>Seu dinheiro, com clareza.</h1>
        <p>Acompanhe sua vida financeira em um só lugar.</p>
      </div>
      <a class="button button-primary" routerLink="/transactions">＋ Nova transação</a>
    </div>

    @if (error()) {
      <div class="notice notice-error" role="alert">{{ error() }}</div>
    }
    @if (loading()) {
      <div class="summary-grid">
        @for (item of [1, 2, 3, 4]; track item) { <div class="summary-card skeleton-card"></div> }
      </div>
      <div class="dashboard-grid">
        <div class="panel skeleton-panel"></div>
        <div class="panel skeleton-panel"></div>
      </div>
    } @else if (summary()) {
      <section class="summary-grid" aria-label="Resumo financeiro">
        <article class="summary-card balance-card">
          <div class="summary-label"><span class="summary-icon icon-balance">R$</span> Saldo atual</div>
          <div class="summary-value">{{ summary()!.currentBalance | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</div>
          <div class="summary-foot">Somado em todas as contas</div>
          <span class="balance-glow"></span>
        </article>
        <article class="summary-card">
          <div class="summary-label"><span class="summary-icon icon-income">↗</span> Receitas do mês</div>
          <div class="summary-value">{{ summary()!.monthlyIncome | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</div>
          <div class="summary-foot"><span class="metric-dot dot-income"></span> Entradas neste mês</div>
        </article>
        <article class="summary-card">
          <div class="summary-label"><span class="summary-icon icon-expense">↘</span> Despesas do mês</div>
          <div class="summary-value">{{ summary()!.monthlyExpense | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</div>
          <div class="summary-foot"><span class="metric-dot dot-expense"></span> Saídas neste mês</div>
        </article>
        <article class="summary-card">
          <div class="summary-label"><span class="summary-icon icon-result">∑</span> Resultado do mês</div>
          <div class="summary-value" [class.positive-value]="summary()!.monthlyResult >= 0"
            [class.negative-value]="summary()!.monthlyResult < 0">
            {{ summary()!.monthlyResult | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}
          </div>
          <div class="summary-foot">Receitas menos despesas</div>
        </article>
      </section>

      <section class="dashboard-grid">
        <article class="panel evolution-panel">
          <div class="panel-heading">
            <div><h2>Movimentação mensal</h2><p>Receitas e despesas nos últimos 6 meses</p></div>
            <div class="chart-legend"><span><i class="legend-income"></i> Receitas</span><span><i class="legend-expense"></i> Despesas</span></div>
          </div>
          @if (monthly().length) {
            <div class="bar-chart" role="img" aria-label="Comparativo mensal de receitas e despesas">
              @for (point of monthly(); track point.month) {
                <div class="bar-month">
                  <div class="bar-pair">
                    <div class="bar-income" [style.height.%]="barHeight(point.income)" [title]="'Receita: ' + (point.income | currency:'BRL')"></div>
                    <div class="bar-expense" [style.height.%]="barHeight(point.expense)" [title]="'Despesa: ' + (point.expense | currency:'BRL')"></div>
                  </div>
                  <span>{{ monthLabel(point.month) }}</span>
                </div>
              }
            </div>
          } @else {
            <div class="empty-chart"><span class="empty-mark">↗</span><strong>Seu histórico começa aqui</strong><span>Registre sua primeira movimentação para ver a evolução.</span></div>
          }
        </article>

        <article class="panel category-panel">
          <div class="panel-heading">
            <div><h2>Despesas por categoria</h2><p>Distribuição deste mês</p></div>
          </div>
          @if (expenses().length) {
            <div class="category-chart">
              @for (item of expenses(); track item.category; let index = $index) {
                <div class="category-chart-row">
                  <div class="category-row-top"><span class="category-swatch" [class.swatch-0]="index % 5 === 0" [class.swatch-1]="index % 5 === 1" [class.swatch-2]="index % 5 === 2" [class.swatch-3]="index % 5 === 3" [class.swatch-4]="index % 5 === 4"></span><strong>{{ item.category }}</strong><span>{{ item.amount | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span></div>
                  <div class="category-track"><span class="category-fill" [class.fill-0]="index % 5 === 0" [class.fill-1]="index % 5 === 1" [class.fill-2]="index % 5 === 2" [class.fill-3]="index % 5 === 3" [class.fill-4]="index % 5 === 4" [style.width.%]="item.percentage"></span></div>
                  <small>{{ item.percentage | number:'1.1-1':'pt-BR' }}% do total</small>
                </div>
              }
            </div>
          } @else {
            <div class="empty-chart"><span class="empty-mark">◌</span><strong>Nenhuma despesa neste mês</strong><span>Suas categorias aparecerão aqui.</span></div>
          }
        </article>
      </section>

      <section class="bottom-grid">
        <article class="panel recent-panel">
          <div class="panel-heading">
            <div><h2>Últimas transações</h2><p>Suas movimentações mais recentes</p></div>
            <a routerLink="/transactions" class="text-link">Ver histórico <span>→</span></a>
          </div>
          @if (recent().length) {
            <div class="table-wrap">
              <table class="data-table">
                <thead><tr><th>DESCRIÇÃO</th><th>CATEGORIA</th><th>DATA</th><th class="align-right">VALOR</th></tr></thead>
                <tbody>
                  @for (item of recent(); track item.id) {
                    <tr>
                      <td><div class="transaction-name"><span class="transaction-symbol" [class.symbol-income]="item.type === 'INCOME'">{{ item.type === 'INCOME' ? '↗' : '↘' }}</span><strong>{{ item.description }}</strong></div></td>
                      <td><span class="table-secondary">{{ item.categoryName }}</span></td>
                      <td><span class="table-secondary">{{ formatDate(item.transactionDate) }}</span></td>
                      <td class="align-right" [class.income-text]="item.type === 'INCOME'"><strong>{{ item.type === 'INCOME' ? '+' : '−' }} {{ item.amount | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong></td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          } @else {
            <div class="empty-inline"><span>Quando você registrar entradas e saídas, elas aparecerão aqui.</span><a routerLink="/transactions">Registrar transação →</a></div>
          }
        </article>

        <article class="panel accounts-panel">
          <div class="panel-heading">
            <div><h2>Suas contas</h2><p>Saldo por instituição</p></div>
            <a routerLink="/accounts" class="text-link">Gerenciar <span>→</span></a>
          </div>
          @if (accounts().length) {
            <div class="account-list">
              @for (account of accounts(); track account.id) {
                <div class="account-list-item">
                  <span class="account-avatar" [class.account-color-0]="$index % 4 === 0" [class.account-color-1]="$index % 4 === 1" [class.account-color-2]="$index % 4 === 2" [class.account-color-3]="$index % 4 === 3">{{ account.name.slice(0, 1).toUpperCase() }}</span>
                  <span class="account-list-copy"><strong>{{ account.name }}</strong><small>{{ accountType(account.type) }}</small></span>
                  <strong class="account-list-balance">{{ account.currentBalance | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong>
                </div>
              }
            </div>
          } @else {
            <div class="empty-inline"><span>Adicione uma conta para acompanhar seu saldo.</span><a routerLink="/accounts">Adicionar conta →</a></div>
          }
        </article>
      </section>
    }
  `,
})
export class DashboardComponent implements OnInit {
  readonly summary = signal<DashboardSummary | null>(null);
  readonly expenses = signal<ExpenseCategory[]>([]);
  readonly monthly = signal<MonthlyPoint[]>([]);
  readonly recent = signal<Transaction[]>([]);
  readonly accounts = signal<Account[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');

  constructor(private readonly api: FinanceApiService) {}

  ngOnInit(): void {
    forkJoin({
      summary: this.api.dashboardSummary(),
      expenses: this.api.expenseCategories(),
      monthly: this.api.monthlyEvolution(),
      recent: this.api.recentTransactions(),
      accounts: this.api.accounts(),
    }).subscribe({
      next: (data) => {
        this.summary.set(data.summary);
        this.expenses.set(data.expenses);
        this.monthly.set(data.monthly);
        this.recent.set(data.recent);
        this.accounts.set(data.accounts);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(apiErrorMessage(error, 'Não foi possível carregar seu dashboard.'));
        this.loading.set(false);
      },
    });
  }

  barHeight(value: number): number {
    const max = Math.max(1, ...this.monthly().flatMap((item) => [item.income, item.expense]));
    return Math.max(value > 0 ? 4 : 0, (value / max) * 100);
  }
  monthLabel(value: string): string {
    const month = Number(value.slice(5));
    return ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez'][month - 1] ?? value;
  }
  formatDate(value: string): string {
    return value.split('-').reverse().join('/');
  }
  accountType(type: string): string {
    const names: Record<string, string> = {
      CHECKING: 'Conta corrente', SAVINGS: 'Poupança', CASH: 'Carteira',
      INVESTMENT: 'Investimentos', OTHER: 'Outra conta',
    };
    return names[type] ?? type;
  }
}
