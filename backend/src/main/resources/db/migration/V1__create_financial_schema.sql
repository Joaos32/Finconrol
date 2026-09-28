CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX ux_users_email_lower ON users (LOWER(email));

CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('CHECKING', 'SAVINGS', 'CASH', 'INVESTMENT', 'OTHER')),
    initial_balance NUMERIC(19, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_accounts_initial_balance CHECK (initial_balance >= 0),
    CONSTRAINT uq_accounts_id_user UNIQUE (id, user_id)
);
CREATE INDEX ix_accounts_user_id ON accounts(user_id);

CREATE TABLE categories (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    name VARCHAR(80) NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_categories_id_user UNIQUE (id, user_id)
);
CREATE UNIQUE INDEX ux_categories_user_type_name ON categories(user_id, type, LOWER(name));
CREATE INDEX ix_categories_user_type ON categories(user_id, type);

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    account_id UUID NOT NULL,
    category_id UUID NOT NULL,
    description VARCHAR(180) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    transaction_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT fk_transactions_account_owner FOREIGN KEY (account_id, user_id)
        REFERENCES accounts(id, user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_transactions_category_owner FOREIGN KEY (category_id, user_id)
        REFERENCES categories(id, user_id) ON DELETE RESTRICT
);
CREATE INDEX ix_transactions_user_date ON transactions(user_id, transaction_date DESC, id);
CREATE INDEX ix_transactions_user_account ON transactions(user_id, account_id);
CREATE INDEX ix_transactions_user_category ON transactions(user_id, category_id);
CREATE INDEX ix_transactions_user_type_date ON transactions(user_id, type, transaction_date DESC);
