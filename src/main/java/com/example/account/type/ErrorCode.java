package com.example.account.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND("사용자가 없습니다."),
    MAX_ACCOUNT_PER_USER_10("사용자 최대 계좌는 10개 입니다."),
    USER_ACCOUNT_UN_MATCH("사용자와 계좌의 소유주가 다릅니다."),
    ACCOUNT_ALREADY_UNREGISTERED("계좌가 이미 해지되었습니다."),

    BALANCE_NOT_EMPTY("잔액이 있는 계좌는 해지할 수 없습니다."),
    ACCOUNT_NOT_FOUND("계좌가 없습니다."),
    EXCEED_THAN_BALANCE("거래 금액이 잔액보다 큽니다."),
    MIN_MAX_AMOUNT_UNMATCH("거래금액이 너무 작거나 큽니다."),
    TRANSACTION_NOT_FOUND("해당 거래가 없습니다."),
    INVALID_REQUEST("잘못된 요청입니다."),
    TRANSACTION_ACCOUNT_UNMATCH("거래와 계좌가 일치하지 않습니다."),
    CANCEL_AMOUNT_TRANSACTION_AMOUNT_UNMATCH("거래금액과 취소금액이 일치하지 않습니다."),
    TOO_OLD_ORDER_FOR_CANCEL("거래한 지 1년이 지나 거래취소가 불가능합니다."),

    INTERNAL_SERVER_ERROR("내부 서버 오류가 발생했습니다."),
    ACCOUNT_TRANSACTION_LOCK("현재 해당 계좌는 사용 중입니다.");

    private final String description;
}
