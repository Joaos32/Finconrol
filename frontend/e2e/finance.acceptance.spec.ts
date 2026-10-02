import { expect, test, type Page } from '@playwright/test';

test('registers, logs in and completes the finance flow through Angular', async ({ page }) => {
  test.setTimeout(60_000);
  const email = `e2e-${Date.now()}@example.com`;
  const password = 'SenhaSegura123!';
  page.on('pageerror', (error) => console.log(`[browser page error] ${error.message}`));
  page.on('console', (message) => {
    if (message.type() === 'error') console.log(`[browser console error] ${message.text()}`);
  });

  await page.goto('/login');
  try {
    await expect(page.getByRole('heading', { name: 'Entre na sua conta' })).toBeVisible();
  } catch (error) {
    console.log(`[login route diagnostic] URL: ${page.url()}`);
    console.log(`[login route diagnostic] body: ${await page.locator('body').innerText()}`);
    throw error;
  }
  await page.getByRole('link', { name: 'Criar minha conta' }).click();
  await page.getByLabel('Nome').fill('Pessoa de Teste');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha', { exact: true }).fill(password);
  await page.getByLabel('Confirme sua senha').fill(password);
  await page.getByRole('button', { name: 'Criar minha conta' }).click();

  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByRole('heading', { name: 'Seu dinheiro, com clareza.' })).toBeVisible();

  await page.getByRole('link', { name: /Contas/ }).click();
  await expect(page.getByRole('heading', { name: 'Contas' })).toBeVisible();
  await page.getByRole('button', { name: /Nova conta/ }).click();
  await page.getByLabel('Nome da conta').fill('Conta E2E');
  await page.getByLabel('Saldo inicial').fill('100');
  await page.getByRole('button', { name: 'Salvar conta' }).click();
  const accountRow = page.getByRole('row', { name: /Conta E2E/ });
  await expect(accountRow).toContainText('100,00');
  await accountRow.getByRole('button', { name: 'Editar conta' }).click();
  await page.getByLabel('Nome da conta').fill('Conta E2E Atualizada');
  await page.getByRole('button', { name: 'Salvar conta' }).click();
  await expect(page.getByRole('row', { name: /Conta E2E Atualizada/ })).toContainText('100,00');

  await page.getByRole('link', { name: /Categorias/ }).click();
  await createCategory(page, 'Renda E2E', 'Receita');
  await createCategory(page, 'Mercado E2E', 'Despesa');
  const incomeCategory = page.locator('.category-list-item').filter({ hasText: 'Renda E2E' });
  await incomeCategory.getByRole('button', { name: 'Editar categoria Renda E2E' }).click();
  await page.getByLabel('Nome').fill('Renda E2E Atualizada');
  await page.getByRole('button', { name: 'Salvar categoria' }).click();
  await expect(page.getByText('Renda E2E Atualizada', { exact: true })).toBeVisible();

  await page.getByRole('link', { name: /Transações/ }).click();
  await createTransaction(page, {
    description: 'Salário E2E', amount: '200', type: 'Receita', account: 'Conta E2E Atualizada', category: 'Renda E2E Atualizada',
  });
  await createTransaction(page, {
    description: 'Compra E2E', amount: '35.50', type: 'Despesa', account: 'Conta E2E Atualizada', category: 'Mercado E2E',
  });

  await page.getByRole('link', { name: /Orçamentos/ }).click();
  await expect(page.getByRole('heading', { name: 'Orçamentos' })).toBeVisible();
  await createBudget(page, 'Mercado E2E', currentMonth(), '40');
  const budgetCard = page.locator('.budget-card').filter({ hasText: 'Mercado E2E' });
  await expect(budgetCard).toContainText('35,50');
  await expect(budgetCard).toContainText('4,50');
  await expect(budgetCard).toContainText('88,8%');

  await page.getByRole('link', { name: /Contas/ }).click();
  await page.getByRole('button', { name: /Nova conta/ }).click();
  await page.getByLabel('Nome da conta').fill('Conta fatura E2E');
  await page.getByLabel('Saldo inicial').fill('50');
  await page.getByRole('button', { name: 'Salvar conta' }).click();
  await expect(page.getByRole('row', { name: /Conta fatura E2E/ })).toContainText('50,00');

  await page.getByRole('link', { name: /Categorias/ }).click();
  await createCategory(page, 'Crédito E2E', 'Despesa');
  await page.getByRole('link', { name: /Cartões/ }).click();
  await page.getByRole('button', { name: /Novo cartão/ }).click();
  await page.getByLabel('Nome do cartão').fill('Cartão E2E');
  await page.getByLabel('Limite de crédito').fill('500');
  await page.getByLabel('Dia de fechamento').fill('28');
  await page.getByLabel('Dia de vencimento').fill('5');
  await page.getByRole('button', { name: 'Salvar cartão' }).click();
  await expect(page.getByRole('row', { name: /Cartão E2E/ })).toBeVisible();

  await page.getByRole('link', { name: /Transações/ }).click();
  await createTransaction(page, {
    description: 'Compra no cartão E2E', amount: '12.25', type: 'Despesa', card: 'Cartão E2E', category: 'Crédito E2E',
  });
  await page.getByRole('link', { name: /Orçamentos/ }).click();
  await createBudget(page, 'Crédito E2E', currentMonth(), '20');
  const cardBudget = page.locator('.budget-card').filter({ hasText: 'Crédito E2E' });
  await expect(cardBudget).toContainText('12,25');

  await page.getByRole('link', { name: /Cartões/ }).click();
  await page.getByLabel('Mês da fatura').fill(invoiceMonthForPurchase(currentMonth(), new Date().getDate(), 28));
  await expect(page.getByText('12,25')).toBeVisible();
  await page.getByLabel('Conta para pagamento').click();
  await page.getByRole('option', { name: 'Conta fatura E2E' }).click();
  await page.getByRole('button', { name: 'Quitar fatura integral' }).click();
  await expect(page.getByText('Fatura quitada com sucesso.')).toBeVisible();
  await expect(page.getByText('Paga', { exact: true })).toBeVisible();
  await page.getByRole('link', { name: /Contas/ }).click();
  await expect(page.getByRole('row', { name: /Conta fatura E2E/ })).toContainText('37,75');

  await page.getByRole('link', { name: 'Dashboard' }).click();
  await expect(page.getByRole('heading', { name: 'Seu dinheiro, com clareza.' })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Resumo financeiro' })).toContainText('302,25');
  await expect(page.getByRole('row', { name: /Compra E2E/ })).toBeVisible();

  await page.getByRole('button', { name: /Sair da aplicação/ }).click();
  await expect(page.getByRole('heading', { name: 'Entre na sua conta' })).toBeVisible();
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha').fill(password);
  await page.getByRole('button', { name: 'Entrar na minha conta' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);

  await page.getByRole('link', { name: /Transações/ }).click();
  await page.getByLabel('Tipo').click();
  await page.getByRole('option', { name: 'Despesas' }).click();
  await page.getByRole('button', { name: 'Aplicar filtros' }).click();
  const expenseRow = page.getByRole('row', { name: /Compra E2E/ });
  await expect(expenseRow).toBeVisible();
  await expect(page.getByRole('row', { name: /Salário E2E/ })).toHaveCount(0);
  await expenseRow.getByRole('button', { name: 'Editar transação' }).click();
  await page.getByLabel('Valor').fill('36.00');
  await page.getByRole('button', { name: 'Salvar transação' }).click();
  const updatedExpenseRow = page.getByRole('row', { name: /Compra E2E/ });
  await expect(updatedExpenseRow).toContainText('36,00');

  await page.getByRole('link', { name: /Orçamentos/ }).click();
  await expect(budgetCard).toContainText('36,00');
  await expect(budgetCard).toContainText('4,00');
  const previousMonth = previousCalendarMonth(currentMonth());
  await page.getByLabel('Mês exibido').fill(previousMonth);
  await expect(page.getByRole('heading', { name: 'Nenhum orçamento neste mês' })).toBeVisible();
  await page.getByLabel('Mês exibido').fill(currentMonth());
  await expect(budgetCard).toBeVisible();
  await budgetCard.getByRole('button', { name: 'Editar orçamento de Mercado E2E' }).click();
  await page.getByLabel('Limite mensal').fill('30');
  await page.getByRole('button', { name: 'Salvar orçamento' }).click();
  await expect(budgetCard).toContainText('6,00');
  await expect(budgetCard).toContainText('120,0%');
  page.once('dialog', (dialog) => dialog.accept());
  await budgetCard.getByRole('button', { name: 'Excluir orçamento de Mercado E2E' }).click();
  await expect(budgetCard).toHaveCount(0);

  await page.getByRole('link', { name: /Transações/ }).click();
  await page.getByRole('button', { name: 'Limpar' }).click();
  page.once('dialog', (dialog) => dialog.accept());
  await updatedExpenseRow.getByRole('button', { name: 'Excluir transação' }).click();
  await expect(updatedExpenseRow).toHaveCount(0);
  const incomeRow = page.getByRole('row', { name: /Salário E2E/ });
  page.once('dialog', (dialog) => dialog.accept());
  await incomeRow.getByRole('button', { name: 'Excluir transação' }).click();
  await expect(incomeRow).toHaveCount(0);

  await page.getByRole('link', { name: /Categorias/ }).click();
  const expenseCategory = page.locator('.category-list-item').filter({ hasText: 'Mercado E2E' });
  page.once('dialog', (dialog) => dialog.accept());
  await expenseCategory.getByRole('button', { name: 'Excluir categoria Mercado E2E' }).click();
  await expect(expenseCategory).toHaveCount(0);
  const updatedIncomeCategory = page.locator('.category-list-item').filter({ hasText: 'Renda E2E Atualizada' });
  page.once('dialog', (dialog) => dialog.accept());
  await updatedIncomeCategory.getByRole('button', { name: 'Excluir categoria Renda E2E Atualizada' }).click();
  await expect(updatedIncomeCategory).toHaveCount(0);

  await page.getByRole('link', { name: /Contas/ }).click();
  const updatedAccountRow = page.getByRole('row', { name: /Conta E2E Atualizada/ });
  page.once('dialog', (dialog) => dialog.accept());
  await updatedAccountRow.getByRole('button', { name: 'Excluir conta' }).click();
  await expect(updatedAccountRow).toHaveCount(0);
});

test('shows each merchant installment on its invoice and monthly budget', async ({ page }) => {
  test.setTimeout(60_000);
  const email = `installments-e2e-${Date.now()}@example.com`;
  const password = 'SenhaSegura123!';
  await page.goto('/login');
  await page.getByRole('link', { name: 'Criar minha conta' }).click();
  await page.getByLabel('Nome').fill('Pessoa Parcelamento');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha', { exact: true }).fill(password);
  await page.getByLabel('Confirme sua senha').fill(password);
  await page.getByRole('button', { name: 'Criar minha conta' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);

  await page.getByRole('link', { name: /Categorias/ }).click();
  await createCategory(page, 'Compra parcelada E2E', 'Despesa');
  await page.getByRole('link', { name: /Cartões/ }).click();
  await page.getByRole('button', { name: /Novo cartão/ }).click();
  await page.getByLabel('Nome do cartão').fill('Cartão parcelado E2E');
  await page.getByLabel('Limite de crédito').fill('500');
  await page.getByLabel('Dia de fechamento').fill('28');
  await page.getByLabel('Dia de vencimento').fill('5');
  await page.getByRole('button', { name: 'Salvar cartão' }).click();

  await page.getByRole('link', { name: /Transações/ }).click();
  await createTransaction(page, {
    description: 'Notebook parcelado E2E', amount: '100', type: 'Despesa',
    card: 'Cartão parcelado E2E', category: 'Compra parcelada E2E', installmentCount: 4,
  });
  const transactionRow = page.getByRole('row', { name: /Notebook parcelado E2E/ });
  await expect(transactionRow).toContainText('100,00');
  await expect(transactionRow).toContainText('4 parcelas');

  const invoiceMonth = invoiceMonthForPurchase(currentMonth(), new Date().getDate(), 28);
  await page.getByRole('link', { name: /Orçamentos/ }).click();
  await createBudget(page, 'Compra parcelada E2E', invoiceMonth, '30');
  const budgetCard = page.locator('.budget-card').filter({ hasText: 'Compra parcelada E2E' });
  await expect(budgetCard).toContainText('25,00');
  await expect(budgetCard).toContainText('5,00');

  await page.getByRole('link', { name: /Cartões/ }).click();
  await page.getByLabel('Mês da fatura').fill(invoiceMonth);
  const invoiceItems = page.locator('.invoice-items');
  await expect(invoiceItems).toContainText('Notebook parcelado E2E');
  await expect(invoiceItems).toContainText('Parcela 1 de 4');
  await expect(invoiceItems).toContainText('25,00');
});

async function createCategory(page: Page, name: string, type: 'Receita' | 'Despesa'): Promise<void> {
  await page.getByRole('button', { name: /Nova categoria/ }).click();
  await page.getByLabel('Nome').fill(name);
  await page.getByLabel('Tipo').click();
  await page.getByRole('option', { name: type }).click();
  await page.getByRole('button', { name: 'Salvar categoria' }).click();
  await expect(page.getByText(name, { exact: true })).toBeVisible();
}

async function createBudget(page: Page, category: string, month: string, limit: string): Promise<void> {
  await page.getByRole('button', { name: /Definir orçamento/ }).click();
  await page.getByLabel('Categoria de despesa').click();
  await page.getByRole('option', { name: category }).click();
  await page.getByLabel('Mês do orçamento').fill(month);
  await page.getByLabel('Limite mensal').fill(limit);
  await page.getByRole('button', { name: 'Salvar orçamento' }).click();
  await expect(page.locator('.budget-card').filter({ hasText: category })).toBeVisible();
}

function currentMonth(): string {
  const today = new Date();
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`;
}

function previousCalendarMonth(month: string): string {
  const [year, monthNumber] = month.split('-').map(Number);
  const previous = new Date(year, monthNumber - 2, 1);
  return `${previous.getFullYear()}-${String(previous.getMonth() + 1).padStart(2, '0')}`;
}

async function createTransaction(page: Page, data: {
  description: string;
  amount: string;
  type: 'Receita' | 'Despesa';
  account?: string;
  card?: string;
  category: string;
  installmentCount?: number;
}): Promise<void> {
  await page.getByRole('button', { name: /Nova transação/ }).click();
  await page.getByLabel('Descrição').fill(data.description);
  if (data.type === 'Receita') {
    await page.getByLabel('Tipo').first().click();
    await page.getByRole('option', { name: 'Receita' }).click();
  }
  await page.getByLabel('Valor').fill(data.amount);
  if (data.card) {
    await page.getByLabel('Origem da despesa').click();
    await page.getByRole('option', { name: 'Cartão de crédito' }).click();
    await page.getByLabel('Cartão de crédito').click();
    await page.getByRole('option', { name: data.card }).click();
    if (data.installmentCount) {
      await page.getByLabel('Quantidade de parcelas').fill(String(data.installmentCount));
    }
  } else {
    await page.getByLabel('Conta').first().click();
    await page.getByRole('option', { name: data.account! }).click();
  }
  await page.getByLabel('Categoria').first().click();
  await page.getByRole('option', { name: data.category }).click();
  await page.getByRole('button', { name: 'Salvar transação' }).click();
  await expect(page.getByRole('row', { name: new RegExp(data.description) })).toBeVisible();
}

function invoiceMonthForPurchase(month: string, day: number, closingDay: number): string {
  if (day <= closingDay) return month;
  const [year, monthNumber] = month.split('-').map(Number);
  const next = new Date(year, monthNumber, 1);
  return `${next.getFullYear()}-${String(next.getMonth() + 1).padStart(2, '0')}`;
}
