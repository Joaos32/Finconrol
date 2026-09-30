package com.fincontrol.creditcard.entity;

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
import java.util.UUID;

@Entity
@Table(name = "credit_cards")
public class CreditCardEntity extends AuditedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "credit_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "closing_day", nullable = false)
    private int closingDay;

    @Column(name = "due_day", nullable = false)
    private int dueDay;

    protected CreditCardEntity() { }

    public CreditCardEntity(UserEntity user, String name, BigDecimal creditLimit, int closingDay, int dueDay) {
        this.user = user;
        this.name = name;
        this.creditLimit = creditLimit;
        this.closingDay = closingDay;
        this.dueDay = dueDay;
    }

    public UUID getId() { return id; }
    public UserEntity getUser() { return user; }
    public String getName() { return name; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public int getClosingDay() { return closingDay; }
    public int getDueDay() { return dueDay; }

    public void update(String name, BigDecimal creditLimit, int closingDay, int dueDay) {
        this.name = name;
        this.creditLimit = creditLimit;
        this.closingDay = closingDay;
        this.dueDay = dueDay;
    }
}
