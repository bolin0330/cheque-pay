package com.chequepay;

import com.chequepay.dto.ChequeRequest;
import com.chequepay.entity.Account;
import com.chequepay.entity.Cheque;
import com.chequepay.entity.User;
import com.chequepay.repository.AccountRepository;
import com.chequepay.repository.ChequeRepository;
import com.chequepay.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class IssueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ChequeRepository chequeRepository;

    @BeforeEach
    void setUp() {
        chequeRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        User payer = User.builder()
                .username("hyunjin1004")
                .realname("Hyunjin Hwang")
                .password("pass123")
                .email("hyunjin@example.com")
                .phoneNumber("0912345678")
                .role("USER")
                .build();
        User payee = User.builder()
                .username("isa8c")
                .realname("Isa Lee")
                .password("word123")
                .email("isa@example.com")
                .phoneNumber("0987654321")
                .role("USER")
                .build();
        userRepository.saveAll(List.of(payer, payee));

        Account payerAccount = Account.builder().username("hyunjin1004").realname("Hyunjin Hwang").balance(BigDecimal.valueOf(5000)).build();
        accountRepository.save(payerAccount);
    }

    @Test
    @WithMockUser(username = "hyunjin1004")
    void issueSuccess() throws Exception {
        ChequeRequest request = new ChequeRequest();
        request.setPayeeUsername("isa8c");
        request.setPayeeRealname("Isa Lee");
        request.setAmount(BigDecimal.valueOf(2000));
        request.setExpiryDate(LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/cheques")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payerUsername").value("hyunjin1004"))
                .andExpect(jsonPath("$.payeeUsername").value("isa8c"))
                .andExpect(jsonPath("$.amount").value(2000));

        Cheque cheque = chequeRepository.findAll().get(0);
        assertEquals("hyunjin1004", cheque.getPayerUsername());
        assertEquals("isa8c", cheque.getPayeeUsername());
        assertEquals(0, cheque.getAmount().compareTo(BigDecimal.valueOf(2000)));
    }

    @Test
    @WithMockUser(username = "hyunjin1004")
    void issuePayeeNotFound() throws Exception {
        ChequeRequest request = new ChequeRequest();
        request.setPayeeUsername("nonexistent");
        request.setPayeeRealname("Nobody");
        request.setAmount(BigDecimal.valueOf(2000));
        request.setExpiryDate(LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/cheques")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Payee not found"));
    }

    @Test
    @WithMockUser(username = "hyunjin1004")
    void issuePayeeNameNotMatch() throws Exception {
        ChequeRequest request = new ChequeRequest();
        request.setPayeeUsername("isa8c");
        request.setPayeeRealname("Nobody");
        request.setAmount(BigDecimal.valueOf(2000));
        request.setExpiryDate(LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/cheques")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Payee real name does not match the account"));
    }

    @Test
    @WithMockUser(username = "hyunjin1004")
    void issueInsufficientBalance() throws Exception {
        ChequeRequest request = new ChequeRequest();
        request.setPayeeUsername("isa8c");
        request.setPayeeRealname("Isa Lee");
        request.setAmount(BigDecimal.valueOf(7777));
        request.setExpiryDate(LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/cheques")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cheque amount exceeds payer's account balance"));
    }

    @Test
    @WithMockUser(username = "hyunjin1004")
    void issueWrongExpiryDate() throws Exception {
        ChequeRequest request = new ChequeRequest();
        request.setPayeeUsername("isa8c");
        request.setPayeeRealname("Isa Lee");
        request.setAmount(BigDecimal.valueOf(2000));
        request.setExpiryDate(LocalDateTime.now().minusDays(1));

        mockMvc.perform(post("/cheques")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cheque expiry date cannot be earlier than now"));
    }
}
