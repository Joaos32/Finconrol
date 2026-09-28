import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { FinanceApiService } from '../../core/api/finance-api.service';
import { apiErrorMessage } from '../../core/api/api-error';
import { Account, AccountType } from '../../core/models/finance.models';

@Component({
  selector: 'fc-accounts',
  standalone: true,
  imports: [CurrencyPipe, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <div class="page-heading">
      <div><span class="eyebrow">ORGANIZAÇÃO</span><h1>Contas</h1><p>Reúna suas contas e acompanhe cada saldo.</p></div>
      @if (!formOpen()) { <button class="button button-primary" type="button" (click)="openCreate()">＋ Nova conta</button> }
    </div>
    @if (notice()) { <div class="notice" [class.notice-error]="noticeError()" role="status">{{ notice() }}</div> }

    @if (formOpen()) {
      <section class="panel form-panel">
        <div class="panel-heading"><div><h2>{{ editingId() ? 'Editar conta' : 'Nova conta' }}</h2><p>Informe os dados da sua conta financeira.</p></div></div>
        <form [formGroup]="form" (ngSubmit)="save()" class="form-grid form-grid-3">
          <mat-form-field appearance="outline"><mat-label>Nome da conta</mat-label><input matInput formControlName="name" placeholder="Ex.: Nubank"><mat-error>Informe um nome.</mat-error></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Tipo</mat-label><mat-select formControlName="type">@for (type of accountTypes; track type.value) { <mat-option [value]="type.value">{{ type.label }}</mat-option> }</mat-select></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Saldo inicial</mat-label><span matTextPrefix>R$&nbsp;</span><input matInput type="number" min="0" step="0.01" formControlName="initialBalance"><mat-error>O saldo inicial não pode ser negativo.</mat-error></mat-form-field>
          <div class="form-actions"><button class="button button-quiet" type="button" (click)="closeForm()">Cancelar</button><button class="button button-primary" type="submit" [disabled]="saving()">{{ saving() ? 'Salvando…' : 'Salvar conta' }}</button></div>
        </form>
      </section>
    }

    <section class="panel table-panel">
      <div class="panel-heading">
        <div><h2>Contas cadastradas</h2><p>{{ accounts().length }} {{ accounts().length === 1 ? 'conta conectada' : 'contas conectadas' }}</p></div>
      </div>
      @if (loading()) {
        <div class="list-skeleton">@for (item of [1,2,3]; track item) { <div></div> }</div>
      } @else if (accounts().length) {
        <div class="table-wrap"><table class="data-table account-table">
          <thead><tr><th>CONTA</th><th>TIPO</th><th>SALDO INICIAL</th><th>SALDO ATUAL</th><th></th></tr></thead>
          <tbody>@for (account of accounts(); track account.id; let index = $index) {
            <tr>
              <td><div class="transaction-name"><span class="account-avatar" [class.account-color-0]="index % 4 === 0" [class.account-color-1]="index % 4 === 1" [class.account-color-2]="index % 4 === 2" [class.account-color-3]="index % 4 === 3">{{ account.name.slice(0,1).toUpperCase() }}</span><strong>{{ account.name }}</strong></div></td>
              <td><span class="type-pill">{{ accountType(account.type) }}</span></td>
              <td class="table-secondary">{{ account.initialBalance | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</td>
              <td><strong>{{ account.currentBalance | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong></td>
              <td class="actions-cell"><button type="button" class="icon-button" aria-label="Editar conta" (click)="edit(account)">Editar</button><button type="button" class="icon-button icon-danger" aria-label="Excluir conta" (click)="remove(account)">Excluir</button></td>
            </tr>
          }</tbody>
        </table></div>
      } @else {
        <div class="empty-state"><span class="empty-mark">◎</span><h3>Nenhuma conta por enquanto</h3><p>Adicione sua primeira conta e comece a acompanhar seus saldos.</p><button class="button button-primary" type="button" (click)="openCreate()">Adicionar primeira conta</button></div>
      }
    </section>
  `,
})
export class AccountsComponent implements OnInit {
  readonly accounts = signal<Account[]>([]);
  readonly loading = signal(true);
  readonly formOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly saving = signal(false);
  readonly notice = signal('');
  readonly noticeError = signal(false);
  readonly accountTypes = [
    { value: 'CHECKING' as const, label: 'Conta corrente' },
    { value: 'SAVINGS' as const, label: 'Poupança' },
    { value: 'CASH' as const, label: 'Carteira' },
    { value: 'INVESTMENT' as const, label: 'Investimentos' },
    { value: 'OTHER' as const, label: 'Outra conta' },
  ];
  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    type: ['CHECKING' as AccountType, Validators.required],
    initialBalance: [0, [Validators.required, Validators.min(0)]],
  });

  constructor(private readonly formBuilder: FormBuilder, private readonly api: FinanceApiService) {}

  ngOnInit(): void { this.load(); }

  openCreate(): void {
    this.editingId.set(null);
    this.form.reset({ name: '', type: 'CHECKING', initialBalance: 0 });
    this.formOpen.set(true);
  }

  edit(account: Account): void {
    this.editingId.set(account.id);
    this.form.reset({ name: account.name, type: account.type, initialBalance: account.initialBalance });
    this.formOpen.set(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  closeForm(): void { this.formOpen.set(false); this.editingId.set(null); }

  save(): void {
    this.notice.set('');
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    this.api.saveAccount(this.form.getRawValue(), this.editingId() ?? undefined).subscribe({
      next: () => {
        this.saving.set(false);
        this.closeForm();
        this.showNotice('Conta salva com sucesso.');
        this.load();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.showNotice(apiErrorMessage(error, 'Não foi possível salvar a conta.'), true);
      },
    });
  }

  remove(account: Account): void {
    if (!window.confirm('Excluir a conta ' + account.name + '?')) return;
    this.api.deleteAccount(account.id).subscribe({
      next: () => { this.showNotice('Conta excluída.'); this.load(); },
      error: (error: unknown) => this.showNotice(apiErrorMessage(error, 'Não foi possível excluir a conta.'), true),
    });
  }

  accountType(type: string): string {
    return this.accountTypes.find((option) => option.value === type)?.label ?? type;
  }

  private load(): void {
    this.loading.set(true);
    this.api.accounts().subscribe({
      next: (data) => { this.accounts.set(data); this.loading.set(false); },
      error: (error: unknown) => {
        this.loading.set(false);
        this.showNotice(apiErrorMessage(error, 'Não foi possível carregar as contas.'), true);
      },
    });
  }
  private showNotice(message: string, isError = false): void {
    this.notice.set(message);
    this.noticeError.set(isError);
  }
}
