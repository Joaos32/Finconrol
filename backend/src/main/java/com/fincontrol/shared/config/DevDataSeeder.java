package com.fincontrol.shared.config;

import com.fincontrol.account.entity.AccountEntity;
import com.fincontrol.account.entity.AccountType;
import com.fincontrol.account.repository.AccountRepository;
import com.fincontrol.category.entity.CategoryEntity;
import com.fincontrol.category.entity.CategoryType;
import com.fincontrol.category.repository.CategoryRepository;
import com.fincontrol.transaction.entity.TransactionEntity;
import com.fincontrol.transaction.entity.TransactionType;
import com.fincontrol.transaction.repository.TransactionRepository;
import com.fincontrol.user.entity.UserEntity;
import com.fincontrol.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Profile("dev")
public class DevDataSeeder {
    @Bean
    @ConditionalOnProperty(name = "fincontrol.seed.enabled", havingValue = "true")
    CommandLineRunner seedDemoData(UserRepository users, AccountRepository accounts, CategoryRepository categories,
                                   TransactionRepository transactions, PasswordEncoder passwordEncoder,
                                   @org.springframework.beans.factory.annotation.Value("${fincontrol.seed.demo-password}") String demoPassword) {
        return args -> {
            if (users.existsByEmailIgnoreCase("demo@fincontrol.dev")) {
                return;
            }
            UserEntity user = users.save(new UserEntity("Usuário Demonstração", "demo@fincontrol.dev",
                    passwordEncoder.encode(demoPassword)));
            AccountEntity checking = accounts.save(new AccountEntity(user, "Conta principal", AccountType.CHECKING,
                    new BigDecimal("4200.00")));
            AccountEntity savings = accounts.save(new AccountEntity(user, "Reserva", AccountType.SAVINGS,
                    new BigDecimal("2500.00")));

            List<CategoryEntity> categoryRows = categories.saveAll(List.of(
                    new CategoryEntity(user, "Salário", CategoryType.INCOME),
                    new CategoryEntity(user, "Freelance", CategoryType.INCOME),
                    new CategoryEntity(user, "Alimentação", CategoryType.EXPENSE),
                    new CategoryEntity(user, "Moradia", CategoryType.EXPENSE),
                    new CategoryEntity(user, "Transporte", CategoryType.EXPENSE),
                    new CategoryEntity(user, "Saúde", CategoryType.EXPENSE),
                    new CategoryEntity(user, "Lazer", CategoryType.EXPENSE)));

            List<TransactionEntity> rows = new ArrayList<>();
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            for (int index = 0; index < 15; index++) {
                boolean income = index == 0 || index == 7 || index == 13;
                TransactionType type = income ? TransactionType.INCOME : TransactionType.EXPENSE;
                CategoryEntity category = categoryRows.get(income ? (index == 13 ? 1 : 0) : 2 + (index % 5));
                AccountEntity account = index % 4 == 0 ? savings : checking;
                String[] descriptions = {"Mercado", "Aluguel", "Transporte", "Farmácia", "Cinema", "Salário", "Café"};
                BigDecimal amount = income
                        ? new BigDecimal(index == 0 ? "5200.00" : "450.00")
                        : new BigDecimal(new String[]{"84.50", "130.00", "42.90", "65.00", "118.00"}[index % 5]);
                rows.add(new TransactionEntity(user, account, category, income ? (index == 0 ? "Salário" : "Renda extra")
                        : descriptions[index % descriptions.length], amount, type, today.minus(index, ChronoUnit.DAYS)));
            }
            transactions.saveAll(rows);
        };
    }
}
