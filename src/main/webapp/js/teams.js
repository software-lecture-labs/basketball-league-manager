/*
 * Takımlar sayfası. Veriyi REST API'den alır (sözleşme: docs/api.md); demo veriye geri düşmez.
 * Kullanıcıdan veya sunucudan gelen metin her zaman textContent ile yazılır.
 */
(function () {
    'use strict';

    var API = 'api/teams';
    var PAGE_SIZE = 100;
    var HASH_PREFIX = '#takim-';
    var POSITIONS = {
        PG: 'Oyun kurucu',
        SG: 'Şutör guard',
        SF: 'Kısa forvet',
        PF: 'Uzun forvet',
        C: 'Pivot'
    };
    var collator = new Intl.Collator('tr', { sensitivity: 'base' });

    var teams = [];
    var selectedId = null;
    var selectionToken = 0;
    var seasonSelectionToken = 0;

    var el = {
        list: document.getElementById('team-list'),
        count: document.getElementById('team-count'),
        search: document.getElementById('team-search'),
        teamNotice: document.getElementById('team-notice'),
        sheet: document.getElementById('sheet'),
        sheetEmpty: document.getElementById('sheet-empty'),
        sheetTeam: document.getElementById('sheet-team'),
        badge: document.getElementById('sheet-badge'),
        name: document.getElementById('sheet-name'),
        city: document.getElementById('sheet-city'),
        roster: document.getElementById('roster'),
        rosterBody: document.getElementById('roster-body'),
        rosterNotice: document.getElementById('roster-notice'),
        seasonPanel: document.getElementById('team-season-panel'),
        seasonSelect: document.getElementById('team-season-select'),
        seasonNotice: document.getElementById('team-season-notice'),
        seasonContent: document.getElementById('team-season-content'),
        seasonRank: document.getElementById('season-rank'),
        seasonPlayed: document.getElementById('season-played'),
        seasonWon: document.getElementById('season-won'),
        seasonLost: document.getElementById('season-lost'),
        seasonPointDiff: document.getElementById('season-point-diff'),
        seasonPointsFor: document.getElementById('season-points-for'),
        seasonPointsAgainst: document.getElementById('season-points-against'),
        seasonLeaguePoints: document.getElementById('season-league-points'),
        recentGames: document.getElementById('recent-games'),
        recentGamesEmpty: document.getElementById('recent-games-empty'),
        rowTemplate: document.getElementById('team-row')
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

    // Lig 100 takımdan büyük olursa sayfaları sırayla toplar.
    function loadAllTeams(page, collected) {
        return getJson(API + '?page=' + page + '&size=' + PAGE_SIZE).then(function (body) {
            var all = collected.concat(body.items);
            return all.length < body.totalItems && body.items.length > 0 ? loadAllTeams(page + 1, all) : all;
        });
    }

    // "FC", "EA7", "Tel" gibi kısa ekler rozeti ayırt etmeye yaramaz; tek kelimelik adlarda üç harf kullanılır.
    function initials(name) {
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

    // Resmî renk kullanılmaz; ton takım adından türetilir, beyaz yazı için koyu tutulur.
    function badgeColor(name) {
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
        node.textContent = message;
        if (retry) {
            var button = document.createElement('button');
            button.type = 'button';
            button.textContent = 'Tekrar dene';
            button.addEventListener('click', retry);
            node.appendChild(button);
        }
        node.hidden = false;
    }

    function matches(team, query) {
        var haystack = (team.name + ' ' + team.city).toLocaleLowerCase('tr');
        return haystack.indexOf(query) !== -1;
    }

    function renderList() {
        var query = el.search.value.trim().toLocaleLowerCase('tr');
        var visible = teams.filter(function (team) { return matches(team, query); });

        el.list.replaceChildren();
        visible.forEach(function (team) {
            var row = el.rowTemplate.content.cloneNode(true);
            var button = row.querySelector('button');
            paintBadge(row.querySelector('.badge'), team);
            row.querySelector('.team-row-name').textContent = team.name;
            row.querySelector('.team-row-city').textContent = team.city;
            button.dataset.teamId = team.id;
            if (team.id === selectedId) {
                button.setAttribute('aria-current', 'true');
            }
            el.list.appendChild(row);
        });

        el.count.textContent = query ? visible.length + ' / ' + teams.length + ' takım' : teams.length + ' takım';
        el.teamNotice.hidden = visible.length > 0;
        if (visible.length === 0) {
            el.teamNotice.textContent = teams.length === 0
                ? 'Henüz takım eklenmedi.'
                : '"' + el.search.value.trim() + '" ile eşleşen takım yok.';
        }
    }

    function renderRoster(players) {
        el.rosterBody.replaceChildren();
        players.forEach(function (player) {
            var tr = document.createElement('tr');

            var number = document.createElement('td');
            var jersey = document.createElement('span');
            jersey.className = 'jersey';
            jersey.textContent = player.jerseyNumber;
            number.appendChild(jersey);

            var name = document.createElement('td');
            name.className = 'player-name';
            name.textContent = player.firstName + ' ' + player.lastName;

            var position = document.createElement('td');
            var code = document.createElement('span');
            code.className = 'position-code';
            code.textContent = player.position;
            var label = document.createElement('span');
            label.className = 'position-name';
            label.textContent = POSITIONS[player.position] || '';
            position.append(code, label);

            tr.append(number, name, position);
            el.rosterBody.appendChild(tr);
        });
        el.roster.hidden = players.length === 0;
        if (players.length === 0) {
            showNotice(el.rosterNotice, 'Bu takımın kadrosu henüz girilmedi.');
        }
    }

    function renderSeasonOverview(overview) {
        var summary = overview.summary;
        el.seasonRank.textContent = summary.rank + '.';
        el.seasonPlayed.textContent = summary.played;
        el.seasonWon.textContent = summary.won;
        el.seasonLost.textContent = summary.lost;
        el.seasonPointDiff.textContent = summary.pointDiff;
        el.seasonPointsFor.textContent = summary.pointsFor;
        el.seasonPointsAgainst.textContent = summary.pointsAgainst;
        el.seasonLeaguePoints.textContent = summary.leaguePoints;
        el.recentGames.replaceChildren();

        overview.lastFiveGames.forEach(function (game) {
            var isHome = game.homeTeam.id === selectedId;
            var teamScore = isHome ? game.homeScore : game.awayScore;
            var opponentScore = isHome ? game.awayScore : game.homeScore;
            var won = teamScore > opponentScore;
            var item = document.createElement('li');
            item.className = 'recent-game ' + (won ? 'won' : 'lost');

            var round = document.createElement('span');
            round.className = 'recent-game-round';
            round.textContent = game.roundNumber + '. hafta';

            var opponent = document.createElement('span');
            opponent.className = 'recent-game-opponent';
            opponent.textContent = isHome ? game.awayTeam.name : game.homeTeam.name;

            var venue = document.createElement('span');
            venue.className = 'recent-game-venue';
            venue.textContent = isHome ? 'İç saha' : 'Deplasman';

            var score = document.createElement('span');
            score.className = 'recent-game-score';
            score.textContent = teamScore + ' - ' + opponentScore;

            var result = document.createElement('span');
            result.className = 'recent-game-result';
            result.textContent = won ? 'G' : 'M';
            result.setAttribute('aria-label', won ? 'Galibiyet' : 'Mağlubiyet');

            item.append(round, opponent, venue, score, result);
            el.recentGames.appendChild(item);
        });

        el.recentGames.hidden = overview.lastFiveGames.length === 0;
        el.recentGamesEmpty.hidden = overview.lastFiveGames.length > 0;
        el.seasonNotice.hidden = true;
        el.seasonContent.hidden = false;
    }

    function loadSeasonOverview(teamId, seasonId, teamToken) {
        var requestToken = ++seasonSelectionToken;
        el.seasonContent.hidden = true;
        el.seasonNotice.hidden = true;

        getJson(API + '/' + teamId + '/seasons/' + seasonId + '/overview')
            .then(function (overview) {
                if (teamToken === selectionToken && requestToken === seasonSelectionToken) {
                    renderSeasonOverview(overview);
                }
            })
            .catch(function (error) {
                if (teamToken === selectionToken && requestToken === seasonSelectionToken) {
                    showNotice(el.seasonNotice, 'Sezon özeti yüklenemedi. ' + error.message, function () {
                        loadSeasonOverview(teamId, seasonId, teamToken);
                    });
                }
            });
    }

    function loadTeamSeasons(teamId, teamToken) {
        el.seasonPanel.hidden = false;
        el.seasonContent.hidden = true;
        el.seasonNotice.hidden = true;
        el.seasonSelect.disabled = true;
        el.seasonSelect.replaceChildren();
        var loading = document.createElement('option');
        loading.value = '';
        loading.textContent = 'Sezonlar yükleniyor...';
        loading.disabled = true;
        el.seasonSelect.appendChild(loading);

        getJson(API + '/' + teamId + '/seasons')
            .then(function (seasons) {
                if (teamToken !== selectionToken) {
                    return;
                }
                el.seasonSelect.replaceChildren();
                if (seasons.length === 0) {
                    var empty = document.createElement('option');
                    empty.value = '';
                    empty.textContent = 'Sezon bulunamadı';
                    empty.disabled = true;
                    el.seasonSelect.appendChild(empty);
                    showNotice(el.seasonNotice, 'Bu takım henüz bir sezona eklenmemiş.');
                    return;
                }

                seasons.forEach(function (season) {
                    var option = document.createElement('option');
                    option.value = season.id;
                    option.textContent = season.name;
                    el.seasonSelect.appendChild(option);
                });
                el.seasonSelect.disabled = false;

                var defaultSeason = seasons.find(function (season) { return season.status === 'ACTIVE'; })
                    || seasons[0];
                el.seasonSelect.value = String(defaultSeason.id);
                loadSeasonOverview(teamId, defaultSeason.id, teamToken);
            })
            .catch(function (error) {
                if (teamToken === selectionToken) {
                    showNotice(el.seasonNotice, 'Sezonlar yüklenemedi. ' + error.message, function () {
                        loadTeamSeasons(teamId, teamToken);
                    });
                }
            });
    }

    function selectTeam(teamId, moveFocus) {
        var team = teams.find(function (candidate) { return candidate.id === teamId; });
        if (!team) {
            return;
        }
        selectedId = teamId;
        var token = ++selectionToken;
        seasonSelectionToken++;
        history.replaceState(null, '', HASH_PREFIX + teamId);
        renderList();

        el.sheetEmpty.hidden = true;
        el.sheetTeam.hidden = false;
        paintBadge(el.badge, team);
        el.name.textContent = team.name;
        el.city.textContent = team.city;
        el.roster.hidden = true;
        el.rosterNotice.hidden = true;
        el.rosterBody.replaceChildren();
        el.seasonPanel.hidden = true;
        el.seasonContent.hidden = true;
        el.seasonNotice.hidden = true;

        if (moveFocus && window.matchMedia('(max-width: 760px)').matches) {
            el.sheet.focus();
            el.sheet.scrollIntoView({ block: 'start' });
        }

        getJson(API + '/' + teamId + '/players?size=' + PAGE_SIZE)
            .then(function (body) {
                if (token === selectionToken) {
                    renderRoster(body.items);
                }
            })
            .catch(function (error) {
                if (token === selectionToken) {
                    showNotice(el.rosterNotice, 'Kadro yüklenemedi. ' + error.message, function () {
                        selectTeam(teamId, false);
                    });
                }
            });

        loadTeamSeasons(teamId, token);
    }

    function teamIdFromHash() {
        if (location.hash.indexOf(HASH_PREFIX) !== 0) {
            return null;
        }
        var id = Number(location.hash.slice(HASH_PREFIX.length));
        return Number.isInteger(id) ? id : null;
    }

    function start() {
        el.teamNotice.hidden = true;
        el.count.textContent = 'Yükleniyor';
        loadAllTeams(0, [])
            .then(function (loaded) {
                teams = loaded.sort(function (a, b) { return collator.compare(a.name, b.name); });
                renderList();
                var fromHash = teamIdFromHash();
                if (fromHash !== null) {
                    selectTeam(fromHash, false);
                }
            })
            .catch(function (error) {
                el.count.textContent = '';
                showNotice(el.teamNotice, 'Takımlar yüklenemedi. ' + error.message, start);
            });
    }

    el.search.addEventListener('input', renderList);
    el.list.addEventListener('click', function (event) {
        var button = event.target.closest('button[data-team-id]');
        if (button) {
            selectTeam(Number(button.dataset.teamId), true);
        }
    });
    el.seasonSelect.addEventListener('change', function () {
        if (selectedId && el.seasonSelect.value) {
            loadSeasonOverview(selectedId, Number(el.seasonSelect.value), selectionToken);
        }
    });

    start();
}());
