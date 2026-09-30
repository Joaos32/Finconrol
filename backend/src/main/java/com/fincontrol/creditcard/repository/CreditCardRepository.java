package com.fincontrol.creditcard.repository;

import com.fincontrol.creditcard.entity.CreditCardEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditCardRepository extends JpaRepository<CreditCardEntity, UUID> {
    List<CreditCardEntity> findAllByUserIdOrderByNameAsc(UUID userId);
    Optional<CreditCardEntity> findByIdAndUserId(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CreditCardEntity c where c.id = :cardId and c.user.id = :userId")
    Optional<CreditCardEntity> findOwnedForUpdate(@Param("cardId") UUID cardId, @Param("userId") UUID userId);

    @Query(value = "SELECT closing_day AS \"closingDay\", due_day AS \"dueDay\" FROM credit_cards " +
            "WHERE id = :cardId AND user_id = :userId FOR UPDATE", nativeQuery = true)
    Optional<CreditCardCycleSettingsProjection> findCycleSettingsForUpdate(@Param("cardId") UUID cardId,
                                                                            @Param("userId") UUID userId);

    @Query(value = """
            SELECT c.id AS "cardId", COALESCE(SUM(CASE WHEN p.id IS NULL THEN t.amount ELSE 0 END), 0) AS "outstandingAmount"
            FROM credit_cards c
            LEFT JOIN credit_card_invoices i ON i.card_id = c.id AND i.user_id = c.user_id
            LEFT JOIN transactions t ON t.invoice_id = i.id AND t.user_id = c.user_id
            LEFT JOIN credit_card_invoice_payments p ON p.invoice_id = i.id
            WHERE c.user_id = :userId
            GROUP BY c.id
            """, nativeQuery = true)
    List<CreditCardOutstandingProjection> outstandingByUser(@Param("userId") UUID userId);
}
