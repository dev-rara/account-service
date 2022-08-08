package com.example.account.dto;

import com.example.account.type.ErrorCode;
import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private ErrorCode errorCode;
    private String errorDescription;
    private LocalDateTime currentTime;
}
