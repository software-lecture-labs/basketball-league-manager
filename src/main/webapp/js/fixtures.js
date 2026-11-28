/*
 * Fikstür sayfası.
 * Veriyi REST API'den alır (/api/seasons/{id}/fixtures).
 * Hafta bazında gruplama ve oynanmış / oynanmamış maç filtrelerini yönetir.
 * Kullanıcıdan veya sunucudan gelen tüm metinler textContent ile yazılır (XSS güvenliği).
 */
(function () {
    'use strict';

    var API_SEASONS = 'api/seasons';
    var API_TEAMS = 'api/teams';

    var STATUS_LABELS = {
        SCHEDULED: 'Oynanmadı',
        LIVE: 'Canlı',
        FINISHED: 'Oynandı',
        CONFIRMED: 'Tamamlandı'
    };

    var state = {
        rounds: [],
        currentRoundNumber: 1,
        showAllRounds: false,
        filter: 'ALL', // 'ALL' | 'SCHEDULED' | 'FINISHED'
        season: null,
        isDemo: false
    };

    var el = {
        seasonTitle: document.getElementById('season-title'),
        roundsNav: document.getElementById('rounds-nav'),
        prevRoundBtn: document.getElementById('prev-round'),
        nextRoundBtn: document.getElementById('next-round'),
        toggleAllBtn: document.getElementById('toggle-all-rounds'),
        filterButtons: document.querySelectorAll('.filter-btn'),
        fixturesContainer: document.getElementById('fixtures-container'),
        notice: document.getElementById('fixtures-notice'),
        gameDetailDialog: document.getElementById('game-detail-dialog'),
        gameDetailTitle: document.getElementById('game-detail-title'),
        gameDetailContent: document.getElementById('game-detail-content'),
        gameDetailClose: document.getElementById('game-detail-close')
    };

    function getJson(url) {
        return fetch(url, { headers: { Accept: 'application/json' } }).then(function (response) {
            return response.json().catch(function () { return null; }).then(function (body) {
                if (!response.ok) {
                    var detail = body && body.detail ? body.detail : 'Sunucu ' + response.status + ' yanıtı verdi.';
                    throw new Error(detail);
                }
                return body;
            });
        }, function () {
            throw new Error('Sunucuya ulaşılamadı; uygulamanın çalıştığını kontrol edin.');
        });
    }

    function getTeamPlayers(teamId, page, players) {
        return getJson(API_TEAMS + '/' + teamId + '/players?page=' + page + '&size=100')
            .then(function (response) {
                var items = response.items || response;
                var allPlayers = players.concat(items);
                if (response.totalItems !== undefined && allPlayers.length < response.totalItems) {
                    return getTeamPlayers(teamId, page + 1, allPlayers);
                }
                return allPlayers;
            });
    }

    // "FC", "EA7" gibi ekleri eler; tek kelimede ilk 3 harfi alır.
    function initials(name) {
        if (!name) return '?';
        var words = name.split(/\s+/).filter(Boolean);
        var meaningful = words.filter(function (word) { return word.length > 3; });
        if (meaningful.length === 0) {
            meaningful = words;
        }
        var letters = meaningful.length > 1
            ? meaningful[0].charAt(0) + meaningful[1].charAt(0)
            : meaningful[0].slice(0, 3);
        return letters.toLocaleUpperCase('tr');
    }

    // Takım adına göre deterministik renk tonu üretir (ADR-0001)
    function badgeColor(name) {
        if (!name) return 'hsl(210 45% 32%)';
        var hash = 0;
        for (var i = 0; i < name.length; i++) {
            hash = (hash * 31 + name.charCodeAt(i)) % 360;
        }
        return 'hsl(' + hash + ' 45% 32%)';
    }

    function paintBadge(node, team) {
        node.textContent = initials(team.name);
        node.style.setProperty('--badge', badgeColor(team.name));
    }

    function showNotice(node, message, retry) {
        node.replaceChildren();
        var span = document.createElement('span');
        span.textContent = message;
        node.appendChild(span);
        if (retry) {
            var button = document.createElement('button');
            button.type = 'button';
            button.textContent = 'Tekrar dene';
            button.addEventListener('click', retry);
            node.appendChild(button);
        }
        node.hidden = false;
    }

    // ADR-0008 Circle Method (Fikstür Üretim Algoritması)
    function generateCircleMethodFixture(teams) {
        var teamList = teams.slice();
        var n = teamList.length;
        var hasBye = (n % 2 !== 0);
        if (hasBye) {
            teamList.push(null);
            n++;
        }

        var halfRounds = n - 1;
        var rounds = [];

        // İlk yarı
        for (var r = 0; r < halfRounds; r++) {
            var roundNumber = r + 1;
            var pairings = [];
            var byeTeam = null;

            for (var i = 0; i < n / 2; i++) {
                var homeIdx = (r + i) % (n - 1);
                var awayIdx = (n - 1 - i + r) % (n - 1);
                if (i === 0) {
                    awayIdx = n - 1;
                }

                var t1 = teamList[homeIdx];
                var t2 = teamList[awayIdx];

                if (t1 === null) {
                    byeTeam = t2;
                } else if (t2 === null) {
                    byeTeam = t1;
                } else {
                    if (r % 2 === 1 && i === 0) {
                        pairings.push({ home: t2, away: t1 });
                    } else {
                        pairings.push({ home: t1, away: t2 });
                    }
                }
            }

            // Durum belirleme (ilk 2 hafta oynanmış, 3. hafta canlı, geri kalan oynanmamış)
            var status = 'SCHEDULED';
            if (roundNumber <= 2) {
                status = 'FINISHED';
            } else if (roundNumber === 3) {
                status = 'LIVE';
            }

            var games = pairings.map(function (p, idx) {
                return {
                    id: roundNumber * 100 + idx,
                    homeTeam: p.home,
                    awayTeam: p.away,
                    status: status
                };
            });

            rounds.push({ number: roundNumber, games: games, byeTeam: byeTeam });
        }

        // İkinci yarı (Saha değişimi)
        for (var r2 = 0; r2 < halfRounds; r2++) {
            var roundNumber2 = halfRounds + r2 + 1;
            var firstHalfRound = rounds[r2];
            var secondHalfGames = firstHalfRound.games.map(function (g, idx) {
                return {
                    id: roundNumber2 * 100 + idx,
                    homeTeam: g.awayTeam,
                    awayTeam: g.homeTeam,
                    status: 'SCHEDULED'
                };
            });
            rounds.push({
                number: roundNumber2,
                games: secondHalfGames,
                byeTeam: firstHalfRound.byeTeam
            });
        }

        return rounds;
    }

    function createMatchCard(game) {
        var card = document.createElement('article');
        card.className = 'match-card';

        var status = (game.status || 'SCHEDULED').toUpperCase();
        if (status === 'FINISHED' || status === 'CONFIRMED') {
            card.classList.add('is-finished');
        } else if (status === 'LIVE') {
            card.classList.add('is-live');
        } else {
            card.classList.add('is-scheduled');
        }

        // Ev Sahibi Takım
        var homeSide = document.createElement('div');
        homeSide.className = 'team-entry home';

        var homeText = document.createElement('div');
        homeText.className = 'team-text';
        var homeName = document.createElement('span');
        homeName.className = 'team-name';
        homeName.textContent = game.homeTeam ? game.homeTeam.name : 'Ev Sahibi';
        var homeCity = document.createElement('span');
        homeCity.className = 'team-city';
        homeCity.textContent = game.homeTeam ? game.homeTeam.city : '';
        homeText.append(homeName, homeCity);

        var homeBadge = document.createElement('span');
        homeBadge.className = 'badge';
        homeBadge.setAttribute('aria-hidden', 'true');
        if (game.homeTeam) {
            paintBadge(homeBadge, game.homeTeam);
        }

        homeSide.append(homeText, homeBadge);

        // Orta: Durum Rozeti ve VS
        var centerCol = document.createElement('div');
        centerCol.className = 'match-status-col';

        var vs = document.createElement('span');
        vs.className = 'vs-badge';
        vs.textContent = 'VS';

        var pill = document.createElement('span');
        pill.className = 'status-pill';
        if (status === 'FINISHED' || status === 'CONFIRMED') {
            pill.classList.add('finished');
            pill.textContent = STATUS_LABELS[status] || 'Oynandı';
        } else if (status === 'LIVE') {
            pill.classList.add('live');
            pill.textContent = STATUS_LABELS[status] || 'Canlı';
        } else {
            pill.classList.add('scheduled');
            pill.textContent = STATUS_LABELS[status] || 'Oynanmadı';
        }

        centerCol.append(vs, pill);
        if (!state.isDemo) {
            var detailButton = document.createElement('button');
            detailButton.type = 'button';
            detailButton.className = 'match-detail-button';
            detailButton.textContent = 'Maç detayı';
            detailButton.setAttribute('aria-label', game.homeTeam.name + ' - ' + game.awayTeam.name
                + ' maç detayını aç');
            detailButton.addEventListener('click', function () {
                showGameDetails(game.id);
            });
            centerCol.appendChild(detailButton);
        }

        // Deplasman Takımı
        var awaySide = document.createElement('div');
        awaySide.className = 'team-entry away';

        var awayBadge = document.createElement('span');
        awayBadge.className = 'badge';
        awayBadge.setAttribute('aria-hidden', 'true');
        if (game.awayTeam) {
            paintBadge(awayBadge, game.awayTeam);
        }

        var awayText = document.createElement('div');
        awayText.className = 'team-text';
        var awayName = document.createElement('span');
        awayName.className = 'team-name';
        awayName.textContent = game.awayTeam ? game.awayTeam.name : 'Deplasman';
        var awayCity = document.createElement('span');
        awayCity.className = 'team-city';
        awayCity.textContent = game.awayTeam ? game.awayTeam.city : '';
        awayText.append(awayName, awayCity);

        awaySide.append(awayBadge, awayText);

        card.append(homeSide, centerCol, awaySide);
        return card;
    }

    function appendText(parent, tagName, className, text) {
        var element = document.createElement(tagName);
        if (className) element.className = className;
        element.textContent = text;
        parent.appendChild(element);
        return element;
    }

    function renderQuarterScores(game) {
        var section = document.createElement('section');
        section.className = 'game-detail-section';
        appendText(section, 'h3', '', 'Çeyrek skorları');

        if (!game.quarters || game.quarters.length === 0) {
            appendText(section, 'p', 'game-detail-empty', 'Bu maç için henüz skor girilmedi.');
            return section;
        }

        var table = document.createElement('table');
        table.className = 'game-detail-table quarter-score-table';
        var head = table.createTHead().insertRow();
        appendText(head, 'th', '', 'Takım');
        game.quarters.forEach(function (quarter) {
            appendText(head, 'th', '', quarter.quarterNo + '. Ç');
        });
        appendText(head, 'th', '', 'Toplam');

        var body = table.createTBody();
        [
            { team: game.homeTeam, side: 'home' },
            { team: game.awayTeam, side: 'away' }
        ].forEach(function (entry) {
            var row = body.insertRow();
            appendText(row, 'th', '', entry.team.name);
            var total = 0;
            game.quarters.forEach(function (quarter) {
                var score = entry.side === 'home' ? quarter.homePoints : quarter.awayPoints;
                total += score;
                appendText(row, 'td', '', String(score));
            });
            appendText(row, 'td', 'score-total', String(total));
        });
        section.appendChild(table);
        return section;
    }

    function playersWithPoints(roster, scores, teamId) {
        var pointsByPlayer = Object.create(null);
        scores.forEach(function (score) {
            if (score.teamId === teamId) pointsByPlayer[score.playerId] = score.points;
        });

        var listedIds = Object.create(null);
        var players = roster.map(function (player) {
            listedIds[player.id] = true;
            return {
                jerseyNumber: player.jerseyNumber,
                name: player.firstName + ' ' + player.lastName,
                position: player.position,
                points: pointsByPlayer[player.id] || 0
            };
        });
        scores.forEach(function (score) {
            if (score.teamId === teamId && !listedIds[score.playerId]) {
                players.push({
                    jerseyNumber: '',
                    name: 'Oyuncu #' + score.playerId,
                    position: '-',
                    points: score.points
                });
            }
        });
        return players;
    }

    function renderPlayerScores(team, players) {
        var section = document.createElement('section');
        section.className = 'game-detail-section';
        appendText(section, 'h3', '', team.name + ' oyuncu sayıları');
        if (players.length === 0) {
            appendText(section, 'p', 'game-detail-empty', 'Oyuncu kadrosu bulunamadı.');
            return section;
        }

        var table = document.createElement('table');
        table.className = 'game-detail-table';
        var head = table.createTHead().insertRow();
        ['Oyuncu', 'Pozisyon', 'Sayı'].forEach(function (label) {
            appendText(head, 'th', '', label);
        });
        var body = table.createTBody();
        players.forEach(function (player) {
            var row = body.insertRow();
            appendText(row, 'td', '', (player.jerseyNumber === '' ? '' : '#' + player.jerseyNumber + ' ')
                + player.name);
            appendText(row, 'td', '', player.position);
            appendText(row, 'td', 'score-total', String(player.points));
        });
        section.appendChild(table);
        return section;
    }

    function renderGameDetail(result) {
        var game = result.game;
        var quarters = game.quarters || [];
        var homeTotal = game.homeScore;
        var awayTotal = game.awayScore;
        if (homeTotal === null && quarters.length > 0) {
            homeTotal = quarters.reduce(function (total, quarter) { return total + quarter.homePoints; }, 0);
            awayTotal = quarters.reduce(function (total, quarter) { return total + quarter.awayPoints; }, 0);
        }

        el.gameDetailContent.replaceChildren();
        el.gameDetailTitle.textContent = game.homeTeam.name + ' - ' + game.awayTeam.name;
        appendText(el.gameDetailContent, 'p', 'game-detail-meta',
            game.roundNumber + '. Hafta · ' + (STATUS_LABELS[game.status] || game.status));
        appendText(el.gameDetailContent, 'p', 'game-detail-score',
            homeTotal === null ? 'Skor girilmedi' : homeTotal + ' - ' + awayTotal);
        el.gameDetailContent.appendChild(renderQuarterScores(game));

        var playerGrid = document.createElement('div');
        playerGrid.className = 'player-score-grid';
        playerGrid.append(
            renderPlayerScores(game.homeTeam,
                playersWithPoints(result.homePlayers, game.playerPoints || [], game.homeTeam.id)),
            renderPlayerScores(game.awayTeam,
                playersWithPoints(result.awayPlayers, game.playerPoints || [], game.awayTeam.id))
        );
        el.gameDetailContent.appendChild(playerGrid);
    }

    function showGameDetails(gameId) {
        el.gameDetailDialog.showModal();
        el.gameDetailContent.replaceChildren();
        appendText(el.gameDetailContent, 'p', 'game-detail-empty', 'Maç bilgileri yükleniyor...');

        getJson('api/games/' + gameId)
            .then(function (game) {
                return Promise.all([
                    getTeamPlayers(game.homeTeam.id, 0, []),
                    getTeamPlayers(game.awayTeam.id, 0, [])
                ]).then(function (rosters) {
                    return { game: game, homePlayers: rosters[0], awayPlayers: rosters[1] };
                });
            })
            .then(renderGameDetail)
            .catch(function (error) {
                el.gameDetailContent.replaceChildren();
                appendText(el.gameDetailContent, 'p', 'result-entry-error',
                    'Maç bilgisi alınamadı: ' + error.message);
            });
    }

    function isGameMatchingFilter(game, filter) {
        var status = (game.status || 'SCHEDULED').toUpperCase();
        if (filter === 'ALL') {
            return true;
        }
        if (filter === 'SCHEDULED') {
            return status === 'SCHEDULED';
        }
        if (filter === 'FINISHED') {
            return status === 'FINISHED' || status === 'CONFIRMED';
        }
        if (filter === 'LIVE') {
            return status === 'LIVE';
        }
        return true;
    }

    function renderRoundSection(round) {
        var section = document.createElement('section');
        section.className = 'round-section';
        section.setAttribute('aria-labelledby', 'round-heading-' + round.number);

        var header = document.createElement('div');
        header.className = 'round-section-header';

        var title = document.createElement('h2');
        title.id = 'round-heading-' + round.number;
        title.className = 'round-section-title';
        title.textContent = round.number + '. Hafta';

        var filteredGames = round.games.filter(function (game) {
            return isGameMatchingFilter(game, state.filter);
        });

        var meta = document.createElement('span');
        meta.className = 'round-meta';
        meta.textContent = filteredGames.length + ' / ' + round.games.length + ' maç gösteriliyor';

        header.append(title, meta);
        section.appendChild(header);

        // Bay geçen takım varsa duyuru kutusu
        if (round.byeTeam) {
            var byeBox = document.createElement('div');
            byeBox.className = 'bye-banner';
            var byeBadge = document.createElement('span');
            byeBadge.className = 'badge';
            byeBadge.style.width = '1.75rem';
            byeBadge.style.height = '1.75rem';
            byeBadge.style.fontSize = '0.75rem';
            paintBadge(byeBadge, round.byeTeam);
            var byeText = document.createElement('span');
            byeText.textContent = round.byeTeam.name + ' bu haftayı bay geçmektedir.';
            byeBox.append(byeBadge, byeText);
            section.appendChild(byeBox);
        }

        // Maçlar
        if (filteredGames.length === 0) {
            var empty = document.createElement('p');
            empty.className = 'notice';
            empty.textContent = 'Bu haftada seçilen filtreye uygun maç bulunmamaktadır.';
            section.appendChild(empty);
        } else {
            var grid = document.createElement('div');
            grid.className = 'matches-grid';
            filteredGames.forEach(function (game) {
                grid.appendChild(createMatchCard(game));
            });
            section.appendChild(grid);
        }

        return section;
    }

    function renderToolbar() {
        el.roundsNav.replaceChildren();

        state.rounds.forEach(function (round) {
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'round-tab';
            btn.textContent = round.number + '. Hafta';
            btn.setAttribute('role', 'tab');
            var isSelected = !state.showAllRounds && round.number === state.currentRoundNumber;
            btn.setAttribute('aria-selected', isSelected ? 'true' : 'false');
            btn.dataset.roundNumber = round.number;
            btn.addEventListener('click', function () {
                state.showAllRounds = false;
                state.currentRoundNumber = round.number;
                render();
                btn.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' });
            });
            el.roundsNav.appendChild(btn);
        });

        el.prevRoundBtn.disabled = state.showAllRounds || state.currentRoundNumber <= 1;
        el.nextRoundBtn.disabled = state.showAllRounds || state.currentRoundNumber >= state.rounds.length;

        if (state.showAllRounds) {
            el.toggleAllBtn.classList.add('active');
            el.toggleAllBtn.textContent = 'Haftalık Görünüm';
        } else {
            el.toggleAllBtn.classList.remove('active');
            el.toggleAllBtn.textContent = 'Tüm Haftalar';
        }
    }

    function render() {
        renderToolbar();
        el.fixturesContainer.replaceChildren();

        if (state.rounds.length === 0) {
            showNotice(el.notice, 'Fikstür bulunamadı veya henüz oluşturulmadı.');
            return;
        }

        el.notice.hidden = true;

        if (state.showAllRounds) {
            state.rounds.forEach(function (round) {
                el.fixturesContainer.appendChild(renderRoundSection(round));
            });
        } else {
            var activeRound = state.rounds.find(function (r) {
                return r.number === state.currentRoundNumber;
            });
            if (activeRound) {
                el.fixturesContainer.appendChild(renderRoundSection(activeRound));
            }
        }
    }

    function setupEvents() {
        el.gameDetailClose.addEventListener('click', function () {
            el.gameDetailDialog.close();
        });
        el.gameDetailDialog.addEventListener('click', function (event) {
            if (event.target === el.gameDetailDialog) el.gameDetailDialog.close();
        });

        el.prevRoundBtn.addEventListener('click', function () {
            if (state.currentRoundNumber > 1) {
                state.showAllRounds = false;
                state.currentRoundNumber--;
                render();
            }
        });

        el.nextRoundBtn.addEventListener('click', function () {
            if (state.currentRoundNumber < state.rounds.length) {
                state.showAllRounds = false;
                state.currentRoundNumber++;
                render();
            }
        });

        el.toggleAllBtn.addEventListener('click', function () {
            state.showAllRounds = !state.showAllRounds;
            render();
        });

        el.filterButtons.forEach(function (btn) {
            btn.addEventListener('click', function () {
                el.filterButtons.forEach(function (b) { b.classList.remove('active'); });
                btn.classList.add('active');
                state.filter = btn.dataset.filter;
                render();
            });
        });
    }

    function init() {
        setupEvents();
        el.notice.hidden = false;
        el.notice.textContent = 'Fikstür yükleniyor...';

        // 1. Önce aktif sezonları yüklemeyi dene
        getJson(API_SEASONS)
            .then(function (seasonsData) {
                var seasonList = seasonsData.items || seasonsData;
                if (!seasonList || seasonList.length === 0) {
                    throw new Error('Aktif sezon bulunamadı.');
                }
                var targetSeason = seasonList[0];
                state.season = targetSeason;
                el.seasonTitle.textContent = targetSeason.name + ' Sezonu';

                // 2. Sezon fikstürünü API'den al
                return getJson(API_SEASONS + '/' + targetSeason.id + '/fixtures');
            })
            .then(function (fixtureData) {
                if (fixtureData && fixtureData.rounds && fixtureData.rounds.length > 0) {
                    state.isDemo = false;
                    state.rounds = fixtureData.rounds;
                    state.currentRoundNumber = 1;
                    render();
                } else {
                    showNotice(el.notice, 'Bu sezon için henüz fikstür üretilmedi.');
                }
            })
            .catch(function () {
                // Backend API henüz mevcut değilse veya 404 aldıysa,
                // Takımlar API'sinden takımları alıp ADR-0008 Circle Method ile demo fikstür oluştur
                getJson(API_TEAMS + '?size=100')
                    .then(function (teamsData) {
                        var teams = teamsData.items || teamsData;
                        if (!teams || teams.length < 2) {
                            throw new Error('Fikstür için yeterli takım bulunamadı.');
                        }
                        el.seasonTitle.textContent = 'EuroLeague 2025-26 Sezonu (Demo Fikstür)';
                        state.isDemo = true;
                        state.rounds = generateCircleMethodFixture(teams);
                        state.currentRoundNumber = 1;
                        render();
                    })
                    .catch(function (err) {
                        showNotice(el.notice, 'Fikstür bilgisi alınamadı: ' + err.message, init);
                    });
            });
    }

    init();
}());
