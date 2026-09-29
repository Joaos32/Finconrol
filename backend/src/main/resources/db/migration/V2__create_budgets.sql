CREATE TABLE budgets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    category_id UUID NOT NULL,
    period_start DATE NOT NULL,
    limit_amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_budgets_period_start CHECK (EXTRACT(DAY FROM period_start) = 1),
    CONSTRAINT ck_budgets_limit_positive CHECK (limit_amount > 0),
    CONSTRAINT fk_budgets_category_owner FOREIGN KEY (category_id, user_id)
        REFERENCES categories(id, user_id) ON DELETE RESTRICT,
    CONSTRAINT uq_budgets_owner_category_month UNIQUE (user_id, category_id, period_start)
);

CREATE INDEX ix_budgets_user_month ON budgets(user_id, period_start);
