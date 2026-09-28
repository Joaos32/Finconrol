package com.fincontrol.transaction.entity;

import com.fincontrol.account.entity.AccountEntity;
import com.fincontrol.category.entity.CategoryEntity;
import com.fincontrol.user.entity.AuditedEntity;
import com.fincontrol.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class TransactionEntity extends AuditedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @Column(nullable = false, length = 180)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    protected TransactionEntity() { }

    public TransactionEntity(UserEntity user, AccountEntity account, CategoryEntity category,
                             String description, BigDecimal amount, TransactionType type, LocalDate transactionDate) {
        this.user = user;
        this.account = account;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.transactionDate = transactionDate;
    }

    public UUID getId() { return id; }
    public UserEntity getUser() { return user; }
    public AccountEntity getAccount() { return account; }
    public CategoryEntity getCategory() { return category; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public void update(AccountEntity account, CategoryEntity category, String description,
                       BigDecimal amount, TransactionType type, LocalDate transactionDate) {
        this.account = account;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.transactionDate = transactionDate;
    }
}
