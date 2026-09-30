package com.fincontrol;

import com.fincontrol.account.dto.AccountDtos;
import com.fincontrol.account.entity.AccountType;
import com.fincontrol.auth.dto.AuthDtos;
import com.fincontrol.budget.dto.BudgetDtos;
import com.fincontrol.category.dto.CategoryDtos;
import com.fincontrol.category.entity.CategoryType;
import com.fincontrol.creditcard.dto.CreditCardDtos;
import com.fincontrol.dashboard.dto.DashboardDtos;
import com.fincontrol.transaction.dto.TransactionDtos;
import com.fincontrol.transaction.entity.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "fincontrol.swagger.enabled=true",
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=true"
})
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
class FinancialApiAcceptanceTest {
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

    @Autowired private TestRestTemplate http;

    @Test
    void exposesOpenApiContractWhenSwaggerIsEnabled() {
        ResponseEntity<String> documentation = http.getForEntity("/v3/api-docs", String.class);

        assertEquals(HttpStatus.OK, documentation.getStatusCode());
        assertNotNull(documentation.getBody());
        assertTrue(documentation.getBody().contains("/api/transactions"));
        assertTrue(documentation.getBody().contains("/api/budgets"));
        assertTrue(documentation.getBody().contains("/api/credit-cards"));
    }

    @Test
    void recordsCardPurchasesOnceAndPaymentOnlyReducesCashBalance() {
        String email = "card-acceptance-" + UUID.randomUUID() + "@example.com";
        AuthDtos.AuthResponse registration = http.postForEntity("/api/auth/register",
                new AuthDtos.RegisterRequest("Pessoa CartÃ£o", email, "SenhaSegura123!"),
                AuthDtos.AuthResponse.class).getBody();
        assertNotNull(registration);
        String token = registration.accessToken();

        AccountDtos.Response account = exchange("/api/accounts", HttpMethod.POST,
                new AccountDtos.Request("Conta para fatura", AccountType.CHECKING, new BigDecimal("100.00")),
                token, AccountDtos.Response.class).getBody();
        CategoryDtos.Response expenseCategory = exchange("/api/categories", HttpMethod.POST,
                new CategoryDtos.Request("Compras no crÃ©dito", CategoryType.EXPENSE), token,
                CategoryDtos.Response.class).getBody();
        CreditCardDtos.Response card = exchange("/api/credit-cards", HttpMethod.POST,
                new CreditCardDtos.Request("CartÃ£o principal", new BigDecimal("500.00"), 20, 5),
                token, CreditCardDtos.Response.class).getBody();
        assertNotNull(account);
        assertNotNull(expenseCategory);
        assertNotNull(card);

        CreditCardDtos.InvoiceResponse emptyInvoice = exchange(
                "/api/credit-cards/" + card.id() + "/invoices?month=2026-06", HttpMethod.GET,
                null, token, CreditCardDtos.InvoiceResponse.class).getBody();
        assertNotNull(emptyInvoice);
        assertNull(emptyInvoice.id());
        assertEquals(BigDecimal.ZERO.setScale(2), emptyInvoice.totalAmount());

        LocalDate purchaseDate = LocalDate.of(2026, 6, 20);
        ResponseEntity<TransactionDtos.Response> purchaseResponse = exchange("/api/transactions", HttpMethod.POST,
                new TransactionDtos.Request("Compra no crÃ©dito", new BigDecimal("60.00"), TransactionType.EXPENSE,
                        null, card.id(), expenseCategory.id(), purchaseDate), token, TransactionDtos.Response.class);
        assertEquals(HttpStatus.CREATED, purchaseResponse.getStatusCode());
        assertNull(purchaseResponse.getBody().accountId());
        assertEquals(card.id(), purchaseResponse.getBody().cardId());

        CreditCardDtos.InvoiceResponse invoice = exchange(
                "/api/credit-cards/" + card.id() + "/invoices?month=2026-06", HttpMethod.GET,
                null, token, CreditCardDtos.InvoiceResponse.class).getBody();
        assertNotNull(invoice.id());
        assertEquals(new BigDecimal("60.00"), invoice.totalAmount());
        assertEquals(LocalDate.of(2026, 5, 21), invoice.periodStart());
        assertEquals(LocalDate.of(2026, 6, 20), invoice.closingDate());
        assertEquals(LocalDate.of(2026, 7, 5), invoice.dueDate());

        ResponseEntity<String> changeClosingWithOpenInvoice = http.exchange("/api/credit-cards/" + card.id(),
                HttpMethod.PUT, new HttpEntity<>(new CreditCardDtos.Request("CartÃ£o principal",
                        new BigDecimal("500.00"), 21, 5), authorized(token).getHeaders()), String.class);
        assertEquals(HttpStatus.CONFLICT, changeClosingWithOpenInvoice.getStatusCode());

        BudgetDtos.Response budget = exchange("/api/budgets", HttpMethod.POST,
                new BudgetDtos.Request(expenseCategory.id(), "2026-06", new BigDecimal("70.00")),
                token, BudgetDtos.Response.class).getBody();
        assertEquals(new BigDecimal("60.00"), budget.spentAmount());
        ResponseEntity<List<CreditCardDtos.Response>> cardBeforePayment = http.exchange("/api/credit-cards", HttpMethod.GET,
                authorized(token), new ParameterizedTypeReference<>() { });
        assertEquals(new BigDecimal("60.00"), cardBeforePayment.getBody().getFirst().outstandingAmount());
        assertEquals(new BigDecimal("440.00"), cardBeforePayment.getBody().getFirst().availableLimit());

        AccountDtos.Response balanceBeforePayment = exchange("/api/accounts/" + account.id(), HttpMethod.GET,
                null, token, AccountDtos.Response.class).getBody();
        assertEquals(new BigDecimal("100.00"), balanceBeforePayment.currentBalance());

        AuthDtos.AuthResponse otherUser = http.postForEntity("/api/auth/register",
                new AuthDtos.RegisterRequest("Outra pessoa", "card-other-" + UUID.randomUUID() + "@example.com",
                        "SenhaSegura123!"), AuthDtos.AuthResponse.class).getBody();
        assertNotNull(otherUser);
        AccountDtos.Response foreignAccount = exchange("/api/accounts", HttpMethod.POST,
                new AccountDtos.Request("Conta de outra pessoa", AccountType.CHECKING, new BigDecimal("90.00")),
                otherUser.accessToken(), AccountDtos.Response.class).getBody();
        assertNotNull(foreignAccount);
        ResponseEntity<String> foreignInvoice = http.exchange(
                "/api/credit-cards/" + card.id() + "/invoices?month=2026-06", HttpMethod.GET,
                authorized(otherUser.accessToken()), String.class);
        assertEquals(HttpStatus.NOT_FOUND, foreignInvoice.getStatusCode());
        ResponseEntity<String> foreignPaymentAccount = http.exchange(
                "/api/credit-cards/" + card.id() + "/invoices/" + invoice.id() + "/payments", HttpMethod.POST,
                new HttpEntity<>(new CreditCardDtos.PaymentRequest(foreignAccount.id()), authorized(token).getHeaders()),
                String.class);
        assertEquals(HttpStatus.NOT_FOUND, foreignPaymentAccount.getStatusCode());

        CreditCardDtos.InvoiceResponse paid = exchange(
                "/api/credit-cards/" + card.id() + "/invoices/" + invoice.id() + "/payments", HttpMethod.POST,
                new CreditCardDtos.PaymentRequest(account.id()), token, CreditCardDtos.InvoiceResponse.class).getBody();
        assertTrue(paid.paid());
        assertEquals(account.id(), paid.paymentAccountId());
        assertEquals(new BigDecimal("60.00"), paid.totalAmount());

        AccountDtos.Response balanceAfterPayment = exchange("/api/accounts/" + account.id(), HttpMethod.GET,
                null, token, AccountDtos.Response.class).getBody();
        assertEquals(new BigDecimal("40.00"), balanceAfterPayment.currentBalance());
        ResponseEntity<List<BudgetDtos.Response>> budgetAfterPayment = http.exchange("/api/budgets?month=2026-06",
                HttpMethod.GET, authorized(token), new ParameterizedTypeReference<>() { });
        assertEquals(new BigDecimal("60.00"), budgetAfterPayment.getBody().getFirst().spentAmount());
        ResponseEntity<List<CreditCardDtos.Response>> listedCards = http.exchange("/api/credit-cards", HttpMethod.GET,
                authorized(token), new ParameterizedTypeReference<>() { });
        assertEquals(0, listedCards.getBody().getFirst().outstandingAmount().compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("500.00"), listedCards.getBody().getFirst().availableLimit());
        ResponseEntity<String> changeClosingAfterPaidInvoice = http.exchange("/api/credit-cards/" + card.id(),
                HttpMethod.PUT, new HttpEntity<>(new CreditCardDtos.Request("CartÃ£o principal",
                        new BigDecimal("500.00"), 21, 5), authorized(token).getHeaders()), String.class);
        assertEquals(HttpStatus.CONFLICT, changeClosingAfterPaidInvoice.getStatusCode());
        CreditCardDtos.Response updatedCard = exchange("/api/credit-cards/" + card.id(), HttpMethod.PUT,
                new CreditCardDtos.Request("CartÃ£o principal", new BigDecimal("500.00"), 20, 6),
                token, CreditCardDtos.Response.class).getBody();
        assertEquals(20, updatedCard.closingDay());
        CreditCardDtos.InvoiceResponse historicalInvoice = exchange(
                "/api/credit-cards/" + card.id() + "/invoices?month=2026-06", HttpMethod.GET,
                null, token, CreditCardDtos.InvoiceResponse.class).getBody();
        assertEquals(LocalDate.of(2026, 6, 20), historicalInvoice.closingDate());

        ResponseEntity<String> duplicatePayment = http.exchange(
                "/api/credit-cards/" + card.id() + "/invoices/" + invoice.id() + "/payments", HttpMethod.POST,
                new HttpEntity<>(new CreditCardDtos.PaymentRequest(account.id()), authorized(token).getHeaders()), String.class);
        assertEquals(HttpStatus.CONFLICT, duplicatePayment.getStatusCode());
        ResponseEntity<String> editPaidPurchase = http.exchange("/api/transactions/" + purchaseResponse.getBody().id(),
                HttpMethod.PUT, new HttpEntity<>(new TransactionDtos.Request("Compra alterada", new BigDecimal("60.00"),
                        TransactionType.EXPENSE, null, card.id(), expenseCategory.id(), purchaseDate),
                        authorized(token).getHeaders()), String.class);
        assertEquals(HttpStatus.CONFLICT, editPaidPurchase.getStatusCode());
        ResponseEntity<String> addToPaidInvoice = http.exchange("/api/transactions", HttpMethod.POST,
                new HttpEntity<>(new TransactionDtos.Request("Compra retroativa", new BigDecimal("15.00"),
                        TransactionType.EXPENSE, null, card.id(), expenseCategory.id(), purchaseDate),
                        authorized(token).getHeaders()), String.class);
        assertEquals(HttpStatus.CONFLICT, addToPaidInvoice.getStatusCode());
        ResponseEntity<String> deletePaidPurchase = http.exchange("/api/transactions/" + purchaseResponse.getBody().id(),
                HttpMethod.DELETE, authorized(token), String.class);
        assertEquals(HttpStatus.CONFLICT, deletePaidPurchase.getStatusCode());

        CreditCardDtos.Response temporaryCard = exchange("/api/credit-cards", HttpMethod.POST,
                new CreditCardDtos.Request("CartÃ£o temporÃ¡rio", new BigDecimal("100.00"), 20, 5),
                token, CreditCardDtos.Response.class).getBody();
        TransactionDtos.Response temporaryPurchase = exchange("/api/transactions", HttpMethod.POST,
                new TransactionDtos.Request("Compra removÃ­vel", new BigDecimal("5.00"), TransactionType.EXPENSE,
                        null, temporaryCard.id(), expenseCategory.id(), LocalDate.of(2026, 7, 1)),
                token, TransactionDtos.Response.class).getBody();
        assertEquals(HttpStatus.NO_CONTENT, http.exchange("/api/transactions/" + temporaryPurchase.id(),
                HttpMethod.DELETE, authorized(token), String.class).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, http.exchange("/api/credit-cards/" + temporaryCard.id(),
                HttpMethod.DELETE, authorized(token), String.class).getStatusCode());
    }

    @Test
    void registersLogsInAndCompletesPersonalFinanceFlow() {
        assertEquals(HttpStatus.UNAUTHORIZED, http.getForEntity("/api/accounts", Object.class).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED,
                http.getForEntity("/api/budgets?month=2026-06", Object.class).getStatusCode());

        String email = "acceptance-" + UUID.randomUUID() + "@example.com";
        AuthDtos.AuthResponse registration = http.postForEntity("/api/auth/register",
                new AuthDtos.RegisterRequest("Pessoa Teste", email, "SenhaSegura123!"),
                AuthDtos.AuthResponse.class).getBody();
        assertNotNull(registration);
        assertEquals("Bearer", registration.tokenType());

        AuthDtos.AuthResponse login = http.postForEntity("/api/auth/login",
                new AuthDtos.LoginRequest(email, "SenhaSegura123!"), AuthDtos.AuthResponse.class).getBody();
        assertNotNull(login);
        String token = login.accessToken();

        AccountDtos.Response account = exchange("/api/accounts", HttpMethod.POST,
                new AccountDtos.Request("Conta principal", AccountType.CHECKING, new BigDecimal("100.00")),
                token, AccountDtos.Response.class).getBody();
        assertNotNull(account);

        CategoryDtos.Response incomeCategory = exchange("/api/categories", HttpMethod.POST,
                new CategoryDtos.Request("Renda de teste", CategoryType.INCOME), token,
                CategoryDtos.Response.class).getBody();
        CategoryDtos.Response expenseCategory = exchange("/api/categories", HttpMethod.POST,
                new CategoryDtos.Request("Despesa de teste", CategoryType.EXPENSE), token,
                CategoryDtos.Response.class).getBody();
        assertNotNull(incomeCategory);
        assertNotNull(expenseCategory);

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        exchange("/api/transactions", HttpMethod.POST,
                new TransactionDtos.Request("Salário", new BigDecimal("200.00"), TransactionType.INCOME,
                        account.id(), incomeCategory.id(), today), token, TransactionDtos.Response.class);
        exchange("/api/transactions", HttpMethod.POST,
                new TransactionDtos.Request("Mercado", new BigDecimal("45.50"), TransactionType.EXPENSE,
                        account.id(), expenseCategory.id(), today), token, TransactionDtos.Response.class);

        ResponseEntity<AccountDtos.Response> balance = exchange("/api/accounts/" + account.id(), HttpMethod.GET,
                null, token, AccountDtos.Response.class);
        assertEquals(HttpStatus.OK, balance.getStatusCode());
        assertEquals(new BigDecimal("254.50"), balance.getBody().currentBalance());

        ResponseEntity<TransactionDtos.PageResponse<TransactionDtos.Response>> expenses = http.exchange(
                "/api/transactions?type=EXPENSE&accountId=" + account.id() + "&size=20", HttpMethod.GET,
                authorized(token), new ParameterizedTypeReference<>() { });
        assertEquals(HttpStatus.OK, expenses.getStatusCode());
        assertEquals(1, expenses.getBody().totalElements());
        assertEquals("Mercado", expenses.getBody().content().getFirst().description());

        DashboardDtos.Summary summary = exchange("/api/dashboard/summary", HttpMethod.GET, null, token,
                DashboardDtos.Summary.class).getBody();
        assertNotNull(summary);
        assertEquals(new BigDecimal("254.50"), summary.currentBalance());
        assertEquals(new BigDecimal("200.00"), summary.monthlyIncome());
        assertEquals(new BigDecimal("45.50"), summary.monthlyExpense());

        String otherUserToken = http.postForEntity("/api/auth/register",
                new AuthDtos.RegisterRequest("Outra Pessoa", "other-" + UUID.randomUUID() + "@example.com",
                        "SenhaSegura123!"), AuthDtos.AuthResponse.class).getBody().accessToken();
        ResponseEntity<List<AccountDtos.Response>> otherUsersAccounts = http.exchange("/api/accounts", HttpMethod.GET,
                authorized(otherUserToken), new ParameterizedTypeReference<>() { });
        assertEquals(HttpStatus.OK, otherUsersAccounts.getStatusCode());
        assertEquals(0, otherUsersAccounts.getBody().size());

        ResponseEntity<String> foreignAccount = http.exchange("/api/accounts/" + account.id(), HttpMethod.GET,
                authorized(otherUserToken), String.class);
        assertEquals(HttpStatus.NOT_FOUND, foreignAccount.getStatusCode());

        YearMonth month = YearMonth.from(today);
        List<TransactionDtos.Request> periodExpenses = List.of(
                new TransactionDtos.Request("Primeiro dia do mês", new BigDecimal("10.00"), TransactionType.EXPENSE,
                        account.id(), expenseCategory.id(), month.atDay(1)),
                new TransactionDtos.Request("Último dia do mês", new BigDecimal("20.00"), TransactionType.EXPENSE,
                        account.id(), expenseCategory.id(), month.atEndOfMonth()),
                new TransactionDtos.Request("Mês anterior", new BigDecimal("30.00"), TransactionType.EXPENSE,
                        account.id(), expenseCategory.id(), month.minusMonths(1).atEndOfMonth()),
                new TransactionDtos.Request("Mês seguinte", new BigDecimal("40.00"), TransactionType.EXPENSE,
                        account.id(), expenseCategory.id(), month.plusMonths(1).atDay(1)));
        periodExpenses.forEach(request -> exchange("/api/transactions", HttpMethod.POST, request,
                token, TransactionDtos.Response.class));

        String monthValue = month.toString();
        BudgetDtos.Request budgetRequest = new BudgetDtos.Request(expenseCategory.id(), monthValue,
                new BigDecimal("70.00"));
        ResponseEntity<BudgetDtos.Response> createdBudget = exchange("/api/budgets", HttpMethod.POST,
                budgetRequest, token, BudgetDtos.Response.class);
        assertEquals(HttpStatus.CREATED, createdBudget.getStatusCode());
        assertEquals(new BigDecimal("75.50"), createdBudget.getBody().spentAmount());
        assertEquals(new BigDecimal("-5.50"), createdBudget.getBody().remainingAmount());
        assertEquals(new BigDecimal("107.9"), createdBudget.getBody().percentageUsed());

        ResponseEntity<List<BudgetDtos.Response>> listedBudgets = http.exchange("/api/budgets?month=" + monthValue,
                HttpMethod.GET, authorized(token), new ParameterizedTypeReference<>() { });
        assertEquals(1, listedBudgets.getBody().size());
        assertEquals(0, http.exchange("/api/budgets?month=" + month.minusMonths(1), HttpMethod.GET,
                authorized(token), new ParameterizedTypeReference<List<BudgetDtos.Response>>() { }).getBody().size());
        assertEquals(0, http.exchange("/api/budgets?month=" + monthValue, HttpMethod.GET,
                authorized(otherUserToken), new ParameterizedTypeReference<List<BudgetDtos.Response>>() { }).getBody().size());

        ResponseEntity<String> duplicateBudget = http.exchange("/api/budgets", HttpMethod.POST,
                new HttpEntity<>(budgetRequest, authorized(token).getHeaders()), String.class);
        assertEquals(HttpStatus.CONFLICT, duplicateBudget.getStatusCode());
        ResponseEntity<String> incomeBudget = http.exchange("/api/budgets", HttpMethod.POST,
                new HttpEntity<>(new BudgetDtos.Request(incomeCategory.id(), monthValue, new BigDecimal("50.00")),
                        authorized(token).getHeaders()), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, incomeBudget.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, http.exchange("/api/budgets?month=2026-13", HttpMethod.GET,
                authorized(token), String.class).getStatusCode());

        CategoryDtos.Response foreignCategory = exchange("/api/categories", HttpMethod.POST,
                new CategoryDtos.Request("Categoria de outra pessoa", CategoryType.EXPENSE), otherUserToken,
                CategoryDtos.Response.class).getBody();
        ResponseEntity<String> foreignBudget = http.exchange("/api/budgets", HttpMethod.POST,
                new HttpEntity<>(new BudgetDtos.Request(foreignCategory.id(), monthValue, new BigDecimal("50.00")),
                        authorized(token).getHeaders()), String.class);
        assertEquals(HttpStatus.NOT_FOUND, foreignBudget.getStatusCode());

        BudgetDtos.Response budget = createdBudget.getBody();
        ResponseEntity<BudgetDtos.Response> updatedBudget = exchange("/api/budgets/" + budget.id(), HttpMethod.PUT,
                new BudgetDtos.Request(expenseCategory.id(), monthValue, new BigDecimal("80.00")),
                token, BudgetDtos.Response.class);
        assertEquals(HttpStatus.OK, updatedBudget.getStatusCode());
        assertEquals(new BigDecimal("4.50"), updatedBudget.getBody().remainingAmount());
        assertEquals(HttpStatus.NOT_FOUND, http.exchange("/api/budgets/" + budget.id(), HttpMethod.DELETE,
                authorized(otherUserToken), String.class).getStatusCode());

        CategoryDtos.Response budgetOnlyCategory = exchange("/api/categories", HttpMethod.POST,
                new CategoryDtos.Request("Categoria com orçamento", CategoryType.EXPENSE), token,
                CategoryDtos.Response.class).getBody();
        BudgetDtos.Response standaloneBudget = exchange("/api/budgets", HttpMethod.POST,
                new BudgetDtos.Request(budgetOnlyCategory.id(), monthValue, new BigDecimal("25.00")),
                token, BudgetDtos.Response.class).getBody();
        ResponseEntity<String> categoryDeleteWhileBudgeted = http.exchange("/api/categories/" + budgetOnlyCategory.id(),
                HttpMethod.DELETE, authorized(token), String.class);
        assertEquals(HttpStatus.CONFLICT, categoryDeleteWhileBudgeted.getStatusCode());
        ResponseEntity<String> categoryTypeChangeWhileBudgeted = http.exchange(
                "/api/categories/" + budgetOnlyCategory.id(), HttpMethod.PUT,
                new HttpEntity<>(new CategoryDtos.Request("Categoria com orçamento", CategoryType.INCOME),
                        authorized(token).getHeaders()), String.class);
        assertEquals(HttpStatus.CONFLICT, categoryTypeChangeWhileBudgeted.getStatusCode());

        assertEquals(HttpStatus.NO_CONTENT, http.exchange("/api/budgets/" + standaloneBudget.id(), HttpMethod.DELETE,
                authorized(token), String.class).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, http.exchange("/api/categories/" + budgetOnlyCategory.id(),
                HttpMethod.DELETE, authorized(token), String.class).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, http.exchange("/api/budgets/" + budget.id(), HttpMethod.DELETE,
                authorized(token), String.class).getStatusCode());
    }

    private <T> ResponseEntity<T> exchange(String path, HttpMethod method, Object body, String token,
                                            Class<T> responseType) {
        return http.exchange(path, method, new HttpEntity<>(body, authorized(token).getHeaders()), responseType);
    }

    private HttpEntity<Void> authorized(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return new HttpEntity<>(null, headers);
    }
}
