ALTER TABLE transactions ADD COLUMN installment_count INTEGER NOT NULL DEFAULT 1;

ALTER TABLE transactions DROP CONSTRAINT ck_transactions_origin;
ALTER TABLE transactions ADD CONSTRAINT ck_transactions_origin CHECK (
    (account_id IS NOT NULL AND card_id IS NULL AND invoice_id IS NULL AND installment_count = 1)
    OR (account_id IS NULL AND card_id IS NOT NULL AND invoice_id IS NOT NULL
        AND type = 'EXPENSE' AND installment_count = 1)
    OR (account_id IS NULL AND card_id IS NOT NULL AND invoice_id IS NULL
        AND type = 'EXPENSE' AND installment_count BETWEEN 2 AND 24)
);

ALTER TABLE transactions ADD CONSTRAINT ck_transactions_installment_count
    CHECK (installment_count BETWEEN 1 AND 24);
ALTER TABLE transactions ADD CONSTRAINT uq_transactions_id_owner_card UNIQUE (id, user_id, card_id);

CREATE TABLE credit_card_installments (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    invoice_id UUID NOT NULL,
    card_id UUID NOT NULL,
    user_id UUID NOT NULL,
    installment_number INTEGER NOT NULL,
    installment_count INTEGER NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_credit_card_installments_number CHECK (
        installment_count BETWEEN 2 AND 24 AND installment_number BETWEEN 1 AND installment_count
    ),
    CONSTRAINT ck_credit_card_installments_amount CHECK (amount > 0),
    CONSTRAINT fk_credit_card_installments_purchase FOREIGN KEY (transaction_id, user_id, card_id)
        REFERENCES transactions(id, user_id, card_id) ON DELETE CASCADE,
    CONSTRAINT fk_credit_card_installments_invoice FOREIGN KEY (invoice_id, user_id, card_id)
        REFERENCES credit_card_invoices(id, user_id, card_id) ON DELETE RESTRICT,
    CONSTRAINT uq_credit_card_installments_number UNIQUE (transaction_id, installment_number)
);

CREATE INDEX ix_credit_card_installments_invoice ON credit_card_installments(invoice_id, user_id);
CREATE INDEX ix_credit_card_installments_purchase ON credit_card_installments(transaction_id, user_id);
