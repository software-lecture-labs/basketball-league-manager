/*
 * Puan durumu ana sayfası ve maç sonucu giriş formu.
 * Veriyi yalnızca REST API'den alır; kurallar sunucuda uygulanır.
 * Sunucudan ve kullanıcıdan gelen metinler her zaman textContent ile yazılır.
 */
(function () {
    'use strict';

    var API_SEASONS = 'api/seasons';
    var API_TEAMS = 'api/teams';
    var API_GAMES = 'api/games';
    var PAGE_SIZE = 100;
    var MIN_QUARTERS = 4;
    var MAX_QUARTERS = 10;
    var MAX_PLAYER_POINTS = 200;

    var STATUS_LABELS = {
        SCHEDULED: 'Oynanmadı',
        LIVE: 'Canlı',
        FINISHED: 'Oynandı (Onay bekliyor)',
        CONFIRMED: 'Onaylandı'
    };

    var state = {
        seasons: [],
        selectedSeasonId: null,
        games: [],
        selectedGame: null,
        quarterCount: MIN_QUARTERS
    };

    var el = {
        seasonSelect: document.getElementById('season-select'),
        standingsSubtitle: document.getElementById('standings-subtitle'),
        standingsNotice: document.getElementById('standings-notice'),
        standingsTbody: document.getElementById('standings-tbody'),
        matchForm: document.getElementById('match-entry-form'),
        gameSelect: document.getElementById('game-select'),
        selectedGameMeta: document.getElementById('selected-game-meta'),
        previewHomeName: document.getElementById('preview-home-name'),
        previewAwayName: document.getElementById('preview-away-name'),
        totalHome: document.getElementById('total-home'),
        totalAway: document.getElementById('total-away'),
        quartersList: document.getElementById('quarters-list'),
        btnAddOt: document.getElementById('btn-add-ot'),
        btnRemoveOt: document.getElementById('btn-remove-ot'),
        playerStatsBox: document.getElementById('player-stats-box'),
        playerStatsSum: document.getElementById('player-stats-sum'),
        btnAutofillStats: document.getElementById('btn-autofill-stats'),
        playerStatsNotice: document.getElementById('player-stats-notice'),
        playerStatsColumns: document.getElementById('player-stats-columns'),
        formFeedback: document.getElementById('form-feedback'),
        btnSubmitFinish: document.getElementById('btn-submit-finish'),
        btnSubmitConfirm: document.getElementById('btn-submit-confirm')
    };

    function requestJson(url, method, body) {
        var options = {
            method: method || 'GET',
            headers: { Accept: 'application/json' }
        };
        if (body !== undefined) {
            options.headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify(body);
        }
        return fetch(url, options).then(function (response) {
            return response.json().catch(function () { return null; }).then(function (data) {
                if (!response.ok) {
                    var detail = data && data.detail
                        ? data.detail
                        : 'Sunucu ' + response.status + ' yanıtı verdi.';
                    throw new Error(detail);
                }
                return data;
            });
        }, function () {
            throw new Error('Sunucuya ulaşılamadı; uygulamanın çalıştığını kontrol edin.');
        });
    }

    function initials(name) {
        var words = (name || '?').split(/\s+/).filter(Boolean);
        var meaningful = words.filter(function (w) { return w.length > 3; });
        if (meaningful.length === 0) {
            meaningful = words;
        }
        var letters = meaningful.length > 1
            ? meaningful[0].charAt(0) + meaningful[1].charAt(0)
            : meaningful[0].slice(0, 3);
        return letters.toLocaleUpperCase('tr');
    }

    function badgeColor(name) {
        var hash = 0;
        for (var i = 0; i < name.length; i++) {
            hash = (hash * 31 + name.charCodeAt(i)) % 360;
        }
        return 'hsl(' + hash + ' 45% 32%)';
    }

    function paintBadge(node, name) {
        node.textContent = initials(name);
        node.style.setProperty('--badge', badgeColor(name));
    }

    function showNotice(node, message, retry) {
        node.textContent = message;
        if (retry) {
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.textContent = 'Tekrar dene';
            btn.addEventListener('click', retry);
            node.appendChild(btn);
        }
        node.hidden = !message;
    }

    function setFeedback(message, type) {
        el.formFeedback.textContent = message;
        el.formFeedback.className = 'form-feedback' + (type ? ' ' + type : '');
        el.formFeedback.hidden = !message;
    }

    function createNumCell(value) {
        var td = document.createElement('td');
        td.textContent = String(value);
        return td;
    }

    function renderStandingsTable(rows) {
        el.standingsTbody.replaceChildren();
        if (!rows || rows.length === 0) {
            showNotice(el.standingsNotice, 'Bu sezonda henüz takım bulunmuyor.');
            return;
        }
        el.standingsNotice.hidden = true;

        rows.forEach(function (row) {
            var tr = document.createElement('tr');

            var tdRank = document.createElement('td');
            tdRank.className = 'col-rank';
            tdRank.textContent = String(row.rank);

            var tdTeam = document.createElement('td');
            tdTeam.className = 'col-team';
            var link = document.createElement('a');
            link.className = 'standings-team-link';
            link.href = 'teams.html#takim-' + row.teamId;
            var badge = document.createElement('span');
            badge.className = 'badge';
            badge.setAttribute('aria-hidden', 'true');
            paintBadge(badge, row.teamName);
            var teamName = document.createElement('span');
            teamName.className = 'standings-team-name';
            teamName.textContent = row.teamName;
            link.append(badge, teamName);
            tdTeam.appendChild(link);

            var tdPlayed = createNumCell(row.played);
            var tdWon = createNumCell(row.won);
            var tdLost = createNumCell(row.lost);
            var tdFor = createNumCell(row.pointsFor);
            var tdAgainst = createNumCell(row.pointsAgainst);

            var tdDiff = document.createElement('td');
            tdDiff.textContent = row.pointDiff > 0 ? '+' + row.pointDiff : String(row.pointDiff);
            if (row.pointDiff > 0) {
                tdDiff.className = 'diff-pos';
            } else if (row.pointDiff < 0) {
                tdDiff.className = 'diff-neg';
            }

            var tdPts = document.createElement('td');
            tdPts.className = 'col-pts';
            tdPts.textContent = String(row.leaguePoints);

            tr.append(tdRank, tdTeam, tdPlayed, tdWon, tdLost, tdFor, tdAgainst, tdDiff, tdPts);
            el.standingsTbody.appendChild(tr);
        });
    }

    // --- Çeyrek satırları ---

    function renderQuarterInputs(existingQuarters) {
        var count = Math.max(MIN_QUARTERS, existingQuarters ? existingQuarters.length : MIN_QUARTERS);
        state.quarterCount = Math.min(MAX_QUARTERS, count);
        el.quartersList.replaceChildren();

        for (var q = 1; q <= state.quarterCount; q++) {
            var existing = existingQuarters
                ? existingQuarters.find(function (item) { return item.quarterNo === q; })
                : null;
            appendQuarterRow(
                q,
                existing ? existing.homePoints : 0,
                existing ? existing.awayPoints : 0
            );
        }
        updateOtButtons();
        recomputeTotals();
    }

    function appendQuarterRow(quarterNo, homeVal, awayVal) {
        var row = document.createElement('div');
        row.className = 'quarter-row';
        row.dataset.quarter = String(quarterNo);

        var label = document.createElement('label');
        label.htmlFor = 'q' + quarterNo + '-home';
        label.textContent = quarterNo <= 4 ? quarterNo + '. Çeyrek' : 'Uzatma ' + (quarterNo - 4);

        var homeInput = document.createElement('input');
        homeInput.type = 'number';
        homeInput.id = 'q' + quarterNo + '-home';
        homeInput.min = '0';
        homeInput.max = '100';
        homeInput.step = '1';
        homeInput.required = true;
        homeInput.value = String(homeVal !== undefined ? homeVal : 0);
        homeInput.setAttribute('aria-label', label.textContent + ' ev sahibi');
        homeInput.addEventListener('input', recomputeTotals);

        var awayInput = document.createElement('input');
        awayInput.type = 'number';
        awayInput.id = 'q' + quarterNo + '-away';
        awayInput.min = '0';
        awayInput.max = '100';
        awayInput.step = '1';
        awayInput.required = true;
        awayInput.value = String(awayVal !== undefined ? awayVal : 0);
        awayInput.setAttribute('aria-label', label.textContent + ' deplasman');
        awayInput.addEventListener('input', recomputeTotals);

        row.append(label, homeInput, awayInput);
        el.quartersList.appendChild(row);
    }

    function updateOtButtons() {
        el.btnAddOt.disabled = state.quarterCount >= MAX_QUARTERS;
        el.btnRemoveOt.hidden = state.quarterCount <= MIN_QUARTERS;
    }

    function readQuarters() {
        var rows = el.quartersList.querySelectorAll('.quarter-row');
        var list = [];
        rows.forEach(function (row) {
            var qNo = Number(row.dataset.quarter);
            var inputs = row.querySelectorAll('input');
            var h = Number.parseInt(inputs[0].value, 10);
            var a = Number.parseInt(inputs[1].value, 10);
            list.push({
                quarterNo: qNo,
                homePoints: Number.isNaN(h) ? 0 : h,
                awayPoints: Number.isNaN(a) ? 0 : a
            });
        });
        return list;
    }

    function recomputeTotals() {
        var quarters = readQuarters();
        var homeTotal = 0;
        var awayTotal = 0;
        quarters.forEach(function (q) {
            homeTotal += q.homePoints;
            awayTotal += q.awayPoints;
        });
        el.totalHome.textContent = String(homeTotal);
        el.totalAway.textContent = String(awayTotal);
        updatePlayerStatsSum();
    }

    // --- Oyuncu Sayıları ---

    function loadRostersForSelectedGame(game) {
        el.playerStatsColumns.replaceChildren();
        el.playerStatsNotice.hidden = true;

        if (!game) {
            el.playerStatsBox.hidden = true;
            return;
        }
        el.playerStatsBox.hidden = false;

        Promise.all([
            loadTeamPlayers(game.homeTeam.id, 0, []),
            loadTeamPlayers(game.awayTeam.id, 0, [])
        ]).then(function (results) {
            renderPlayerStatsInputs(game, results[0], results[1]);
        }).catch(function (err) {
            el.playerStatsNotice.textContent = err.message;
            el.playerStatsNotice.hidden = false;
        });
    }

    function loadTeamPlayers(teamId, page, collected) {
        var url = API_TEAMS + '/' + teamId + '/players?page=' + page + '&size=' + PAGE_SIZE;
        return requestJson(url).then(function (res) {
            var items = res.items || [];
            var all = collected.concat(items);
            if (items.length > 0 && res.totalItems !== undefined && all.length < res.totalItems) {
                return loadTeamPlayers(teamId, page + 1, all);
            }
            return all;
        });
    }

    function renderPlayerStatsInputs(game, homePlayers, awayPlayers) {
        el.playerStatsColumns.replaceChildren();

        var existingMap = {};
        if (game.playerPoints) {
            game.playerPoints.forEach(function (pp) {
                existingMap[pp.playerId] = pp.points;
            });
        }

        if (homePlayers.length === 0 || awayPlayers.length === 0) {
            el.playerStatsNotice.textContent =
                'Onay için her iki takımın da kadrosunda oyuncu olmalıdır.';
            el.playerStatsNotice.hidden = false;
        }

        el.playerStatsColumns.append(
            buildTeamStatsGroup(game.homeTeam.name, 'home', homePlayers, existingMap),
            buildTeamStatsGroup(game.awayTeam.name, 'away', awayPlayers, existingMap)
        );
        updatePlayerStatsSum();
    }

    function buildTeamStatsGroup(teamName, side, players, existingMap) {
        var group = document.createElement('div');
        group.className = 'player-team-group';
        var heading = document.createElement('h4');
        heading.textContent = teamName;
        group.appendChild(heading);

        players.forEach(function (p) {
            var row = document.createElement('div');
            row.className = 'player-stat-row';

            var label = document.createElement('label');
            label.htmlFor = 'player-pts-' + p.id;
            label.textContent = '#' + p.jerseyNumber + ' ' + p.firstName + ' ' + p.lastName;

            var input = document.createElement('input');
            input.type = 'number';
            input.id = 'player-pts-' + p.id;
            input.dataset.side = side;
            input.dataset.playerId = String(p.id);
            input.min = '0';
            input.max = String(MAX_PLAYER_POINTS);
            input.step = '1';
            input.value = String(existingMap[p.id] !== undefined ? existingMap[p.id] : 0);
            input.addEventListener('input', updatePlayerStatsSum);

            row.append(label, input);
            group.appendChild(row);
        });
        return group;
    }

    function collectPlayerStats() {
        var inputs = el.playerStatsColumns.querySelectorAll('input[data-player-id]');
        var homeSum = 0;
        var awaySum = 0;
        var entries = [];
        inputs.forEach(function (inp) {
            var val = Number.parseInt(inp.value, 10);
            var pts = Number.isNaN(val) ? 0 : Math.max(0, val);
            if (inp.dataset.side === 'home') {
                homeSum += pts;
            } else {
                awaySum += pts;
            }
            if (pts > 0) {
                entries.push({ playerId: Number(inp.dataset.playerId), points: pts });
            }
        });
        return { homeSum: homeSum, awaySum: awaySum, entries: entries };
    }

    function updatePlayerStatsSum() {
        var stats = collectPlayerStats();
        var targetHome = Number(el.totalHome.textContent) || 0;
        var targetAway = Number(el.totalAway.textContent) || 0;
        el.playerStatsSum.textContent =
            'Ev: ' + stats.homeSum + '/' + targetHome + ' · Dep: ' + stats.awaySum + '/' + targetAway;
    }

    function autofillPlayerStats() {
        var targetHome = Number(el.totalHome.textContent) || 0;
        var targetAway = Number(el.totalAway.textContent) || 0;
        distributePoints('home', targetHome);
        distributePoints('away', targetAway);
        updatePlayerStatsSum();
    }

    function distributePoints(side, total) {
        var selector = 'input[data-side="' + side + '"]';
        var inputs = Array.from(el.playerStatsColumns.querySelectorAll(selector));
        if (inputs.length === 0) {
            return;
        }
        var base = Math.floor(total / inputs.length);
        var remainder = total % inputs.length;
        inputs.forEach(function (inp, idx) {
            var pts = Math.min(MAX_PLAYER_POINTS, base + (idx < remainder ? 1 : 0));
            inp.value = String(pts);
        });
    }

    // --- API Akışı ---

    function init() {
        el.standingsNotice.hidden = true;
        el.standingsSubtitle.textContent = 'Sezonlar yükleniyor...';

        requestJson(API_SEASONS + '?page=0&size=' + PAGE_SIZE)
            .then(function (data) {
                var seasons = data.items || [];
                if (seasons.length === 0) {
                    handleEmptySeasons();
                    return;
                }
                state.seasons = seasons;
                renderSeasonSelector(seasons);

                var preferred = seasons.find(function (s) {
                    return s.status === 'ACTIVE';
                }) || seasons[0];
                el.seasonSelect.value = String(preferred.id);
                return loadSeasonData(preferred.id);
            })
            .catch(function (err) {
                el.standingsTbody.replaceChildren();
                el.standingsSubtitle.textContent = 'Veri yüklenemedi';
                showNotice(el.standingsNotice, err.message, init);
                disableMatchForm('Sezon verisi alınamadığı için maç sonucu girilemiyor.');
            });
    }

    function handleEmptySeasons() {
        el.standingsTbody.replaceChildren();
        el.standingsSubtitle.textContent = 'Henüz sezon bulunmuyor';
        el.seasonSelect.replaceChildren();
        var opt = document.createElement('option');
        opt.value = '';
        opt.textContent = 'Sezon yok';
        el.seasonSelect.appendChild(opt);
        el.seasonSelect.disabled = true;

        showNotice(
            el.standingsNotice,
            'Henüz sezon oluşturulmamış. Önce Yönetici sayfasından sezon oluşturun.'
        );
        disableMatchForm('Önce Yönetici sayfasından sezon oluşturun ve fikstür üretin.');
    }

    function disableMatchForm(selectText) {
        el.gameSelect.replaceChildren();
        var opt = document.createElement('option');
        opt.value = '';
        opt.textContent = selectText;
        el.gameSelect.appendChild(opt);
        el.gameSelect.disabled = true;
        el.btnSubmitFinish.disabled = true;
        el.btnSubmitConfirm.disabled = true;
        el.selectedGameMeta.hidden = true;
        el.playerStatsBox.hidden = true;
        el.previewHomeName.textContent = 'Ev Sahibi';
        el.previewAwayName.textContent = 'Deplasman';
    }

    function renderSeasonSelector(seasons) {
        el.seasonSelect.replaceChildren();
        seasons.forEach(function (s) {
            var opt = document.createElement('option');
            opt.value = String(s.id);
            opt.textContent = s.name;
            el.seasonSelect.appendChild(opt);
        });
        el.seasonSelect.disabled = false;
    }

    function loadSeasonData(seasonId) {
        state.selectedSeasonId = seasonId;
        var season = state.seasons.find(function (s) { return s.id === seasonId; });
        el.standingsSubtitle.textContent = season
            ? season.name + ' · Onaylı maçlardan hesaplanır'
            : 'Sezon puan durumu';

        return Promise.all([
            requestJson(API_SEASONS + '/' + seasonId + '/standings'),
            requestJson(API_SEASONS + '/' + seasonId + '/fixtures').catch(function () {
                return null;
            })
        ]).then(function (results) {
            renderStandingsTable(results[0]);
            populateFixtureGames(results[1]);
        }).catch(function (err) {
            el.standingsTbody.replaceChildren();
            showNotice(el.standingsNotice, err.message, function () {
                void loadSeasonData(seasonId);
            });
        });
    }

    function populateFixtureGames(fixture) {
        var allGames = [];
        if (fixture && fixture.rounds) {
            fixture.rounds.forEach(function (round) {
                (round.games || []).forEach(function (g) {
                    allGames.push({
                        id: g.id,
                        roundNumber: round.number,
                        homeTeam: g.homeTeam,
                        awayTeam: g.awayTeam,
                        status: g.status
                    });
                });
            });
        }

        state.games = allGames;
        if (allGames.length === 0) {
            disableMatchForm('Bu sezonda henüz fikstür üretilmemiş');
            return;
        }

        el.gameSelect.replaceChildren();
        el.gameSelect.disabled = false;
        allGames.forEach(function (g) {
            var opt = document.createElement('option');
            opt.value = String(g.id);
            var statusLabel = STATUS_LABELS[g.status] || g.status;
            opt.textContent = g.roundNumber + '. Hafta · '
                + g.homeTeam.name + ' – ' + g.awayTeam.name
                + ' (' + statusLabel + ')';
            el.gameSelect.appendChild(opt);
        });

        var firstEditable = allGames.find(function (g) {
            return g.status !== 'CONFIRMED';
        }) || allGames[0];
        el.gameSelect.value = String(firstEditable.id);
        loadSelectedGameDetail(firstEditable.id);
    }

    function loadSelectedGameDetail(gameId) {
        if (!gameId) {
            return;
        }
        requestJson(API_GAMES + '/' + gameId)
            .then(function (detail) {
                state.selectedGame = detail;
                el.previewHomeName.textContent = detail.homeTeam.name;
                el.previewAwayName.textContent = detail.awayTeam.name;

                var isConfirmed = detail.status === 'CONFIRMED';
                var statusLabel = STATUS_LABELS[detail.status] || detail.status;
                el.selectedGameMeta.textContent =
                    detail.roundNumber + '. Hafta · Durum: ' + statusLabel;
                el.selectedGameMeta.hidden = false;

                el.btnSubmitFinish.disabled = isConfirmed;
                el.btnSubmitConfirm.disabled = isConfirmed;

                renderQuarterInputs(detail.quarters);
                loadRostersForSelectedGame(detail);
                setFeedback(
                    isConfirmed ? 'Bu maç onaylanmış durumda; skoru değiştirilemez.' : '',
                    ''
                );
            })
            .catch(function (err) {
                setFeedback(err.message, 'error');
            });
    }

    function submitGame(shouldConfirm) {
        var game = state.selectedGame;
        if (!game) {
            setFeedback('Lütfen önce bir maç seçin.', 'error');
            return;
        }
        if (game.status === 'CONFIRMED') {
            setFeedback('Onaylanmış maçlar değiştirilemez.', 'error');
            return;
        }

        var quarters = readQuarters();
        var stats = collectPlayerStats();
        if (shouldConfirm) {
            var targetHome = Number(el.totalHome.textContent) || 0;
            var targetAway = Number(el.totalAway.textContent) || 0;
            if (stats.homeSum !== targetHome || stats.awaySum !== targetAway) {
                el.playerStatsBox.open = true;
                setFeedback(
                    'Onay için oyuncu sayıları toplamı maç skoruna eşit olmalıdır '
                    + '(Ev: ' + stats.homeSum + '/' + targetHome
                    + ', Dep: ' + stats.awaySum + '/' + targetAway + ').',
                    'error'
                );
                return;
            }
        }

        el.btnSubmitFinish.disabled = true;
        el.btnSubmitConfirm.disabled = true;
        setFeedback('Maç sonucu kaydediliyor...', '');

        var chain = Promise.resolve(game);
        if (game.status === 'SCHEDULED') {
            chain = chain.then(function (current) {
                var url = API_GAMES + '/' + current.id + '/start';
                return requestJson(url, 'POST', { version: current.version });
            });
        } else if (game.status === 'FINISHED') {
            chain = chain.then(function (current) {
                var url = API_GAMES + '/' + current.id + '/reopen';
                return requestJson(url, 'POST', { version: current.version });
            });
        }

        chain = chain
            .then(function (current) {
                var url = API_GAMES + '/' + current.id + '/quarters';
                return requestJson(url, 'PUT', {
                    version: current.version,
                    quarters: quarters
                });
            })
            .then(function (current) {
                var url = API_GAMES + '/' + current.id + '/finish';
                return requestJson(url, 'POST', { version: current.version });
            });

        if (shouldConfirm) {
            chain = chain
                .then(function (current) {
                    var url = API_GAMES + '/' + current.id + '/stats';
                    return requestJson(url, 'PUT', {
                        version: current.version,
                        players: stats.entries
                    });
                })
                .then(function (current) {
                    var url = API_GAMES + '/' + current.id + '/confirm';
                    return requestJson(url, 'POST', { version: current.version });
                });
        }

        chain
            .then(function (updated) {
                state.selectedGame = updated;
                var msg = shouldConfirm
                    ? 'Maç onaylandı ve puan durumu güncellendi.'
                    : 'Maç skoru kaydedildi (Oynandı). Puan tablosu için onaylayın.';
                setFeedback(msg, 'success');
                return loadSeasonData(state.selectedSeasonId);
            })
            .catch(function (error) {
                setFeedback(error.message, 'error');
                el.btnSubmitFinish.disabled = false;
                el.btnSubmitConfirm.disabled = false;
            });
    }

    // --- Olay Dinleyicileri ---

    function setupEvents() {
        el.seasonSelect.addEventListener('change', function () {
            var id = Number(el.seasonSelect.value);
            if (id > 0) {
                // loadSeasonData hatayı kendisi yakalayıp tabloda gösteriyor.
                void loadSeasonData(id);
            }
        });

        el.gameSelect.addEventListener('change', function () {
            var id = Number(el.gameSelect.value);
            if (id > 0) {
                loadSelectedGameDetail(id);
            }
        });

        el.btnAddOt.addEventListener('click', function () {
            if (state.quarterCount < MAX_QUARTERS) {
                state.quarterCount++;
                appendQuarterRow(state.quarterCount, 0, 0);
                updateOtButtons();
                recomputeTotals();
            }
        });

        el.btnRemoveOt.addEventListener('click', function () {
            if (state.quarterCount > MIN_QUARTERS) {
                var last = el.quartersList.lastElementChild;
                if (last) {
                    el.quartersList.removeChild(last);
                }
                state.quarterCount--;
                updateOtButtons();
                recomputeTotals();
            }
        });

        el.btnAutofillStats.addEventListener('click', autofillPlayerStats);

        el.matchForm.addEventListener('submit', function (event) {
            event.preventDefault();
            setFeedback('', '');
            submitGame(false);
        });

        el.btnSubmitConfirm.addEventListener('click', function () {
            setFeedback('', '');
            submitGame(true);
        });
    }

    setupEvents();
    renderQuarterInputs(null);
    init();
}());
