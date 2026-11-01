# Basketball League Manager

Basketball League Manager project for managing teams, players, and matches.

## Features

- Track player statistics
- Generate league standings
- Manage coaches and assign them to teams
- Record player injuries and availability
- View head-to-head records between teams

## Configuration

Database settings are read from environment variables, never from source code:

| Variable | Example |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/basketball_league` |
| `DB_USER` | `basketball` |
| `DB_PASSWORD` | your local database password |

Copy `.env.example` to `.env` and fill in the values. `.env` is git-ignored, so keep real secrets
only there.

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
