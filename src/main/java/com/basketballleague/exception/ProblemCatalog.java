package com.basketballleague.exception;

import com.basketballleague.domain.ErrorCode;

/**
 * Client-facing Turkish texts. Exception messages are never sent to the client; they go to the server log.
 */
final class ProblemCatalog {

    private static final int NOT_FOUND = 404;
    private static final int METHOD_NOT_ALLOWED = 405;
    private static final int NOT_ACCEPTABLE = 406;
    private static final int UNSUPPORTED_MEDIA_TYPE = 415;

    private ProblemCatalog() {
    }

    // No default branch: a new ErrorCode without a text stops the build.
    static String detailFor(ErrorCode code) {
        return switch (code) {
            case INVALID_FIXTURE_INPUT -> "Fikstür için birbirinden farklı en az iki takım gerekir.";
        };
    }

    static String detailForHttpStatus(int status) {
        return switch (status) {
            case NOT_FOUND -> "İstenen adres bulunamadı.";
            case METHOD_NOT_ALLOWED -> "Bu adres, kullanılan HTTP metodunu desteklemiyor.";
            case NOT_ACCEPTABLE -> "İstenen yanıt biçimi desteklenmiyor.";
            case UNSUPPORTED_MEDIA_TYPE -> "Gönderilen içerik türü desteklenmiyor.";
            default -> "İstek işlenemedi.";
        };
    }
}
