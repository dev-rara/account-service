package com.example.account.controller;

import com.example.account.aop.AccountLock;
import com.example.account.dto.CancelBalance;
import com.example.account.dto.TransactionResponse;
import com.example.account.dto.UseBalance;
import com.example.account.exception.AccountException;
import com.example.account.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping("/transaction/use")
    @AccountLock
    public UseBalance.Response useBalance(
            @RequestBody @Valid UseBalance.Request request)
            throws InterruptedException {
            try {
                Thread.sleep(5000L);
                return UseBalance.Response.from(transactionService.useBalance(
                       request.getUserId(), request.getAccountNumber(), request.getAmount()));
            } catch (AccountException e) {
                transactionService.saveFailedUseTransaction(
                        request.getAccountNumber(), request.getAmount());
            throw e;
        }
    }

    @PostMapping("/transaction/cancel")
    @AccountLock
    public CancelBalance.Response cancelBalance(
            @RequestBody @Valid CancelBalance.Request request) {
        try {
            return CancelBalance.Response.from(transactionService.cancelBalance(
                    request.getTransactionId(), request.getAccountNumber(), request.getAmount()));
        } catch (AccountException e) {
            transactionService.saveFailedCancelTransaction(
                    request.getAccountNumber(), request.getAmount());
            throw e;
        }
    }

    @GetMapping("/transaction/{transactionId}")
    public TransactionResponse confirmTransaction(
            @PathVariable String transactionId) {
        return TransactionResponse.from(
                transactionService.ConfirmTransaction(transactionId));
    }
}
