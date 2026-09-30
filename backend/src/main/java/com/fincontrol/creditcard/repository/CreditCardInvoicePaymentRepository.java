package com.fincontrol.creditcard.repository;

import com.fincontrol.creditcard.entity.CreditCardInvoicePaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CreditCardInvoicePaymentRepository extends JpaRepository<CreditCardInvoicePaymentEntity, UUID> {
    Optional<CreditCardInvoicePaymentEntity> findByInvoiceId(UUID invoiceId);
    boolean existsByInvoiceId(UUID invoiceId);
    boolean existsByCardIdAndUserId(UUID cardId, UUID userId);
}
