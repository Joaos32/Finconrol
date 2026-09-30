import { CurrencyPipe } from '@angular/common';
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
import { Account, CreditCard, CreditCardInvoice } from '../../core/models/finance.models';

function currentMonth(): string {
  const today = new Date();
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`;
}

@Component({
  selector: 'fc-credit-cards',
  standalone: true,
  imports: [CurrencyPipe, ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <div class="page-heading">
      <div><span class="eyebrow">MEIOS DE PAGAMENTO</span><h1>Cartões de crédito</h1><p>Acompanhe limites, compras e faturas em um só lugar.</p></div>
      @if (!formOpen()) { <button class="button button-primary" type="button" (click)="openCreate()">＋ Novo cartão</button> }
    </div>
    @if (notice()) { <div class="notice" [class.notice-error]="noticeError()" role="status">{{ notice() }}</div> }

    @if (formOpen()) {
      <section class="panel form-panel">
        <div class="panel-heading"><div><h2>{{ editingId() ? 'Editar cartão' : 'Novo cartão' }}</h2><p>Cadastre apenas os dados necessários para organizar as faturas. O fechamento fica fixo após a primeira fatura; o vencimento pode mudar quando não há fatura em aberto.</p></div></div>
        <form [formGroup]="form" (ngSubmit)="save()" class="form-grid form-grid-3">
          <mat-form-field appearance="outline"><mat-label>Nome do cartão</mat-label><input matInput formControlName="name" placeholder="Ex.: Cartão principal"><mat-error>Informe um nome.</mat-error></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Limite de crédito</mat-label><span matTextPrefix>R$&nbsp;</span><input matInput type="number" min="0.01" step="0.01" formControlName="creditLimit"><mat-error>Informe um limite maior que zero.</mat-error></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Dia de fechamento</mat-label><input matInput type="number" min="1" max="28" step="1" formControlName="closingDay"><mat-hint>Escolha um dia entre 1 e 28.</mat-hint><mat-error>Use um dia entre 1 e 28.</mat-error></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Dia de vencimento</mat-label><input matInput type="number" min="1" max="28" step="1" formControlName="dueDay"><mat-hint>Escolha um dia entre 1 e 28.</mat-hint><mat-error>Use um dia entre 1 e 28.</mat-error></mat-form-field>
          <div class="form-actions"><button class="button button-quiet" type="button" (click)="closeForm()">Cancelar</button><button class="button button-primary" type="submit" [disabled]="saving()">{{ saving() ? 'Salvando…' : 'Salvar cartão' }}</button></div>
        </form>
      </section>
    }

    @if (loading()) {
      <section class="panel table-panel"><div class="list-skeleton">@for (item of [1,2,3]; track item) { <div></div> }</div></section>
    } @else if (cards().length) {
      <section class="panel table-panel">
        <div class="panel-heading"><div><h2>Cartões cadastrados</h2><p>{{ cards().length }} {{ cards().length === 1 ? 'cartão' : 'cartões' }} disponíveis</p></div></div>
        <div class="table-wrap"><table class="data-table card-table">
          <thead><tr><th>CARTÃO</th><th>FECHAMENTO / VENCIMENTO</th><th>EM ABERTO</th><th>DISPONÍVEL</th><th>LIMITE</th><th></th></tr></thead>
          <tbody>@for (card of cards(); track card.id) {
            <tr [class.selected-card-row]="selectedCardId() === card.id">
              <td><button class="card-select" type="button" (click)="selectCard(card)"><span class="card-chip">CC</span><strong>{{ card.name }}</strong></button></td>
              <td><span class="table-secondary">Fecha dia {{ card.closingDay }} · vence dia {{ card.dueDay }}</span></td>
              <td>{{ card.outstandingAmount | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</td>
              <td><strong>{{ card.availableLimit | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong></td>
              <td class="table-secondary">{{ card.creditLimit | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</td>
              <td class="actions-cell"><button type="button" class="icon-button" [attr.aria-label]="'Editar ' + card.name" (click)="edit(card)">Editar</button><button type="button" class="icon-button icon-danger" [attr.aria-label]="'Excluir ' + card.name" (click)="remove(card)">Excluir</button></td>
            </tr>
          }</tbody>
        </table></div>
      </section>

      @if (selectedCard(); as card) {
        <section class="panel invoice-panel">
          <div class="panel-heading invoice-heading">
            <div><span class="eyebrow">FATURA DO CARTÃO</span><h2>{{ card.name }}</h2><p>Confira o ciclo e registre a quitação integral usando uma conta.</p></div>
            <mat-form-field appearance="outline" class="invoice-month"><mat-label>Mês da fatura</mat-label><input matInput type="month" [value]="month()" (change)="changeMonth($any($event.target).value)"></mat-form-field>
          </div>
          @if (invoiceLoading()) {
            <div class="list-skeleton"><div></div><div></div></div>
          } @else if (invoice()) {
            <div class="invoice-summary">
              <div><span>Valor total</span><strong>{{ invoice()!.totalAmount | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong></div>
              <div><span>Período</span><strong>{{ formatDate(invoice()!.periodStart) }} a {{ formatDate(invoice()!.closingDate) }}</strong></div>
              <div><span>Vencimento</span><strong>{{ formatDate(invoice()!.dueDate) }}</strong></div>
              <div><span>Status</span><strong class="invoice-status" [class.invoice-paid]="invoice()!.paid">{{ invoice()!.paid ? 'Paga' : 'Em aberto' }}</strong></div>
            </div>
            @if (invoice()!.paid) {
              <div class="payment-confirmation">Fatura quitada{{ invoice()!.paymentAccountName ? ' pela conta ' + invoice()!.paymentAccountName : '' }}{{ invoice()!.paidAt ? ' em ' + formatDate(invoice()!.paidAt!.slice(0, 10)) : '' }}.</div>
            } @else if (invoice()!.totalAmount > 0) {
              @if (accounts().length) {
                <form [formGroup]="paymentForm" (ngSubmit)="payInvoice()" class="invoice-payment">
                  <mat-form-field appearance="outline"><mat-label>Conta para pagamento</mat-label><mat-select formControlName="accountId"><mat-option value="">Selecione uma conta</mat-option>@for (account of accounts(); track account.id) { <mat-option [value]="account.id">{{ account.name }}</mat-option> }</mat-select></mat-form-field>
                  <button class="button button-primary" type="submit" [disabled]="paying() || paymentForm.invalid">{{ paying() ? 'Registrando…' : 'Quitar fatura integral' }}</button>
                </form>
                <p class="invoice-payment-hint">O pagamento integral reduz o saldo da conta selecionada.</p>
              } @else {
                <div class="empty-inline">Cadastre uma conta antes de pagar uma fatura. <a routerLink="/accounts">Ir para contas</a></div>
              }
            } @else {
              <div class="empty-inline">Não há compras nesta fatura.</div>
            }
          } @else {
            <div class="empty-inline">{{ invoiceError() || 'Não foi possível carregar esta fatura.' }}</div>
          }
        </section>
      }
    } @else {
      <section class="panel card-empty">
        <span class="empty-mark">CC</span><h2>Nenhum cartão cadastrado</h2><p>Adicione um cartão para registrar compras e acompanhar as próximas faturas.</p>
        <button class="button button-primary" type="button" (click)="openCreate()">Cadastrar primeiro cartão</button>
      </section>
    }
  `,
  styles: [`
    .card-table { min-width: 760px; }
    .card-table tr.selected-card-row { background: #f4f9f6; }
    .card-select { display: inline-flex; align-items: center; gap: 9px; padding: 0; border: 0; color: inherit; background: none; cursor: pointer; }
    .card-chip { display: grid; width: 28px; height: 28px; place-items: center; border-radius: 8px; color: #397b68; background: #eaf5f0; font-size: 8px; font-weight: 700; }
    .card-select strong { color: #33414a; font-size: 10px; font-weight: 600; }
    .invoice-panel { margin-bottom: 14px; padding: 20px 21px; }
    .invoice-heading { align-items: center; }
    .invoice-heading h2 { margin: 4px 0; color: #27353d; font-size: 15px; }
    .invoice-month { max-width: 210px; margin-bottom: -1.25em; }
    .invoice-summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; padding: 17px; border: 1px solid #edf0f1; border-radius: 9px; background: #fafcfc; }
    .invoice-summary > div { display: grid; align-content: start; gap: 7px; }
    .invoice-summary span { color: #8d999e; font-size: 9px; }
    .invoice-summary strong { color: #3d4d55; font-size: 12px; font-weight: 650; }
    .invoice-summary > div:first-child strong { font-size: 17px; }
    .invoice-status { color: #a75a52 !important; }
    .invoice-status.invoice-paid { color: #23785f !important; }
    .invoice-payment { display: flex; align-items: center; gap: 12px; margin-top: 16px; }
    .invoice-payment .mat-mdc-form-field { max-width: 330px; margin-bottom: -1.25em; }
    .invoice-payment-hint { margin: 9px 0 0; color: #8d999e; font-size: 9px; }
    .payment-confirmation { margin-top: 15px; padding: 11px 13px; border-radius: 7px; color: #397b68; background: #edf7f1; font-size: 10px; }
    .card-empty { display: flex; min-height: 280px; flex-direction: column; align-items: center; justify-content: center; padding: 25px; text-align: center; }
    .card-empty .empty-mark { margin-bottom: 8px; }
    .card-empty h2 { margin: 5px 0; color: #4c5a62; font-size: 14px; }
    .card-empty p { max-width: 350px; margin: 0 0 16px; color: #8b979c; font-size: 10px; line-height: 1.6; }
    @media (max-width: 680px) {
      .invoice-panel { padding: 16px 14px; }
      .invoice-heading { align-items: stretch; flex-direction: column; }
      .invoice-month { max-width: none; margin-bottom: 0; }
      .invoice-summary { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 17px 10px; padding: 13px; }
      .invoice-payment { align-items: stretch; flex-direction: column; }
      .invoice-payment .mat-mdc-form-field { max-width: none; }
    }
  `],
})
export class CreditCardsComponent implements OnInit {
  readonly cards = signal<CreditCard[]>([]);
  readonly accounts = signal<Account[]>([]);
  readonly selectedCardId = signal<string | null>(null);
  readonly selectedCard = signal<CreditCard | null>(null);
  readonly invoice = signal<CreditCardInvoice | null>(null);
  readonly month = signal(currentMonth());
  readonly loading = signal(true);
  readonly invoiceLoading = signal(false);
  readonly invoiceError = signal('');
  readonly formOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly saving = signal(false);
  readonly paying = signal(false);
  readonly notice = signal('');
  readonly noticeError = signal(false);
  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    creditLimit: [0, [Validators.required, Validators.min(0.01), Validators.pattern(/^\d+(\.\d{1,2})?$/)]],
    closingDay: [1, [Validators.required, Validators.min(1), Validators.max(28), Validators.pattern(/^(?:[1-9]|1\d|2[0-8])$/)]],
    dueDay: [1, [Validators.required, Validators.min(1), Validators.max(28), Validators.pattern(/^(?:[1-9]|1\d|2[0-8])$/)]],
  });
  readonly paymentForm = this.formBuilder.nonNullable.group({ accountId: ['', Validators.required] });
  private invoiceRequest = 0;

  constructor(private readonly formBuilder: FormBuilder, private readonly api: FinanceApiService) {}

  ngOnInit(): void { this.load(); }

  openCreate(): void {
    this.editingId.set(null);
    this.form.reset({ name: '', creditLimit: 0, closingDay: 1, dueDay: 1 });
    this.formOpen.set(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  edit(card: CreditCard): void {
    this.editingId.set(card.id);
    this.form.reset({ name: card.name, creditLimit: card.creditLimit, closingDay: card.closingDay, dueDay: card.dueDay });
    this.formOpen.set(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  closeForm(): void { this.formOpen.set(false); this.editingId.set(null); }

  save(): void {
    this.notice.set('');
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const raw = this.form.getRawValue();
    this.api.saveCreditCard({ ...raw, creditLimit: Number(raw.creditLimit) }, this.editingId() ?? undefined).subscribe({
      next: () => {
        this.saving.set(false);
        this.closeForm();
        this.showNotice('Cartão salvo com sucesso.');
        this.load();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.showNotice(apiErrorMessage(error, 'Não foi possível salvar o cartão.'), true);
      },
    });
  }

  remove(card: CreditCard): void {
    if (!window.confirm('Excluir o cartão “' + card.name + '”?')) return;
    this.api.deleteCreditCard(card.id).subscribe({
      next: () => {
        this.showNotice('Cartão excluído.');
        if (this.selectedCardId() === card.id) {
          this.selectedCardId.set(null);
          this.selectedCard.set(null);
          this.invoice.set(null);
        }
        this.load();
      },
      error: (error: unknown) => this.showNotice(apiErrorMessage(error, 'Não foi possível excluir o cartão.'), true),
    });
  }

  selectCard(card: CreditCard): void {
    this.selectedCardId.set(card.id);
    this.selectedCard.set(card);
    this.loadInvoice();
  }

  changeMonth(value: string): void {
    if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(value)) return;
    this.month.set(value);
    this.loadInvoice();
  }

  payInvoice(): void {
    const invoice = this.invoice();
    const cardId = this.selectedCardId();
    if (!invoice || !invoice.id || !cardId || invoice.paid || invoice.totalAmount <= 0 || this.paymentForm.invalid) {
      this.paymentForm.markAllAsTouched();
      return;
    }
    this.paying.set(true);
    this.api.payCreditCardInvoice(cardId, invoice.id, this.paymentForm.controls.accountId.value).subscribe({
      next: (paidInvoice) => {
        this.paying.set(false);
        this.invoice.set(paidInvoice);
        this.paymentForm.reset({ accountId: '' });
        this.showNotice('Fatura quitada com sucesso.');
        this.loadCards();
      },
      error: (error: unknown) => {
        this.paying.set(false);
        this.showNotice(apiErrorMessage(error, 'Não foi possível quitar a fatura.'), true);
      },
    });
  }

  formatDate(value: string): string { return value.split('-').reverse().join('/'); }

  private load(): void {
    this.loading.set(true);
    forkJoin({ cards: this.api.creditCards(), accounts: this.api.accounts() }).subscribe({
      next: ({ cards, accounts }) => {
        this.cards.set(cards);
        this.accounts.set(accounts);
        this.loading.set(false);
        const selected = cards.find((card) => card.id === this.selectedCardId()) ?? cards[0] ?? null;
        this.selectedCardId.set(selected?.id ?? null);
        this.selectedCard.set(selected);
        if (selected) this.loadInvoice();
        else this.invoice.set(null);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.showNotice(apiErrorMessage(error, 'Não foi possível carregar cartões e contas.'), true);
      },
    });
  }

  private loadCards(): void {
    this.api.creditCards().subscribe({
      next: (cards) => {
        this.cards.set(cards);
        const selected = cards.find((card) => card.id === this.selectedCardId()) ?? null;
        this.selectedCard.set(selected);
      },
      error: (error: unknown) => this.showNotice(apiErrorMessage(error, 'Não foi possível atualizar os limites.'), true),
    });
  }

  private loadInvoice(): void {
    const cardId = this.selectedCardId();
    if (!cardId) return;
    const requestId = ++this.invoiceRequest;
    this.invoiceLoading.set(true);
    this.invoiceError.set('');
    this.invoice.set(null);
    this.paymentForm.reset({ accountId: '' });
    this.api.creditCardInvoice(cardId, this.month()).subscribe({
      next: (invoice) => {
        if (requestId !== this.invoiceRequest) return;
        this.invoice.set(invoice);
        this.invoiceLoading.set(false);
      },
      error: (error: unknown) => {
        if (requestId !== this.invoiceRequest) return;
        this.invoiceLoading.set(false);
        this.invoiceError.set(apiErrorMessage(error, 'Não foi possível carregar a fatura deste mês.'));
      },
    });
  }

  private showNotice(message: string, isError = false): void {
    this.notice.set(message);
    this.noticeError.set(isError);
  }
}
