<#
.SYNOPSIS
Çalışan uygulamaya demo sezonu ekler: sezon, sezonun takımları ve fikstür.

.DESCRIPTION
Veri REST API üzerinden girilir; migration'lara demo verisi eklenmez.
Tekrar çalıştırmak güvenlidir: var olan sezon kullanılır, üretilmiş fikstür yeniden üretilmez.

.EXAMPLE
.\scripts\demo-sezon.ps1
.\scripts\demo-sezon.ps1 -TeamCount 8
#>
param(
    [ValidateRange(2, 20)]
    [int]$TeamCount = 6,
    [string]$ApiUrl = $(if ($env:API_URL) { $env:API_URL } else { 'http://localhost:8080/basketball-league/api' }),
    [string]$SeasonName = '2026-27 Demo Sezonu',
    [string]$SeasonStart = '2026-10-01'
)

$ErrorActionPreference = 'Stop'

# İsteği gönderir; başarılıysa Data, hatalıysa Status ve Problem Details kodunu (Code) döner.
function Invoke-Api {
    param([string]$Method, [string]$Path, $Body = $null)

    $request = @{ Method = $Method; Uri = "$ApiUrl$Path"; Headers = @{ Accept = 'application/json' } }
    if ($null -ne $Body) {
        $request.ContentType = 'application/json; charset=utf-8'
        $request.Body = [System.Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Compress))
    }
    try {
        return [pscustomobject]@{ Ok = $true; Status = 200; Code = $null; Data = (Invoke-RestMethod @request) }
    } catch {
        $status = 0
        if ($null -ne $_.Exception.Response) { $status = [int]$_.Exception.Response.StatusCode }
        $code = $null
        if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
            try { $code = ($_.ErrorDetails.Message | ConvertFrom-Json).code } catch { $code = $null }
        }
        return [pscustomobject]@{ Ok = $false; Status = $status; Code = $code; Data = $null }
    }
}

function Stop-WithError([string]$Message) {
    Write-Host "Hata: $Message" -ForegroundColor Red
    exit 1
}

$ping = Invoke-Api GET '/ping'
if (-not $ping.Ok) {
    Stop-WithError "API'ye ulaşılamadı ($ApiUrl/ping → $($ping.Status)). Uygulama Payara'da çalışıyor mu?"
}

# 1. Sezon
$created = Invoke-Api POST '/seasons' @{ name = $SeasonName; startDate = $SeasonStart }
if ($created.Ok) {
    $seasonId = $created.Data.id
    Write-Host "Sezon oluşturuldu: $SeasonName (id $seasonId)"
} elseif ($created.Status -eq 409 -and $created.Code -eq 'SEASON_NAME_TAKEN') {
    $list = Invoke-Api GET '/seasons?size=100'
    $seasonId = ($list.Data.items | Where-Object { $_.name -eq $SeasonName } | Select-Object -First 1).id
    if (-not $seasonId) { Stop-WithError "`"$SeasonName`" adlı sezon var ama listede bulunamadı." }
    Write-Host "Sezon zaten var, kullanılıyor: $SeasonName (id $seasonId)"
} else {
    Stop-WithError "Sezon oluşturulamadı ($($created.Status) $($created.Code))."
}

# 2. Takımlar: kimlikleri varsaymak yerine ada göre ilk N takım alınır
$teams = @((Invoke-Api GET "/teams?size=$TeamCount").Data.items)
if ($teams.Count -lt $TeamCount) {
    Stop-WithError "$TeamCount takım istendi, veritabanında $($teams.Count) takım var. flyway:migrate çalıştı mı?"
}
foreach ($team in $teams) {
    $added = Invoke-Api POST "/seasons/$seasonId/teams" @{ teamId = $team.id }
    if ($added.Ok) {
        Write-Host "  + takım $($team.name)"
    } elseif ($added.Code -eq 'TEAM_ALREADY_IN_SEASON') {
        continue
    } elseif ($added.Code -eq 'SEASON_ALREADY_STARTED') {
        Write-Host '  Sezon başlamış; takım listesi değiştirilmedi.'
        break
    } else {
        Stop-WithError "Takım $($team.id) eklenemedi ($($added.Status) $($added.Code))."
    }
}

# 3. Fikstür
$fixture = Invoke-Api POST "/seasons/$seasonId/fixtures"
if ($fixture.Ok) {
    Write-Host 'Fikstür üretildi.'
} elseif ($fixture.Code -eq 'SEASON_ALREADY_STARTED') {
    Write-Host 'Fikstür zaten üretilmiş.'
} else {
    Stop-WithError "Fikstür üretilemedi ($($fixture.Status) $($fixture.Code))."
}

$rounds = @((Invoke-Api GET "/seasons/$seasonId/fixtures").Data.rounds)
$games = ($rounds | ForEach-Object { @($_.games).Count } | Measure-Object -Sum).Sum
Write-Host "Hazır: $($rounds.Count) tur, $games maç."
Write-Host "Fikstür sayfası: $($ApiUrl -replace '/api$', '')/fixtures.html"
