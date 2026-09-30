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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "credit_card_invoices", uniqueConstraints =
        @UniqueConstraint(name = "uq_credit_card_invoices_card_month", columnNames = {"card_id", "closing_month"}))
public class CreditCardInvoiceEntity extends AuditedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private CreditCardEntity card;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "closing_month", nullable = false)
    private LocalDate closingMonth;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "closing_date", nullable = false)
    private LocalDate closingDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    protected CreditCardInvoiceEntity() { }

    public CreditCardInvoiceEntity(CreditCardEntity card, UserEntity user, LocalDate closingMonth,
                                   LocalDate periodStart, LocalDate closingDate, LocalDate dueDate) {
        this.card = card;
        this.user = user;
        this.closingMonth = closingMonth;
        this.periodStart = periodStart;
        this.closingDate = closingDate;
        this.dueDate = dueDate;
    }

    public UUID getId() { return id; }
    public CreditCardEntity getCard() { return card; }
    public UserEntity getUser() { return user; }
    public LocalDate getClosingMonth() { return closingMonth; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getClosingDate() { return closingDate; }
    public LocalDate getDueDate() { return dueDate; }
}
