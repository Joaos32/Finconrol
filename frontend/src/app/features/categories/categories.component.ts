import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { FinanceApiService } from '../../core/api/finance-api.service';
import { apiErrorMessage } from '../../core/api/api-error';
import { Category, CategoryType } from '../../core/models/finance.models';

@Component({
  selector: 'fc-categories',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <div class="page-heading">
      <div><span class="eyebrow">PERSONALIZAÇÃO</span><h1>Categorias</h1><p>Organize suas movimentações do seu jeito.</p></div>
      @if (!formOpen()) { <button class="button button-primary" type="button" (click)="openCreate()">＋ Nova categoria</button> }
    </div>
    @if (notice()) { <div class="notice" [class.notice-error]="noticeError()" role="status">{{ notice() }}</div> }

    @if (formOpen()) {
      <section class="panel form-panel">
        <div class="panel-heading"><div><h2>{{ editingId() ? 'Editar categoria' : 'Nova categoria' }}</h2><p>Escolha se ela será usada em receitas ou despesas.</p></div></div>
        <form [formGroup]="form" (ngSubmit)="save()" class="form-grid form-grid-3 category-form">
          <mat-form-field appearance="outline"><mat-label>Nome</mat-label><input matInput formControlName="name" placeholder="Ex.: Viagem"><mat-error>Informe um nome.</mat-error></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Tipo</mat-label><mat-select formControlName="type"><mat-option value="EXPENSE">Despesa</mat-option><mat-option value="INCOME">Receita</mat-option></mat-select></mat-form-field>
          <div class="form-actions"><button class="button button-quiet" type="button" (click)="closeForm()">Cancelar</button><button class="button button-primary" type="submit" [disabled]="saving()">{{ saving() ? 'Salvando…' : 'Salvar categoria' }}</button></div>
        </form>
      </section>
    }

    @if (loading()) {
      <div class="category-groups">
        <section class="panel skeleton-panel"></section><section class="panel skeleton-panel"></section>
      </div>
    } @else {
      <div class="category-groups">
        <section class="panel category-list-panel">
          <div class="panel-heading"><div><span class="category-heading-icon income-heading-icon">↗</span><h2>Receitas</h2><p>Entradas organizadas por origem</p></div><span class="count-badge">{{ incomeCategories().length }}</span></div>
          @if (incomeCategories().length) {
            <div class="category-list">@for (category of incomeCategories(); track category.id) {
              <div class="category-list-item"><span class="category-bullet income-bullet"></span><strong>{{ category.name }}</strong><span class="category-item-actions"><button class="icon-button" type="button" (click)="edit(category)">Editar</button><button class="icon-button icon-danger" type="button" (click)="remove(category)">Excluir</button></span></div>
            }</div>
          } @else { <p class="category-empty">Nenhuma categoria de receita.</p> }
        </section>
        <section class="panel category-list-panel">
          <div class="panel-heading"><div><span class="category-heading-icon expense-heading-icon">↘</span><h2>Despesas</h2><p>Saídas organizadas por destino</p></div><span class="count-badge">{{ expenseCategories().length }}</span></div>
          @if (expenseCategories().length) {
            <div class="category-list">@for (category of expenseCategories(); track category.id) {
              <div class="category-list-item"><span class="category-bullet expense-bullet"></span><strong>{{ category.name }}</strong><span class="category-item-actions"><button class="icon-button" type="button" (click)="edit(category)">Editar</button><button class="icon-button icon-danger" type="button" (click)="remove(category)">Excluir</button></span></div>
            }</div>
          } @else { <p class="category-empty">Nenhuma categoria de despesa.</p> }
        </section>
      </div>
    }
  `,
})
export class CategoriesComponent implements OnInit {
  readonly categories = signal<Category[]>([]);
  readonly loading = signal(true);
  readonly formOpen = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly saving = signal(false);
  readonly notice = signal('');
  readonly noticeError = signal(false);
  readonly incomeCategories = () => this.categories().filter((category) => category.type === 'INCOME');
  readonly expenseCategories = () => this.categories().filter((category) => category.type === 'EXPENSE');
  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(80)]],
    type: ['EXPENSE' as CategoryType, Validators.required],
  });

  constructor(private readonly formBuilder: FormBuilder, private readonly api: FinanceApiService) {}

  ngOnInit(): void { this.load(); }
  openCreate(): void {
    this.editingId.set(null);
    this.form.reset({ name: '', type: 'EXPENSE' });
    this.formOpen.set(true);
  }
  edit(category: Category): void {
    this.editingId.set(category.id);
    this.form.reset({ name: category.name, type: category.type });
    this.formOpen.set(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
  closeForm(): void { this.formOpen.set(false); this.editingId.set(null); }
  save(): void {
    this.notice.set('');
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    this.api.saveCategory(this.form.getRawValue(), this.editingId() ?? undefined).subscribe({
      next: () => {
        this.saving.set(false);
        this.closeForm();
        this.showNotice('Categoria salva com sucesso.');
        this.load();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.showNotice(apiErrorMessage(error, 'Não foi possível salvar a categoria.'), true);
      },
    });
  }
  remove(category: Category): void {
    if (!window.confirm('Excluir a categoria “' + category.name + '”?')) return;
    this.api.deleteCategory(category.id).subscribe({
      next: () => { this.showNotice('Categoria excluída.'); this.load(); },
      error: (error: unknown) => this.showNotice(apiErrorMessage(error, 'Não foi possível excluir a categoria.'), true),
    });
  }
  private load(): void {
    this.loading.set(true);
    this.api.categories().subscribe({
      next: (data) => { this.categories.set(data); this.loading.set(false); },
      error: (error: unknown) => {
        this.loading.set(false);
        this.showNotice(apiErrorMessage(error, 'Não foi possível carregar as categorias.'), true);
      },
    });
  }
  private showNotice(message: string, isError = false): void {
    this.notice.set(message);
    this.noticeError.set(isError);
  }
}
