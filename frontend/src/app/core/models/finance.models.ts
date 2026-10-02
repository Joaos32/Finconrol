export type AccountType = 'CHECKING' | 'SAVINGS' | 'CASH' | 'INVESTMENT' | 'OTHER';
export type CategoryType = 'INCOME' | 'EXPENSE';
export type TransactionType = CategoryType;

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
}

export interface Account {
  id: string;
  name: string;
  type: AccountType;
  initialBalance: number;
  currentBalance: number;
  createdAt: string;
  updatedAt: string;
}

export interface AccountRequest {
  name: string;
  type: AccountType;
  initialBalance: number;
}

export interface Category {
  id: string;
  name: string;
  type: CategoryType;
  createdAt: string;
}

export interface CategoryRequest {
  name: string;
  type: CategoryType;
}

export interface Transaction {
  id: string;
  description: string;
  amount: number;
  type: TransactionType;
  accountId: string | null;
  accountName: string | null;
  cardId: string | null;
  cardName: string | null;
  categoryId: string;
  categoryName: string;
  transactionDate: string;
  installmentCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface TransactionRequest {
  description: string;
  amount: number;
  type: TransactionType;
  accountId: string | null;
  cardId: string | null;
  categoryId: string;
  transactionDate: string;
  installmentCount: number;
}

export interface TransactionPage {
  content: Transaction[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface TransactionFilters {
  startDate?: string;
  endDate?: string;
  type?: TransactionType;
  categoryId?: string;
  accountId?: string;
  page?: number;
  size?: number;
}

export interface CreditCard {
  id: string;
  name: string;
  creditLimit: number;
  closingDay: number;
  dueDay: number;
  outstandingAmount: number;
  availableLimit: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreditCardRequest {
  name: string;
  creditLimit: number;
  closingDay: number;
  dueDay: number;
}

export interface CreditCardInvoice {
  id: string | null;
  cardId: string;
  cardName: string;
  month: string;
  periodStart: string;
  closingDate: string;
  dueDate: string;
  totalAmount: number;
  paid: boolean;
  paidAt: string | null;
  paymentAccountId: string | null;
  paymentAccountName: string | null;
  items: CreditCardInvoiceItem[];
}

export interface CreditCardInvoiceItem {
  description: string;
  amount: number;
  installmentNumber: number | null;
  installmentCount: number | null;
}

export interface DashboardSummary {
  currentBalance: number;
  monthlyIncome: number;
  monthlyExpense: number;
  monthlyResult: number;
}

export interface ExpenseCategory {
  category: string;
  amount: number;
  percentage: number;
}

export interface MonthlyPoint {
  month: string;
  income: number;
  expense: number;
}

export interface Budget {
  id: string;
  categoryId: string;
  categoryName: string;
  month: string;
  limitAmount: number;
  spentAmount: number;
  remainingAmount: number;
  percentageUsed: number;
}

export interface BudgetRequest {
  categoryId: string;
  month: string;
  limitAmount: number;
}
