CREATE TABLE credit_cards (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    name VARCHAR(100) NOT NULL,
    credit_limit NUMERIC(19, 2) NOT NULL,
    closing_day INTEGER NOT NULL,
    due_day INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_credit_cards_limit_positive CHECK (credit_limit > 0),
    CONSTRAINT ck_credit_cards_closing_day CHECK (closing_day BETWEEN 1 AND 28),
    CONSTRAINT ck_credit_cards_due_day CHECK (due_day BETWEEN 1 AND 28),
    CONSTRAINT uq_credit_cards_id_user UNIQUE (id, user_id)
);
CREATE INDEX ix_credit_cards_user_id ON credit_cards(user_id);

CREATE TABLE credit_card_invoices (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL,
    user_id UUID NOT NULL,
    closing_month DATE NOT NULL,
    period_start DATE NOT NULL,
    closing_date DATE NOT NULL,
    due_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_credit_card_invoices_month_start CHECK (EXTRACT(DAY FROM closing_month) = 1),
    CONSTRAINT fk_credit_card_invoices_card_owner FOREIGN KEY (card_id, user_id)
        REFERENCES credit_cards(id, user_id) ON DELETE RESTRICT,
    CONSTRAINT uq_credit_card_invoices_id_owner_card UNIQUE (id, user_id, card_id),
    CONSTRAINT uq_credit_card_invoices_card_month UNIQUE (card_id, closing_month)
);
CREATE INDEX ix_credit_card_invoices_user_month ON credit_card_invoices(user_id, closing_month);

ALTER TABLE transactions ALTER COLUMN account_id DROP NOT NULL;
ALTER TABLE transactions ADD COLUMN card_id UUID;
ALTER TABLE transactions ADD COLUMN invoice_id UUID;
ALTER TABLE transactions ADD CONSTRAINT fk_transactions_card_owner
    FOREIGN KEY (card_id, user_id) REFERENCES credit_cards(id, user_id) ON DELETE RESTRICT;
ALTER TABLE transactions ADD CONSTRAINT fk_transactions_invoice_owner_card
    FOREIGN KEY (invoice_id, user_id, card_id)
    REFERENCES credit_card_invoices(id, user_id, card_id) ON DELETE RESTRICT;
ALTER TABLE transactions ADD CONSTRAINT ck_transactions_origin CHECK (
    (account_id IS NOT NULL AND card_id IS NULL AND invoice_id IS NULL)
    OR (account_id IS NULL AND card_id IS NOT NULL AND invoice_id IS NOT NULL AND type = 'EXPENSE')
);
CREATE INDEX ix_transactions_user_card ON transactions(user_id, card_id);
CREATE INDEX ix_transactions_invoice ON transactions(invoice_id);

CREATE TABLE credit_card_invoice_payments (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL,
    card_id UUID NOT NULL,
    user_id UUID NOT NULL,
    account_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    paid_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_credit_card_invoice_payments_amount_positive CHECK (amount > 0),
    CONSTRAINT fk_credit_card_invoice_payments_invoice FOREIGN KEY (invoice_id, user_id, card_id)
        REFERENCES credit_card_invoices(id, user_id, card_id) ON DELETE RESTRICT,
    CONSTRAINT fk_credit_card_invoice_payments_account FOREIGN KEY (account_id, user_id)
        REFERENCES accounts(id, user_id) ON DELETE RESTRICT,
    CONSTRAINT uq_credit_card_invoice_payments_invoice UNIQUE (invoice_id)
);
CREATE INDEX ix_credit_card_invoice_payments_account ON credit_card_invoice_payments(account_id);
