package com.fincontrol.creditcard.entity;

import com.fincontrol.transaction.entity.TransactionEntity;
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

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "credit_card_installments", uniqueConstraints =
        @UniqueConstraint(name = "uq_credit_card_installments_number", columnNames = {"transaction_id", "installment_number"}))
public class CreditCardInstallmentEntity extends AuditedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private TransactionEntity purchase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private CreditCardInvoiceEntity invoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private CreditCardEntity card;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "installment_number", nullable = false)
    private int installmentNumber;

    @Column(name = "installment_count", nullable = false)
    private int installmentCount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    protected CreditCardInstallmentEntity() { }

    public CreditCardInstallmentEntity(TransactionEntity purchase, CreditCardInvoiceEntity invoice,
                                       CreditCardEntity card, UserEntity user, int installmentNumber,
                                       int installmentCount, BigDecimal amount) {
        this.purchase = purchase;
        this.invoice = invoice;
        this.card = card;
        this.user = user;
        this.installmentNumber = installmentNumber;
        this.installmentCount = installmentCount;
        this.amount = amount;
    }

    public UUID getId() { return id; }
    public TransactionEntity getPurchase() { return purchase; }
    public CreditCardInvoiceEntity getInvoice() { return invoice; }
    public CreditCardEntity getCard() { return card; }
    public UserEntity getUser() { return user; }
    public int getInstallmentNumber() { return installmentNumber; }
    public int getInstallmentCount() { return installmentCount; }
    public BigDecimal getAmount() { return amount; }
}
