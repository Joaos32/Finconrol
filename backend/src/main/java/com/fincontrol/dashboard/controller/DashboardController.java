package com.fincontrol.dashboard.controller;

import com.fincontrol.dashboard.dto.DashboardDtos;
import com.fincontrol.dashboard.service.DashboardService;
import com.fincontrol.shared.security.CurrentUser;
import com.fincontrol.transaction.dto.TransactionDtos;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {
    private final DashboardService service;
    private final CurrentUser currentUser;

    public DashboardController(DashboardService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/summary")
    public DashboardDtos.Summary summary() {
        return service.summary(currentUser.id());
    }

    @GetMapping("/expenses-by-category")
    public List<DashboardDtos.ExpenseCategory> expensesByCategory() {
        return service.expensesByCategory(currentUser.id());
    }

    @GetMapping("/monthly-evolution")
    public List<DashboardDtos.MonthlyPoint> monthlyEvolution() {
        return service.monthlyEvolution(currentUser.id());
    }

    @GetMapping("/recent-transactions")
    public List<TransactionDtos.Response> recentTransactions() {
        return service.recentTransactions(currentUser.id());
    }
}
