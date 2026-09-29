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
  await expect(page.getByRole('row', { name: /Conta E2E/ })).toContainText('100,00');

  await page.getByRole('link', { name: /Categorias/ }).click();
  await createCategory(page, 'Renda E2E', 'Receita');
  await createCategory(page, 'Mercado E2E', 'Despesa');

  await page.getByRole('link', { name: /Transações/ }).click();
  await createTransaction(page, {
    description: 'Salário E2E', amount: '200', type: 'Receita', account: 'Conta E2E', category: 'Renda E2E',
  });
  await createTransaction(page, {
    description: 'Compra E2E', amount: '35.50', type: 'Despesa', account: 'Conta E2E', category: 'Mercado E2E',
  });

  await page.getByRole('link', { name: 'Dashboard' }).click();
  await expect(page.getByRole('heading', { name: 'Seu dinheiro, com clareza.' })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Resumo financeiro' })).toContainText('264,50');
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
  await expect(page.getByRole('row', { name: /Compra E2E/ })).toBeVisible();
  await expect(page.getByRole('row', { name: /Salário E2E/ })).toHaveCount(0);
});

async function createCategory(page: Page, name: string, type: 'Receita' | 'Despesa'): Promise<void> {
  await page.getByRole('button', { name: /Nova categoria/ }).click();
  await page.getByLabel('Nome').fill(name);
  await page.getByLabel('Tipo').click();
  await page.getByRole('option', { name: type }).click();
  await page.getByRole('button', { name: 'Salvar categoria' }).click();
  await expect(page.getByText(name, { exact: true })).toBeVisible();
}

async function createTransaction(page: Page, data: {
  description: string;
  amount: string;
  type: 'Receita' | 'Despesa';
  account: string;
  category: string;
}): Promise<void> {
  await page.getByRole('button', { name: /Nova transação/ }).click();
  await page.getByLabel('Descrição').fill(data.description);
  if (data.type === 'Receita') {
    await page.getByLabel('Tipo').first().click();
    await page.getByRole('option', { name: 'Receita' }).click();
  }
  await page.getByLabel('Valor').fill(data.amount);
  await page.getByLabel('Conta').first().click();
  await page.getByRole('option', { name: data.account }).click();
  await page.getByLabel('Categoria').first().click();
  await page.getByRole('option', { name: data.category }).click();
  await page.getByRole('button', { name: 'Salvar transação' }).click();
  await expect(page.getByRole('row', { name: new RegExp(data.description) })).toBeVisible();
}
