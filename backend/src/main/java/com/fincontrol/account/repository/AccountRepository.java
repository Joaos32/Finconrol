package com.fincontrol.account.repository;

import com.fincontrol.account.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    List<AccountEntity> findAllByUserIdOrderByNameAsc(UUID userId);
    Optional<AccountEntity> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByIdAndUserId(UUID id, UUID userId);

    @Query(value = """
            SELECT a.id AS "accountId",
                   a.initial_balance
                   + COALESCE((SELECT SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE -t.amount END)
                               FROM transactions t WHERE t.account_id = a.id AND t.user_id = a.user_id), 0)
                   - COALESCE((SELECT SUM(p.amount) FROM credit_card_invoice_payments p
                               WHERE p.account_id = a.id AND p.user_id = a.user_id), 0) AS "currentBalance"
            FROM accounts a
            WHERE a.user_id = :userId
            """, nativeQuery = true)
    List<AccountBalanceProjection> findBalancesByUserId(@Param("userId") UUID userId);

    @Query("select coalesce(sum(a.initialBalance), 0) from AccountEntity a where a.user.id = :userId")
    BigDecimal sumInitialBalancesByUserId(@Param("userId") UUID userId);
}
