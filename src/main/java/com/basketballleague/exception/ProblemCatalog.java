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
            case INVALID_TEAM -> "Takım adı ve şehri boş olamaz.";
            case INVALID_PLAYER ->
                    "Oyuncunun adı, soyadı, takımı ve pozisyonu zorunludur; forma numarası 0-99 arasında olmalıdır.";
            case TEAM_NOT_FOUND -> "İstenen takım bulunamadı.";
            case TEAM_NAME_TAKEN -> "Bu adda bir takım zaten var.";
            case JERSEY_NUMBER_TAKEN -> "Bu forma numarası takımda başka bir oyuncu tarafından kullanılıyor.";
            case INVALID_GAME_STATUS_TRANSITION -> "Maç mevcut durumundan istenen duruma geçirilemez.";
            case INVALID_SEASON -> "Sezon adı boş olamaz ve başlangıç tarihi zorunludur.";
            case SEASON_ALREADY_STARTED -> "Sezon başladı; takım listesi ve fikstür artık değiştirilemez.";
            case TEAM_ALREADY_IN_SEASON -> "Bu takım sezona zaten eklenmiş.";
            case SEASON_FULL -> "Sezondaki takım sayısı üst sınıra ulaştı.";
            case INVALID_GAME -> "Maç için sezon, tur ve birbirinden farklı iki takım gerekir.";
            case SEASON_NOT_FOUND -> "İstenen sezon bulunamadı.";
            case GAME_NOT_FOUND -> "İstenen maç bulunamadı.";
            case SEASON_NAME_TAKEN -> "Bu adda bir sezon zaten var.";
            case INVALID_QUARTER_SCORE ->
                    "Çeyrek skorları 1'den başlayarak boşluksuz artmalı, sayılar negatif olmamalı ve eşitlik "
                            + "yoksa uzatma girilmemelidir.";
            case OVERTIME_REQUIRED ->
                    "Berabere biten maç tamamlanamaz; eşitlik bozulana kadar uzatma çeyreği girilmelidir.";
            case PLAYER_NOT_IN_GAME -> "Oyuncu bu maçta oynayan takımlardan birinin kadrosunda değil.";
            case SCORE_MISMATCH -> "Çeyrek toplamları veya oyuncu sayıları maç skoruyla uyuşmuyor.";
            case INVALID_PLAYER_POINTS ->
                    "Her oyuncu sayısı kaydında oyuncu ve takım zorunludur, sayı negatif olamaz ve bir oyuncu "
                            + "listede yalnızca bir kez yer alır.";
            case CONCURRENT_UPDATE ->
                    "Maç sizden önce başka bir işlem tarafından güncellendi; sayfayı yenileyip tekrar deneyin.";
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
