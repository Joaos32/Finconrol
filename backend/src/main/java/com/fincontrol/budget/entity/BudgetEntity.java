package com.fincontrol.budget.entity;

import com.fincontrol.category.entity.CategoryEntity;
import com.fincontrol.user.entity.AuditedEntity;
import com.fincontrol.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "budgets")
public class BudgetEntity extends AuditedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "limit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal limitAmount;

    protected BudgetEntity() { }

    public BudgetEntity(UserEntity user, CategoryEntity category, LocalDate periodStart, BigDecimal limitAmount) {
        this.user = user;
        this.category = category;
        this.periodStart = periodStart;
        this.limitAmount = limitAmount;
    }

    public UUID getId() { return id; }
    public UserEntity getUser() { return user; }
    public CategoryEntity getCategory() { return category; }
    public LocalDate getPeriodStart() { return periodStart; }
    public BigDecimal getLimitAmount() { return limitAmount; }

    public void update(CategoryEntity category, LocalDate periodStart, BigDecimal limitAmount) {
        this.category = category;
        this.periodStart = periodStart;
        this.limitAmount = limitAmount;
    }
}
