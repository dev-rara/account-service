package com.example.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.TransactionRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.AccountRepository;

import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("계좌 잔액을 사용하는 경우 - 계좌 잔액 사용 성공")
    void useBalanceTest() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));

        Account account = Account.builder()
                .accountUser(pobi)
                .accountNumber("1000000011")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .transactionType(TransactionType.USE)
                        .transactionResultType(TransactionResultType.SUCCESS)
                        .account(account)
                        .amount(1000L)
                        .balanceSnapshot(99000L)
                        .transactionId("transactionId")
                        .build());
        //when
        TransactionDto transactionDto = transactionService.useBalance
                (1L, "1000000011", 1000L);

        //then
        assertEquals(TransactionType.USE, transactionDto.getTransactionType());
        assertEquals(TransactionResultType.SUCCESS, transactionDto.getTransactionResultType());
        assertEquals(99000L, transactionDto.getBalanceSnapshot());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("해당 사용자가 없는 경우 - 잔액 사용 실패")
    void userNotFoundTest() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000011", 1000L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("계좌가 없는 경우 - 잔액 사용 실패")
    void transactionAccountNotFoundTest() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000011", 1000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("사용자와 계좌 소유주가 상이한 경우 - 잔액 사용 실패")
    void transactionAccountNotUserUnMatchTest() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();
        AccountUser harry = AccountUser.builder()
                .id(13L)
                .name("Harry")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(harry)
                        .accountNumber("1000000001")
                        .balance(0L)
                        .build()));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000011", 1000L));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, accountException.getErrorCode());
    }

    @Test
    @DisplayName("계좌가 이미 해지된 경우 - 잔액 사용 실패")
    void transactionAccountAlreadyUnregisteredTest() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(pobi)
                        .accountNumber("1000000012")
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .balance(0L)
                        .build()));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000011", 1000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, accountException.getErrorCode());
    }

    @Test
    @DisplayName("거래금액이 잔액보다 큰 경우 - 잔액 사용 실패")
    void exceedThanBalanceTest() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        Account account = Account.builder()
                .accountUser(pobi)
                .accountNumber("1000000012")
                .accountStatus(AccountStatus.IN_USE)
                .balance(100L)
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));


        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000011", 1000L));

        //then
        assertEquals(ErrorCode.EXCEED_THAN_BALANCE, accountException.getErrorCode());
    }

    @Test
    @DisplayName("실패 트랜잭션 저장 성공인 경우")
    void saveFailedUseTransaction() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        Account account = Account.builder()
                .accountUser(pobi)
                .accountNumber("1000000012")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .transactionType(TransactionType.USE)
                        .transactionResultType(TransactionResultType.SUCCESS)
                        .account(account)
                        .amount(1000L)
                        .balanceSnapshot(99000L)
                        .transactionId("transactionId")
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        transactionService.saveFailedUseTransaction("1000000011", 1000L);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(1000L, captor.getValue().getAmount());
        assertEquals(10000L, captor.getValue().getBalanceSnapshot());
        assertEquals(TransactionResultType.FAIL, captor.getValue().getTransactionResultType());
    }

    @Test
    @DisplayName("잔액을 사용하는 경우 - 계좌 잔액 사용 취소 성공")
    void cancelBalanceTest() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();

        Account account = Account.builder()
                .accountUser(pobi)
                .accountNumber("1000000011")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();

        Transaction transaction = Transaction.builder()
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.SUCCESS)
                .account(account)
                .amount(1000L)
                .balanceSnapshot(9900L)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .transactionType(TransactionType.USE)
                        .transactionResultType(TransactionResultType.SUCCESS)
                        .account(account)
                        .amount(1000L)
                        .balanceSnapshot(10000L)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        TransactionDto transactionDto =
                transactionService.cancelBalance(
                        "transactionId", "1000000011", 1000L);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(1000L, captor.getValue().getAmount());
        assertEquals(11000L, captor.getValue().getBalanceSnapshot());
        assertEquals(TransactionType.USE, transactionDto.getTransactionType());
        assertEquals(TransactionResultType.SUCCESS, transactionDto.getTransactionResultType());
        assertEquals(10000L, transactionDto.getBalanceSnapshot());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("해당 거래가 없는 경우 - 잔액 사용 취소 실패")
    void cancelTransactionNotFoundTest() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1000000011", 1000L));

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("계좌가 없는 경우 - 잔액 사용 실패")
    void cancelTransactionAccountNotFoundTest() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(Transaction.builder().build()));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1000000011", 1000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("계좌와 거래가 일치하지 않는 경우 - 잔액 사용 취소 실패")
    void cancelTransactionAccountNotUserUnMatchTest() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();
        Account account = Account.builder()
                .id(1L)
                .accountUser(pobi)
                .accountNumber("1000000010")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();
        Account accountUnMatch = Account.builder()
                .id(2L)
                .accountUser(pobi)
                .accountNumber("1000000011")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();
        Transaction transaction = Transaction.builder()
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.SUCCESS)
                .account(account)
                .amount(1000L)
                .balanceSnapshot(9000L)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(accountUnMatch));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1000000011", 1000L));

        //then
        assertEquals(ErrorCode.TRANSACTION_ACCOUNT_UNMATCH, accountException.getErrorCode());
    }

    @Test
    @DisplayName("거래금액과 거래 취소 금액이 상이한 경우 - 잔액 사용 취소 실패")
    void CancelAmountTransactionAmountTest() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();
        Account account = Account.builder()
                .id(1L)
                .accountUser(pobi)
                .accountNumber("1000000012")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();
        Transaction transaction = Transaction.builder()
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.SUCCESS)
                .account(account)
                .amount(1500L)
                .balanceSnapshot(9000L)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1000000011", 1000L));

        //then
        assertEquals(ErrorCode.CANCEL_AMOUNT_TRANSACTION_AMOUNT_UNMATCH, accountException.getErrorCode());
    }

    @Test
    @DisplayName("거래 기간이 1년이 넘은 경우 - 잔액 사용 취소 실패")
    void CancelAfterOneYearTransactionTest() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();
        Account account = Account.builder()
                .id(1L)
                .accountUser(pobi)
                .accountNumber("1000000011")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();
        Transaction transaction = Transaction.builder()
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.SUCCESS)
                .account(account)
                .amount(1000L)
                .balanceSnapshot(9000L)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance(
                        "transactionId", "1000000011", 1000L));

        //then
        assertEquals(ErrorCode.TOO_OLD_ORDER_FOR_CANCEL, accountException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 확인 - 잔액 사용 확인 성공")
    void confirmTransactionTest() {
        //given
        AccountUser pobi = AccountUser.builder()
                .id(12L)
                .name("pobi")
                .build();
        Account account = Account.builder()
                .id(1L)
                .accountUser(pobi)
                .accountNumber("1000000011")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();
        Transaction transaction = Transaction.builder()
                .transactionType(TransactionType.USE)
                .transactionResultType(TransactionResultType.SUCCESS)
                .account(account)
                .amount(1000L)
                .balanceSnapshot(9000L)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        //when
        TransactionDto transactionDto =
                transactionService.ConfirmTransaction(
                        "transactionId");

        //then
        assertEquals(TransactionType.USE, transactionDto.getTransactionType());
        assertEquals(TransactionResultType.SUCCESS, transactionDto.getTransactionResultType());
        assertEquals(1000L, transactionDto.getAmount());
        assertEquals("transactionId", transactionDto.getTransactionId());
    }

    @Test
    @DisplayName("해당 아이디에 맞는 거래가 없는 경우 - 잔액 사용 확인 실패")
    void confirmTransactionNotFoundTest() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class,
                () -> transactionService.ConfirmTransaction(
                        "transactionId0"));

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, accountException.getErrorCode());
    }
}