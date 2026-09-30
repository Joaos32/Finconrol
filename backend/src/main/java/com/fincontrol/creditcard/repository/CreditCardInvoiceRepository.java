package com.fincontrol.creditcard.repository;

import com.fincontrol.creditcard.entity.CreditCardInvoiceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface CreditCardInvoiceRepository extends JpaRepository<CreditCardInvoiceEntity, UUID> {
    boolean existsByCardIdAndUserId(UUID cardId, UUID userId);
    Optional<CreditCardInvoiceEntity> findByCardIdAndUserIdAndClosingMonth(UUID cardId, UUID userId, LocalDate closingMonth);
    void deleteAllByCardIdAndUserId(UUID cardId, UUID userId);

    @Query("select case when count(i) > 0 then true else false end from CreditCardInvoiceEntity i " +
            "where i.card.id = :cardId and i.user.id = :userId and not exists " +
            "(select p.id from CreditCardInvoicePaymentEntity p where p.invoice.id = i.id)")
    boolean existsUnpaidByCardAndUser(@Param("cardId") UUID cardId, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from CreditCardInvoiceEntity i where i.card.id = :cardId and i.user.id = :userId and i.closingMonth = :closingMonth")
    Optional<CreditCardInvoiceEntity> findForUpdateByCardAndMonth(@Param("cardId") UUID cardId,
                                                                  @Param("userId") UUID userId,
                                                                  @Param("closingMonth") LocalDate closingMonth);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from CreditCardInvoiceEntity i where i.id = :invoiceId and i.card.id = :cardId and i.user.id = :userId")
    Optional<CreditCardInvoiceEntity> findOwnedForUpdate(@Param("invoiceId") UUID invoiceId,
                                                         @Param("cardId") UUID cardId,
                                                         @Param("userId") UUID userId);

    @Query("select coalesce(sum(t.amount), 0) from TransactionEntity t where t.invoice.id = :invoiceId and t.user.id = :userId")
    BigDecimal totalByInvoiceAndUser(@Param("invoiceId") UUID invoiceId, @Param("userId") UUID userId);

    @Query("select coalesce(sum(t.amount), 0) from TransactionEntity t where t.invoice.card.id = :cardId " +
            "and t.user.id = :userId and not exists " +
            "(select p.id from CreditCardInvoicePaymentEntity p where p.invoice.id = t.invoice.id)")
    BigDecimal outstandingByCardAndUser(@Param("cardId") UUID cardId, @Param("userId") UUID userId);
}
