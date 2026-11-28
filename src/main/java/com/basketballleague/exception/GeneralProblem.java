package com.basketballleague.exception;

import com.basketballleague.domain.ErrorCode;
import com.basketballleague.dto.FieldViolation;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * Problems that do not come from a business rule. The constant name is sent as the {@code code} member.
 */
enum GeneralProblem {
    VALIDATION_FAILED(Response.Status.BAD_REQUEST, "İstekte geçersiz alanlar var."),
    MALFORMED_REQUEST(Response.Status.BAD_REQUEST, "İstek okunamadı; gönderilen verinin biçimini kontrol edin."),
    /** Same code and text as the version check in the services, so the client sees one message (ADR-0004). */
    CONCURRENT_UPDATE(Response.Status.CONFLICT, ProblemCatalog.detailFor(ErrorCode.CONCURRENT_UPDATE)),
    DATA_CONFLICT(Response.Status.CONFLICT,
            "İstek mevcut verilerle çakışıyor: aynı kayıt zaten var ya da başka kayıtlar bu kayda bağlı."),
    INTERNAL_ERROR(Response.Status.INTERNAL_SERVER_ERROR, "Beklenmeyen bir hata oluştu. Lütfen daha sonra deneyin.");

    private final Response.Status status;
    private final String detail;

    GeneralProblem(Response.Status status, String detail) {
        this.status = status;
        this.detail = detail;
    }

    Response toResponse() {
        return toResponse(null);
    }

    Response toResponse(List<FieldViolation> errors) {
        return ProblemResponses.of(status, name(), detail, errors);
    }
}
