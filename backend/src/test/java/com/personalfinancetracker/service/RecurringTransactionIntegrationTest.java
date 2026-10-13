package com.personalfinancetracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.personalfinancetracker.dto.BudgetRequest;
import com.personalfinancetracker.dto.BudgetResponse;
import com.personalfinancetracker.dto.RecurringTransactionRequest;
import com.personalfinancetracker.dto.RecurringTransactionResponse;
import com.personalfinancetracker.entity.AppUser;
import com.personalfinancetracker.entity.RecurringExecutionStatus;
import com.personalfinancetracker.entity.RecurringStatus;
import com.personalfinancetracker.entity.Transaction;
import com.personalfinancetracker.entity.TransactionType;
import com.personalfinancetracker.exception.ApiException;
import com.personalfinancetracker.repository.AppUserRepository;
import com.personalfinancetracker.repository.RecurringExecutionRepository;
import com.personalfinancetracker.repository.RecurringTransactionRepository;
import com.personalfinancetracker.repository.TransactionRepository;
import com.personalfinancetracker.security.AppUserPrincipal;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:finance-recurring-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.cookie-secure=false",
        "app.recurring.enabled=false"
})
@AutoConfigureMockMvc
@Transactional
class RecurringTransactionIntegrationTest {
    @Autowired
    private RecurringTransactionService recurringService;

    @Autowired
    private RecurringTransactionScheduler scheduler;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private RecurringTransactionRepository recurringRepository;

    @Autowired
    private RecurringExecutionRepository executionRepository;

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
        String suffix = UUID.randomUUID().toString();
        owner = userRepository.saveAndFlush(verifiedUser("owner-" + suffix + "@example.test"));
        anotherUser = userRepository.saveAndFlush(verifiedUser("other-" + suffix + "@example.test"));
    }

    @Test
    void createsValidatesListsAndMutatesTemplatesThroughAuthenticatedApi() throws Exception {
        mockMvc.perform(post("/api/recurring-transactions")
                .with(user(new AppUserPrincipal(owner)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest(request("Monthly rent", "1200.00", "expense", "Housing",
                        "MONTHLY", "2026-10-31", null, "home"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nextScheduledDate").value("2026-10-31"))
                .andExpect(jsonPath("$.frequency").value("MONTHLY"))
                .andExpect(jsonPath("$.amount").value("1200.00"));

        MvcResult createResult = mockMvc.perform(post("/api/recurring-transactions")
                .with(user(new AppUserPrincipal(owner)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest(request("Salary", "5000.00", "income", "Salary",
                        "MONTHLY", "2026-10-01", null, null))))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/recurring-transactions/{id}", id)
                .with(user(new AppUserPrincipal(anotherUser))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/recurring-transactions")
                .with(user(new AppUserPrincipal(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        mockMvc.perform(put("/api/recurring-transactions/{id}", id)
                .with(user(new AppUserPrincipal(owner)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest(request("Salary revised", "5500.00", "income", "Salary",
                        "MONTHLY", "2026-10-01", null, "updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Salary revised"))
                .andExpect(jsonPath("$.amount").value("5500.00"));

        mockMvc.perform(patch("/api/recurring-transactions/{id}/pause", id)
                .with(user(new AppUserPrincipal(owner)))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));

        mockMvc.perform(patch("/api/recurring-transactions/{id}/resume", id)
                .with(user(new AppUserPrincipal(owner)))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(delete("/api/recurring-transactions/{id}", id)
                .with(user(new AppUserPrincipal(anotherUser)))
                .with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/recurring-transactions/{id}", id)
                .with(user(new AppUserPrincipal(owner)))
                .with(csrf()))
                .andExpect(status().isNoContent());
        assertEquals(RecurringStatus.DELETED,
                recurringRepository.findById(id).orElseThrow().getStatus());
    }

    @Test
    void rejectsInvalidAmountFrequencyAndDateRange() {
        assertThrows(ApiException.class,
                () -> recurringService.create(owner.getId(), request("Rent", "0.00", "expense", "Housing",
                        "MONTHLY", "2026-10-01", null, null)));
        assertThrows(ApiException.class,
                () -> recurringService.create(owner.getId(), request("Rent", "10.001", "expense", "Housing",
                        "MONTHLY", "2026-10-01", null, null)));
        assertThrows(ApiException.class,
                () -> recurringService.create(owner.getId(), request("Rent", "10.00", "expense", "Housing",
                        "QUARTERLY", "2026-10-01", null, null)));
        assertThrows(ApiException.class,
                () -> recurringService.create(owner.getId(), request("Rent", "10.00", "expense", "Housing",
                        "MONTHLY", "2026-10-10", "2026-10-09", null)));
        assertThrows(ApiException.class,
                () -> recurringService.create(owner.getId(), request("Rent", "10.00", "other", "Housing",
                        "MONTHLY", "2026-10-01", null, null)));
    }

    @Test
    void processesBacklogOneOccurrencePerTickAndFeedsBudgetsAndTransactions() throws Exception {
        RecurringTransactionResponse recurring = recurringService.create(owner.getId(),
                request("Groceries", "100.00", "expense", "Food", "MONTHLY", "2026-01-30", null, null));
        budgetService.createBudget(owner.getId(), new BudgetRequest(2026, 1, "Food", new BigDecimal("500.00")));

        assertEquals(1, scheduler.processDueOccurrences(LocalDate.parse("2026-03-31")));
        assertEquals(1, scheduler.processDueOccurrences(LocalDate.parse("2026-03-31")));
        assertEquals(1, scheduler.processDueOccurrences(LocalDate.parse("2026-03-31")));
        assertEquals(0, scheduler.processDueOccurrences(LocalDate.parse("2026-03-31")));

        assertEquals(3, executionRepository.countByRecurringTransactionId(recurring.id()));
        var transactions = transactionRepository.findByUserId(owner.getId(), PageRequest.of(
                0, 10, Sort.by(Sort.Direction.ASC, "date"))).getContent();
        assertEquals(3, transactions.size());
        assertEquals(LocalDate.parse("2026-01-30"), transactions.get(0).getDate());
        assertEquals(LocalDate.parse("2026-02-28"), transactions.get(1).getDate());
        assertEquals(LocalDate.parse("2026-03-30"), transactions.get(2).getDate());
        assertEquals(owner.getId(), transactions.get(0).getUser().getId());
        assertEquals(recurring.id(), transactions.get(0).getRecurringExecution()
                .getRecurringTransaction().getId());
        assertEquals(RecurringExecutionStatus.COMPLETED,
                transactions.get(0).getRecurringExecution().getStatus());
        mockMvc.perform(get("/api/transactions").with(user(new AppUserPrincipal(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3));
        mockMvc.perform(get("/api/transactions").with(user(new AppUserPrincipal(anotherUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        BudgetResponse januaryBudget = budgetService.listBudgets(owner.getId(), 2026, 1).getFirst();
        assertEquals(0, new BigDecimal("100.00").compareTo(new BigDecimal(januaryBudget.actualSpending())));
        assertEquals(RecurringStatus.ACTIVE, recurringRepository.findById(recurring.id()).orElseThrow().getStatus());
        assertEquals(LocalDate.parse("2026-04-30"),
                recurringRepository.findById(recurring.id()).orElseThrow().getNextScheduledDate());
    }

    @Test
    void pauseFreezesBacklogAndEndDateIsInclusiveWithoutDeletingHistory() {
        RecurringTransactionResponse recurring = recurringService.create(owner.getId(),
                request("Membership", "25.00", "expense", "Lifestyle", "DAILY",
                        "2026-10-10", "2026-10-11", null));
        recurringService.pause(owner.getId(), recurring.id());
        assertEquals(0, scheduler.processDueOccurrences(LocalDate.parse("2026-10-12")));

        recurringService.resume(owner.getId(), recurring.id());
        assertEquals(1, scheduler.processDueOccurrences(LocalDate.parse("2026-10-12")));
        assertEquals(1, scheduler.processDueOccurrences(LocalDate.parse("2026-10-12")));
        assertEquals(0, scheduler.processDueOccurrences(LocalDate.parse("2026-10-12")));
        assertEquals(RecurringStatus.ENDED, recurringRepository.findById(recurring.id()).orElseThrow().getStatus());

        Transaction generated = transactionRepository.findByUserId(owner.getId(), PageRequest.of(0, 10))
                .getContent().getFirst();
        recurringService.delete(owner.getId(), recurring.id());
        assertEquals(2, transactionRepository.count());
        assertEquals(2, executionRepository.countByRecurringTransactionId(recurring.id()));
        assertEquals(generated.getId(), transactionRepository.findById(generated.getId()).orElseThrow().getId());
    }

    @Test
    void generatedIncomeAppearsAsAnOwnedTransaction() throws Exception {
        var recurring = recurringService.create(owner.getId(),
                request("Salary", "1250.50", "income", "Salary", "DAILY", "2026-10-13", null, null));

        assertEquals(1, scheduler.processDueOccurrences(LocalDate.parse("2026-10-13")));
        mockMvc.perform(get("/api/transactions").with(user(new AppUserPrincipal(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value("income"))
                .andExpect(jsonPath("$.content[0].category").value("Salary"));
        assertEquals(1, executionRepository.countByRecurringTransactionId(recurring.id()));
    }

    @Test
    void processesNoMoreThanOneHundredTemplatesInOneTick() {
        for (int index = 0; index < 101; index++) {
            recurringService.create(owner.getId(), request(
                    "Due template " + index,
                    "1.00",
                    "expense",
                    "Other",
                    "DAILY",
                    "2026-10-13",
                    null,
                    null));
        }

        assertEquals(100, scheduler.processDueOccurrences(LocalDate.parse("2026-10-13")));
        assertEquals(1, recurringRepository.findDueIds(
                LocalDate.parse("2026-10-13"), PageRequest.of(0, 100)).size());
        assertEquals(100, transactionRepository.count());
    }

    private AppUser verifiedUser(String email) {
        AppUser user = new AppUser("Recurring Test", email, "unused-password-hash");
        user.verifyEmail();
        return user;
    }

    private RecurringTransactionRequest request(
            String description,
            String amount,
            String type,
            String category,
            String frequency,
            String startDate,
            String endDate,
            String notes) {
        return new RecurringTransactionRequest(
                description,
                new BigDecimal(amount),
                type,
                category,
                frequency,
                LocalDate.parse(startDate),
                endDate == null ? null : LocalDate.parse(endDate),
                notes);
    }

    private String jsonRequest(RecurringTransactionRequest request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }
}