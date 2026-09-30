package com.fincontrol;

import com.fincontrol.account.entity.AccountEntity;
import com.fincontrol.account.entity.AccountType;
import com.fincontrol.account.repository.AccountBalanceProjection;
import com.fincontrol.account.repository.AccountRepository;
import com.fincontrol.category.entity.CategoryEntity;
import com.fincontrol.category.entity.CategoryType;
import com.fincontrol.category.repository.CategoryRepository;
import com.fincontrol.creditcard.entity.CreditCardEntity;
import com.fincontrol.creditcard.entity.CreditCardInvoiceEntity;
import com.fincontrol.creditcard.entity.CreditCardInvoicePaymentEntity;
import com.fincontrol.creditcard.repository.CreditCardInvoicePaymentRepository;
import com.fincontrol.creditcard.repository.CreditCardInvoiceRepository;
import com.fincontrol.creditcard.repository.CreditCardRepository;
import com.fincontrol.transaction.entity.TransactionEntity;
import com.fincontrol.transaction.entity.TransactionType;
import com.fincontrol.transaction.repository.TransactionRepository;
import com.fincontrol.user.entity.UserEntity;
import com.fincontrol.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
class RepositoryIsolationIntegrationTest {
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
            .withDatabaseName("fincontrol")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private UserRepository users;
    @Autowired private AccountRepository accounts;
    @Autowired private CategoryRepository categories;
    @Autowired private TransactionRepository transactions;
    @Autowired private CreditCardRepository creditCards;
    @Autowired private CreditCardInvoiceRepository invoices;
    @Autowired private CreditCardInvoicePaymentRepository invoicePayments;

    @Test
    @Transactional
    void calculatesAccountBalanceFromPostgresTransactions() {
        UserEntity user = users.save(new UserEntity("Demo", "db-test@example.com", "bcrypt-hash"));
        AccountEntity account = accounts.save(new AccountEntity(user, "Principal", AccountType.CHECKING,
                new BigDecimal("100.00")));
        CategoryEntity income = categories.save(new CategoryEntity(user, "Salário", CategoryType.INCOME));
        CategoryEntity expense = categories.save(new CategoryEntity(user, "Mercado", CategoryType.EXPENSE));
        transactions.save(new TransactionEntity(user, account, income, "Pagamento", new BigDecimal("50.00"),
                TransactionType.INCOME, LocalDate.of(2026, 9, 1)));
        transactions.save(new TransactionEntity(user, account, expense, "Compra", new BigDecimal("20.00"),
                TransactionType.EXPENSE, LocalDate.of(2026, 9, 2)));
        CreditCardEntity card = creditCards.save(new CreditCardEntity(user, "CartÃ£o", new BigDecimal("1000.00"), 20, 5));
        CreditCardInvoiceEntity invoice = invoices.save(new CreditCardInvoiceEntity(card, user,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 8, 21), LocalDate.of(2026, 9, 20),
                LocalDate.of(2026, 10, 5)));
        transactions.save(new TransactionEntity(user, null, card, invoice, expense, "Compra no cartÃ£o",
                new BigDecimal("40.00"), TransactionType.EXPENSE, LocalDate.of(2026, 9, 3)));
        invoicePayments.save(new CreditCardInvoicePaymentEntity(invoice, card, user, account, new BigDecimal("40.00")));

        AccountBalanceProjection result = accounts.findBalancesByUserId(user.getId()).getFirst();

        assertEquals(account.getId(), result.getAccountId());
        assertEquals(new BigDecimal("90.00"), result.getCurrentBalance());
        assertTrue(users.findByEmailIgnoreCase("db-test@example.com").isPresent());
    }
}
