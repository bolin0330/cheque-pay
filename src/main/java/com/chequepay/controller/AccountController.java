package com.chequepay.controller;

import com.chequepay.dto.BalanceResponse;
import com.chequepay.entity.Account;
import com.chequepay.repository.AccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account")
public class AccountController {

    private final AccountRepository accountRepository;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @GetMapping("/balance")
    public BalanceResponse getBalance(Authentication authentication) {
        String username = authentication.getName();

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        return new BalanceResponse(
                account.getBalance(),
                account.getUsername(),
                account.getRealname()
        );
    }
}
