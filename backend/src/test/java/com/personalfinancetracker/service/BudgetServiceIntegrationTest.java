package com.personalfinancetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.personalfinancetracker.dto.BudgetRequest;
import com.personalfinancetracker.dto.BudgetResponse;
import com.personalfinancetracker.entity.AppUser;
import com.personalfinancetracker.entity.BudgetStatus;
import com.personalfinancetracker.entity.Transaction;
import com.personalfinancetracker.entity.TransactionType;
import com.personalfinancetracker.exception.ApiException;
import com.personalfinancetracker.repository.AppUserRepository;
import com.personalfinancetracker.repository.TransactionRepository;
import com.personalfinancetracker.security.AppUserPrincipal;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:finance-budget-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.cookie-secure=false"
})
@AutoConfigureMockMvc
@Transactional
class BudgetServiceIntegrationTest {
    @Autowired
    private BudgetService budgetService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private AppUser owner;
    private AppUser anotherUser;

    @BeforeEach
    void setUp() {
        owner = userRepository.saveAndFlush(verifiedUser("owner@example.test"));
        anotherUser = userRepository.saveAndFlush(verifiedUser("another@example.test"));
    }

    @Test
    void calculatesOnlySavedExpensesForOwnerCategoryAndSelectedMonthAndRefreshesLive() {
        BudgetResponse created = budgetService.createBudget(owner.getId(), budget(2026, 10, " Food ", "100.00"));
        BudgetResponse transportBudget = budgetService.createBudget(
                owner.getId(), budget(2026, 10, "Transport", "200.00"));
        Transaction expense = saveTransaction(owner, "Food", "20.00", TransactionType.EXPENSE, "2026-10-01");
        saveTransaction(owner, " food ", "10.00", TransactionType.EXPENSE, "2026-10-31");
        saveTransaction(owner, "Food", "80.00", TransactionType.INCOME, "2026-10-10");
        saveTransaction(owner, "Transport", "70.00", TransactionType.EXPENSE, "2026-10-10");
        saveTransaction(owner, "Food", "90.00", TransactionType.EXPENSE, "2026-11-01");
        saveTransaction(anotherUser, "Food", "60.00", TransactionType.EXPENSE, "2026-10-15");

        BudgetResponse response = budgetService.getBudget(owner.getId(), created.id());
        assertMoney("30.00", response.actualSpending());
        assertMoney("70.00", response.remainingAmount());
        assertMoney("30.00", response.percentageUsed());
        assertEquals(BudgetStatus.NORMAL, response.status());
        assertMoney("70.00", budgetService.getBudget(owner.getId(), transportBudget.id()).actualSpending());

        expense.updateDetails("Food updated", new BigDecimal("50.00"), TransactionType.EXPENSE, "Transport",
                LocalDate.parse("2026-10-01"));
        transactionRepository.saveAndFlush(expense);
        assertMoney("10.00", budgetService.getBudget(owner.getId(), created.id()).actualSpending());
        assertMoney("120.00", budgetService.getBudget(owner.getId(), transportBudget.id()).actualSpending());

        transactionRepository.delete(expense);
        transactionRepository.flush();
        assertMoney("10.00", budgetService.getBudget(owner.getId(), created.id()).actualSpending());
        assertMoney("70.00", budgetService.getBudget(owner.getId(), transportBudget.id()).actualSpending());
    }

    @Test
    void categorizesStatusAtExactPercentageBoundaries() {
        assertBudgetStatus("79.99", BudgetStatus.NORMAL, "79.99");
        assertBudgetStatus("80.00", BudgetStatus.WARNING, "80.00");
        assertBudgetStatus("99.99", BudgetStatus.WARNING, "99.99");
        assertBudgetStatus("100.00", BudgetStatus.EXCEEDED, "100.00");
        assertBudgetStatus("125.00", BudgetStatus.EXCEEDED, "125.00");
    }

    @Test
    void rejectsInvalidLimitsAndDuplicateCaseInsensitiveCategoryBudgets() {
        assertThrows(ApiException.class,
                () -> budgetService.createBudget(owner.getId(), budget(2026, 10, "Food", "0.00")));
        assertThrows(ApiException.class,
                () -> budgetService.createBudget(owner.getId(), budget(2026, 10, "Food", "-1.00")));
        assertThrows(ApiException.class,
                () -> budgetService.createBudget(owner.getId(), budget(2026, 10, "Food", "1.001")));
        assertThrows(ApiException.class,
                () -> budgetService.createBudget(owner.getId(),
                        budget(2026, 10, "Food", "100000000000000000.00")));

        budgetService.createBudget(owner.getId(), budget(2026, 10, "Food", "100.00"));
        ApiException duplicate = assertThrows(ApiException.class,
                () -> budgetService.createBudget(owner.getId(), budget(2026, 10, " food ", "200.00")));
        assertEquals("BUDGET_DUPLICATE", duplicate.getCode());

        BudgetResponse maximum = budgetService.createBudget(owner.getId(),
                budget(2026, 10, "Maximum", "99999999999999999.99"));
        assertEquals("99999999999999999.99", maximum.budgetLimit());

        BudgetResponse nextMonth = budgetService.createBudget(owner.getId(), budget(2026, 11, "Food", "100.00"));
        assertMoney("0.00", nextMonth.actualSpending());
    }

    @Test
    void supportsUpdateDeleteAndHidesBudgetsFromOtherUsers() {
        BudgetResponse created = budgetService.createBudget(owner.getId(), budget(2026, 10, "Food", "100.00"));
        assertThrows(ApiException.class, () -> budgetService.getBudget(anotherUser.getId(), created.id()));

        BudgetResponse updated = budgetService.updateBudget(owner.getId(), created.id(),
                budget(2026, 11, "Housing", "250.00"));
        assertEquals(11, updated.month());
        assertEquals("Housing", updated.category());
        assertMoney("250.00", updated.budgetLimit());

        budgetService.deleteBudget(owner.getId(), created.id());
        assertEquals(0, budgetService.listBudgets(owner.getId(), 2026, 11).size());
    }

    @Test
    void rejectsInvalidPeriodAndSecuresHttpEndpoints() throws Exception {
        assertThrows(ApiException.class, () -> budgetService.listBudgets(owner.getId(), 2026, 13));
        assertThrows(ApiException.class, () -> budgetService.listBudgets(owner.getId(), 10000, 1));

        mockMvc.perform(get("/api/budgets").param("year", "2026").param("month", "13")
                .with(user(new AppUserPrincipal(owner))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUDGET_PERIOD_INVALID"));

        mockMvc.perform(get("/api/budgets").param("year", "twenty26").param("month", "10")
                .with(user(new AppUserPrincipal(owner))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BUDGET_PERIOD_INVALID"));

        var createResult = mockMvc.perform(post("/api/budgets")
                .with(user(new AppUserPrincipal(owner)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"year\":2026,\"month\":10,\"category\":\"Food\",\"amount\":\"100.00\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.actualSpending").value("0.00"))
                .andExpect(jsonPath("$.status").value("NORMAL"))
                .andReturn();

        long budgetId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();
        mockMvc.perform(get("/api/budgets/{id}", budgetId)
                .with(user(new AppUserPrincipal(anotherUser))))
                .andExpect(status().isNotFound());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/budgets/{id}", budgetId)
                .with(user(new AppUserPrincipal(anotherUser)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(budget(2026, 10, "Food", "200.00"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/budgets/{id}", budgetId)
                .with(user(new AppUserPrincipal(anotherUser)))
                .with(csrf()))
                .andExpect(status().isNotFound());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/budgets/{id}", budgetId)
                .with(user(new AppUserPrincipal(owner)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(budget(2026, 10, "Food", "200.00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetLimit").value("200.00"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/budgets/{id}", budgetId)
                .with(user(new AppUserPrincipal(owner)))
                .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/budgets").param("year", "2026").param("month", "10"))
                .andExpect(status().isUnauthorized());
    }

    private void assertBudgetStatus(String expense, BudgetStatus expectedStatus, String expectedPercentage) {
        String category = "Boundary " + expense;
        BudgetResponse budget = budgetService.createBudget(owner.getId(), budget(2026, 10, category, "10000.00"));
        BigDecimal expenseAmount = new BigDecimal(expense).multiply(new BigDecimal("100"));
        saveTransaction(owner, category, expenseAmount.toPlainString(), TransactionType.EXPENSE, "2026-10-15");

        BudgetResponse result = budgetService.getBudget(owner.getId(), budget.id());
        assertEquals(expectedStatus, result.status());
        assertMoney(expectedPercentage, result.percentageUsed());
    }

    private Transaction saveTransaction(
            AppUser user,
            String category,
            String amount,
            TransactionType type,
            String date) {
        return transactionRepository.saveAndFlush(new Transaction(
                user, "Test transaction", new BigDecimal(amount), type, category, LocalDate.parse(date)));
    }

    private AppUser verifiedUser(String email) {
        AppUser user = new AppUser("Budget Test", email, "unused-password-hash");
        user.verifyEmail();
        return user;
    }

    private BudgetRequest budget(int year, int month, String category, String amount) {
        return new BudgetRequest(year, month, category, new BigDecimal(amount));
    }

    private void assertMoney(String expected, String actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(new BigDecimal(actual)));
    }
}