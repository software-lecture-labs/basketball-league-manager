# Basketball League Manager

Basketball League Manager project for managing teams, players, and matches.

## Features

- Track player statistics
- Generate league standings
- Manage coaches and assign them to teams
- Record player injuries and availability
- View head-to-head records between teams

## Tech Stack & Architecture

- **Platform:** Java 21 LTS
- **Specification:** Jakarta EE 11 (Web Profile) — [ADR-0002](docs/adr/0002-jakarta-ee-ve-payara-micro.md)
- **Runtime Server:** Payara Micro 7.x
- **Architecture:** Layered Monolith (single WAR deployment) — [ADR-0001](docs/adr/0001-katmanli-mimari.md)
- **Database:** PostgreSQL

## Configuration

Database settings are read from environment variables, never from source code:

| Variable | Example |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/basketball_league` |
| `DB_USER` | `basketball` |
| `DB_PASSWORD` | your local database password |

Copy `.env.example` to `.env` and fill in the values. `.env` is git-ignored, so keep real secrets only there.

Payara does not read `.env` by itself. Load the variables into the shell that starts Payara:

```bash
# macOS / Linux
set -a; source .env; set +a
```

```powershell
# Windows (PowerShell)
Get-Content .env | Where-Object { $_ -match '^[A-Z_]+=' } | ForEach-Object {
    $name, $value = $_ -split '=', 2
    Set-Item "env:$name" $value
}
```

If a variable is missing, the application is not deployed and `GET /api/ping` returns 404.

## Running with Payara Micro

Build the WAR package:

```bash
./mvnw clean package
```

Start Payara Micro:

```bash
# macOS / Linux
java -jar payara-micro.jar --deploy target/basketball-league.war --contextroot /basketball-league --nocluster

# Windows (PowerShell)
java -jar payara-micro.jar --deploy target\basketball-league.war --contextroot /basketball-league --nocluster
```

Once started, verify the deployment:
- UI Portal: `http://localhost:8080/basketball-league/`
- Teams & Roster Page: `http://localhost:8080/basketball-league/teams.html`
- Health Ping: `http://localhost:8080/basketball-league/api/ping`

## Architecture Decision Records (ADR)

| ADR | Title | Status |
|---|---|---|
| [ADR-0001](docs/adr/0001-katmanli-mimari.md) | Katmanlı Monolit Mimari | Kabul edildi |
| [ADR-0002](docs/adr/0002-jakarta-ee-ve-payara-micro.md) | Jakarta EE 11 Web Profile ve Payara Micro Seçimi | Kabul edildi |
| [ADR-0006](docs/adr/0006-hata-yonetimi-ve-loglama.md) | Hata Yönetimi ve Loglama Politikası | Kabul edildi |
| [ADR-0008](docs/adr/0008-fikstur-algoritmasi.md) | Fikstür Üretimi (Circle Method) | Kabul edildi |
