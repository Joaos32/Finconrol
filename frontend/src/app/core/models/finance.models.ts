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
  accountId: string;
  accountName: string;
  categoryId: string;
  categoryName: string;
  transactionDate: string;
  createdAt: string;
  updatedAt: string;
}

export interface TransactionRequest {
  description: string;
  amount: number;
  type: TransactionType;
  accountId: string;
  categoryId: string;
  transactionDate: string;
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
