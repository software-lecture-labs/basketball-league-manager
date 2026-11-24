// standings.js – Client‑side logic for the Puan Durumu ana sayfa
// 1. Load team list (from API or demo data)
// 2. Load standings (from API or demo data)
// 3. Render standings table
// 4. Calculate totals while the user edits quarter scores
// 5. Submit a match – updates the in‑memory standings and re‑renders

(function () {
    'use strict';

    const API_TEAMS = 'api/teams';
    const API_STANDINGS = 'api/seasons/1/standings';
    const API_GAMES = 'api/games'; // not used in demo mode

    // Demo teams – same order as V2 seed migration
    const DEMO_TEAMS = [
        {id: 1, name: 'Anadolu Efes', city: 'İstanbul'},
        {id: 2, name: 'AS Monaco', city: 'Monako'},
        {id: 3, name: 'ASVEL', city: 'Villeurbanne'},
        {id: 4, name: 'Barcelona', city: 'Barselona'},
        {id: 5, name: 'Baskonia', city: 'Vitoria-Gasteiz'},
        {id: 6, name: 'Bayern Munich', city: 'Münih'},
        {id: 7, name: 'Crvena zvezda', city: 'Belgrad'},
        {id: 8, name: 'Dubai Basketball', city: 'Dubai'},
        {id: 9, name: 'Fenerbahçe', city: 'İstanbul'},
        {id: 10, name: 'Hapoel IBI Tel Aviv', city: 'Tel Aviv'},
        {id: 11, name: 'Maccabi Tel Aviv', city: 'Tel Aviv'},
        {id: 12, name: 'Olimpia Milano', city: 'Milano'},
        {id: 13, name: 'Olympiacos', city: 'Pire'},
        {id: 14, name: 'Panathinaikos', city: 'Atina'},
        {id: 15, name: 'Paris Basketball', city: 'Paris'},
        {id: 16, name: 'Partizan', city: 'Belgrad'},
        {id: 17, name: 'Real Madrid', city: 'Madrid'},
        {id: 18, name: 'Valencia Basket', city: 'Valensiya'},
        {id: 19, name: 'Virtus Bologna', city: 'Bologna'},
        {id: 20, name: 'Žalgiris', city: 'Kaunas'}
    ];

    // In‑memory list of confirmed games (demo only)
    const games = [];

    // DOM references
    const el = {
        homeSelect: document.getElementById('home-team'),
        awaySelect: document.getElementById('away-team'),
        qHome: [
            document.getElementById('q1-home'),
            document.getElementById('q2-home'),
            document.getElementById('q3-home'),
            document.getElementById('q4-home'),
            document.getElementById('ot-home')
        ],
        qAway: [
            document.getElementById('q1-away'),
            document.getElementById('q2-away'),
            document.getElementById('q3-away'),
            document.getElementById('q4-away'),
            document.getElementById('ot-away')
        ],
        totalHome: document.getElementById('total-home'),
        totalAway: document.getElementById('total-away'),
        formNotice: document.getElementById('form-notice'),
        standingsTbody: document.getElementById('standings-tbody'),
        standingsCount: document.getElementById('standings-count'),
        standingsNotice: document.getElementById('standings-notice'),
        matchForm: document.getElementById('match-entry-form')
    };

    /** Utility: fetch JSON, fallback to null on network error */
    function fetchJson(url) {
        return fetch(url, {headers: {Accept: 'application/json'}})
            .then(res => res.ok ? res.json() : Promise.reject(new Error('HTTP ' + res.status)))
            .catch(() => null);
    }

    /** Populate team <select> elements */
    function populateTeams(teams) {
        teams.forEach(t => {
            const optHome = document.createElement('option');
            optHome.value = t.id;
            optHome.textContent = `${t.name} (${t.city})`;
            el.homeSelect.appendChild(optHome);
            const optAway = optHome.cloneNode(true);
            optAway.value = t.id;
            el.awaySelect.appendChild(optAway);
        });
    }

    /** Compute aggregate totals for each side */
    function recomputeTotals() {
        const sum = (arr) => arr.reduce((a, i) => a + (parseInt(i.value, 10) || 0), 0);
        const home = sum(el.qHome);
        const away = sum(el.qAway);
        el.totalHome.textContent = home;
        el.totalAway.textContent = away;
    }

    /** Simple validation – no draw, totals match quarter sum */
    function validateForm() {
        const home = parseInt(el.totalHome.textContent, 10);
        const away = parseInt(el.totalAway.textContent, 10);
        if (home === away) {
            return 'Maç berabere bitti. Uzatma skoru girilmedi mi?';
        }
        return null;
    }

    /** Calculate standings from the in‑memory games array */
    function calculateStandings(teams) {
        const map = {};
        teams.forEach(t => {
            map[t.id] = {
                id: t.id,
                name: t.name,
                city: t.city,
                played: 0,
                won: 0,
                lost: 0,
                pf: 0,
                pa: 0,
                diff: 0,
                pts: 0 // league points (win=2, loss=1)
            };
        });
        games.forEach(g => {
            const h = map[g.homeTeamId];
            const a = map[g.awayTeamId];
            h.played++; a.played++;
            h.pf += g.homeScore; h.pa += g.awayScore;
            a.pf += g.awayScore; a.pa += g.homeScore;
            h.diff = h.pf - h.pa; a.diff = a.pf - a.pa;
            if (g.homeScore > g.awayScore) {
                h.won++; a.lost++; h.pts += 2; a.pts += 1;
            } else {
                a.won++; h.lost++; a.pts += 2; h.pts += 1;
            }
        });
        const list = Object.values(map);
        // Simple tie‑breaker: pts → diff → pf → name
        list.sort((x, y) => {
            if (y.pts !== x.pts) return y.pts - x.pts;
            if (y.diff !== x.diff) return y.diff - x.diff;
            if (y.pf !== x.pf) return y.pf - x.pf;
            return x.name.localeCompare(y.name, 'tr');
        });
        return list;
    }

    /** Render the standings table */
    function renderStandings(standings) {
        el.standingsTbody.innerHTML = '';
        standings.forEach((s, idx) => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="col-rank">${idx + 1}</td>
                <td class="col-team">${s.name}</td>
                <td class="col-played">${s.played}</td>
                <td class="col-won">${s.won}</td>
                <td class="col-lost">${s.lost}</td>
                <td class="col-points">${s.pf}</td>
                <td class="col-conceded">${s.pa}</td>
                <td class="col-diff">${s.diff}</td>
                <td class="col-pts">${s.pts}</td>
            `;
            el.standingsTbody.appendChild(tr);
        });
        el.standingsCount.textContent = `Toplam ${standings.length} takım`;
    }

    /** Initialise the page */
    async function init() {
        // 1. Load teams
        const teams = await fetchJson(API_TEAMS) || DEMO_TEAMS;
        populateTeams(teams);

        // 2. Load standings (fallback to empty – will be recomputed from games)
        const remoteStandings = await fetchJson(API_STANDINGS);
        if (remoteStandings) {
            // Server already provides a ready‑made list; render it directly.
            renderStandings(remoteStandings);
        } else {
            // Demo mode – compute from in‑memory games (initially empty).
            const standings = calculateStandings(teams);
            renderStandings(standings);
        }

        // 3. Wire quarter inputs
        [...el.qHome, ...el.qAway].forEach(inp => inp.addEventListener('input', recomputeTotals));
        recomputeTotals();

        // 4. Form submit handler
        el.matchForm.addEventListener('submit', ev => {
            ev.preventDefault();
            el.formNotice.hidden = true;
            const err = validateForm();
            if (err) {
                el.formNotice.textContent = err;
                el.formNotice.hidden = false;
                return;
            }
            const homeId = parseInt(el.homeSelect.value, 10);
            const awayId = parseInt(el.awaySelect.value, 10);
            if (homeId === awayId) {
                el.formNotice.textContent = 'Ev sahibi ve deplasman aynı takım olamaz.';
                el.formNotice.hidden = false;
                return;
            }
            const homeScore = parseInt(el.totalHome.textContent, 10);
            const awayScore = parseInt(el.totalAway.textContent, 10);
            // Record the game locally (demo mode). In a real app you would POST to API_GAMES.
            games.push({homeTeamId: homeId, awayTeamId: awayId, homeScore, awayScore});
            const updated = calculateStandings(teams);
            renderStandings(updated);
            el.formNotice.textContent = 'Maç kaydedildi (demo).';
            el.formNotice.hidden = false;
            // reset form
            el.matchForm.reset();
            recomputeTotals();
        });
    }

    init();
})();
