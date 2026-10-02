package com.fincontrol.creditcard.repository;

import com.fincontrol.creditcard.entity.CreditCardInstallmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CreditCardInstallmentRepository extends JpaRepository<CreditCardInstallmentEntity, UUID> {
    @Query("select i from CreditCardInstallmentEntity i join fetch i.purchase " +
            "where i.invoice.id = :invoiceId and i.user.id = :userId order by i.installmentNumber")
    List<CreditCardInstallmentEntity> findInvoiceItems(@Param("invoiceId") UUID invoiceId,
                                                       @Param("userId") UUID userId);

    @Query("select i from CreditCardInstallmentEntity i " +
            "where i.purchase.id = :purchaseId and i.user.id = :userId order by i.installmentNumber")
    List<CreditCardInstallmentEntity> findAllByPurchase(@Param("purchaseId") UUID purchaseId,
                                                        @Param("userId") UUID userId);

    @Query("select coalesce(sum(i.amount), 0) from CreditCardInstallmentEntity i " +
            "where i.invoice.id = :invoiceId and i.user.id = :userId")
    BigDecimal totalByInvoiceAndUser(@Param("invoiceId") UUID invoiceId, @Param("userId") UUID userId);

    @Query("select coalesce(sum(i.amount), 0) from CreditCardInstallmentEntity i " +
            "where i.card.id = :cardId and i.user.id = :userId and not exists " +
            "(select p.id from CreditCardInvoicePaymentEntity p where p.invoice.id = i.invoice.id)")
    BigDecimal outstandingByCardAndUser(@Param("cardId") UUID cardId, @Param("userId") UUID userId);

    @Query("select case when count(i) > 0 then true else false end from CreditCardInstallmentEntity i " +
            "where i.purchase.id = :purchaseId and i.user.id = :userId and exists " +
            "(select p.id from CreditCardInvoicePaymentEntity p where p.invoice.id = i.invoice.id)")
    boolean hasPaidInvoiceForPurchase(@Param("purchaseId") UUID purchaseId, @Param("userId") UUID userId);

    long countByInvoiceIdAndUserId(UUID invoiceId, UUID userId);

    void deleteAllByPurchaseIdAndUserId(UUID purchaseId, UUID userId);
}
