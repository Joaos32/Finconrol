package com.fincontrol.creditcard.entity;

import com.fincontrol.account.entity.AccountEntity;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credit_card_invoice_payments", uniqueConstraints =
        @UniqueConstraint(name = "uq_credit_card_invoice_payments_invoice", columnNames = "invoice_id"))
public class CreditCardInvoicePaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private CreditCardInvoiceEntity invoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private CreditCardEntity card;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    protected CreditCardInvoicePaymentEntity() { }

    public CreditCardInvoicePaymentEntity(CreditCardInvoiceEntity invoice, CreditCardEntity card,
                                         UserEntity user, AccountEntity account, BigDecimal amount) {
        this.invoice = invoice;
        this.card = card;
        this.user = user;
        this.account = account;
        this.amount = amount;
        this.paidAt = Instant.now();
    }

    public UUID getId() { return id; }
    public CreditCardInvoiceEntity getInvoice() { return invoice; }
    public UserEntity getUser() { return user; }
    public CreditCardEntity getCard() { return card; }
    public AccountEntity getAccount() { return account; }
    public BigDecimal getAmount() { return amount; }
    public Instant getPaidAt() { return paidAt; }
}
