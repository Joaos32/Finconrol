package com.fincontrol;

import com.fincontrol.account.dto.AccountDtos;
import com.fincontrol.account.entity.AccountType;
import com.fincontrol.auth.dto.AuthDtos;
import com.fincontrol.category.dto.CategoryDtos;
import com.fincontrol.category.entity.CategoryType;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
    void registersLogsInAndCompletesPersonalFinanceFlow() {
        assertEquals(HttpStatus.UNAUTHORIZED, http.getForEntity("/api/accounts", Object.class).getStatusCode());

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
