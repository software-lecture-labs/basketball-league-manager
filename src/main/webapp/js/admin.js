/*
 * Yönetici paneli. Takım, oyuncu ve sezon işlemlerini REST API'ye iletir.
 * Kullanıcıdan ve sunucudan gelen metinler DOM'a textContent ile yazılır.
 */
(function () {
    'use strict';

    var API_TEAMS = 'api/teams';
    var API_SEASONS = 'api/seasons';
    var PAGE_SIZE = 100;
    var MAX_SEASON_TEAMS = 20;

    var state = {
        teams: [],
        seasons: [],
        seasonTeams: [],
        selectedSeasonId: null,
        seasonTeamsLoading: false,
        seasonRequestToken: 0
    };

    var el = {
        teamForm: document.getElementById('team-form'),
        teamFeedback: document.getElementById('team-feedback'),
        playerForm: document.getElementById('player-form'),
        playerTeam: document.getElementById('player-team'),
        playerSubmit: document.getElementById('player-submit'),
        playerFeedback: document.getElementById('player-feedback'),
        seasonForm: document.getElementById('season-form'),
        seasonFeedback: document.getElementById('season-feedback'),
        seasonSelect: document.getElementById('season-select'),
        seasonTeamForm: document.getElementById('season-team-form'),
        seasonTeamSelect: document.getElementById('season-team-select'),
        seasonTeamSubmit: document.getElementById('season-team-submit'),
        seasonTeamFeedback: document.getElementById('season-team-feedback'),
        seasonTeamList: document.getElementById('season-team-list'),
        seasonTeamCount: document.getElementById('season-team-count'),
        seasonTeamEmpty: document.getElementById('season-team-empty'),
        fixtureForm: document.getElementById('fixture-form'),
        fixtureSubmit: document.getElementById('fixture-submit'),
        fixtureFeedback: document.getElementById('fixture-feedback')
    };

    function requestJson(url, method, body) {
        var options = {
            method: method,
            headers: { Accept: 'application/json' }
        };
        if (body !== undefined) {
            options.headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify(body);
        }
        return fetch(url, options).then(function (response) {
            return response.json().catch(function () { return null; }).then(function (data) {
                if (!response.ok) {
                    var detail = data && data.detail ? data.detail
                        : 'Sunucu ' + response.status + ' yanıtı verdi.';
                    throw new Error(detail);
                }
                return data;
            });
        }, function () {
            throw new Error('Sunucuya ulaşılamadı; uygulamanın çalıştığını kontrol edin.');
        });
    }

    function loadAllTeams(page, collected) {
        return requestJson(API_TEAMS + '?page=' + page + '&size=' + PAGE_SIZE, 'GET')
            .then(function (response) {
                var teams = collected.concat(response.items);
                return teams.length < response.totalItems && response.items.length > 0
                    ? loadAllTeams(page + 1, teams) : teams;
            });
    }

    function loadAllSeasons(page, collected) {
        return requestJson(API_SEASONS + '?page=' + page + '&size=' + PAGE_SIZE, 'GET')
            .then(function (response) {
                var seasons = collected.concat(response.items);
                return seasons.length < response.totalItems && response.items.length > 0
                    ? loadAllSeasons(page + 1, seasons) : seasons;
            });
    }

    function setFeedback(node, message, type) {
        node.textContent = message;
        node.className = 'admin-feedback' + (type ? ' ' + type : '');
        node.hidden = !message;
    }

    function appendOption(select, value, label, disabled) {
        var option = document.createElement('option');
        option.value = value;
        option.textContent = label;
        option.disabled = Boolean(disabled);
        select.appendChild(option);
    }

    function renderPlayerTeamOptions(selectedTeamId) {
        el.playerTeam.replaceChildren();
        appendOption(el.playerTeam, '', state.teams.length ? 'Takım seçin' : 'Önce takım ekleyin', true);
        state.teams.forEach(function (team) {
            appendOption(el.playerTeam, team.id, team.name + ' · ' + team.city);
        });
        el.playerTeam.disabled = state.teams.length === 0;
        el.playerSubmit.disabled = state.teams.length === 0;
        if (selectedTeamId !== undefined && selectedTeamId !== null) {
            el.playerTeam.value = String(selectedTeamId);
        }
    }

    function renderSeasonOptions(selectedSeasonId) {
        var drafts = state.seasons.filter(function (season) { return season.status === 'DRAFT'; });
        el.seasonSelect.replaceChildren();
        appendOption(el.seasonSelect, '', drafts.length ? 'Taslak sezon seçin' : 'Taslak sezon yok', true);
        drafts.forEach(function (season) {
            appendOption(el.seasonSelect, season.id, season.name);
        });
        el.seasonSelect.disabled = drafts.length === 0;

        if (selectedSeasonId !== undefined && selectedSeasonId !== null
                && drafts.some(function (season) { return season.id === selectedSeasonId; })) {
            el.seasonSelect.value = String(selectedSeasonId);
        } else if (drafts.length > 0) {
            el.seasonSelect.value = String(drafts[0].id);
        }
        // Hata loadSeasonTeams içinde yakalanıp gösteriliyor; dönen Promise bilerek beklenmiyor.
        void selectSeason(el.seasonSelect.value);
    }

    function renderSeasonTeamOptions() {
        el.seasonTeamSelect.replaceChildren();
        appendOption(el.seasonTeamSelect, '', state.teams.length ? 'Takım seçin' : 'Önce takım ekleyin', true);
        var members = Object.create(null);
        state.seasonTeams.forEach(function (team) { members[team.id] = true; });
        state.teams.forEach(function (team) {
            appendOption(el.seasonTeamSelect, team.id, team.name, members[team.id]);
        });

        var selectedSeason = state.seasons.find(function (season) {
            return season.id === state.selectedSeasonId;
        });
        var canAddTeam = Boolean(selectedSeason && selectedSeason.status === 'DRAFT'
            && !state.seasonTeamsLoading && el.seasonTeamForm.dataset.submitting !== 'true'
            && state.teams.length > 0 && state.seasonTeams.length < MAX_SEASON_TEAMS);
        el.seasonTeamSelect.disabled = !canAddTeam;
        el.seasonTeamSubmit.disabled = !canAddTeam;

        if (canAddTeam) {
            var firstAvailable = state.teams.find(function (team) { return !members[team.id]; });
            if (firstAvailable) el.seasonTeamSelect.value = String(firstAvailable.id);
        }
    }

    function renderSeasonTeams() {
        el.seasonTeamList.replaceChildren();
        state.seasonTeams.forEach(function (team) {
            var item = document.createElement('li');
            item.textContent = team.name;
            el.seasonTeamList.appendChild(item);
        });
        el.seasonTeamCount.textContent = '(' + state.seasonTeams.length + '/' + MAX_SEASON_TEAMS + ')';
        el.seasonTeamEmpty.hidden = state.seasonTeams.length > 0;
        el.seasonTeamList.hidden = state.seasonTeams.length === 0;
        renderSeasonTeamOptions();
        el.fixtureSubmit.disabled = !state.selectedSeasonId || state.seasonTeams.length < 2
            || el.fixtureForm.dataset.submitting === 'true';
    }

    function loadSeasonTeams(seasonId) {
        var token = ++state.seasonRequestToken;
        if (!seasonId) {
            state.selectedSeasonId = null;
            state.seasonTeamsLoading = false;
            state.seasonTeams = [];
            renderSeasonTeams();
            el.seasonTeamEmpty.textContent = 'Taslak sezon oluşturup seçin.';
            return Promise.resolve();
        }
        state.selectedSeasonId = Number(seasonId);
        state.seasonTeamsLoading = true;
        state.seasonTeams = [];
        renderSeasonTeams();
        setFeedback(el.seasonTeamFeedback, '', '');

        return requestJson(API_SEASONS + '/' + seasonId + '/teams', 'GET')
            .then(function (teams) {
                if (token === state.seasonRequestToken) {
                    state.seasonTeamsLoading = false;
                    state.seasonTeams = teams;
                    renderSeasonTeams();
                    el.seasonTeamEmpty.textContent = 'Bu sezona henüz takım eklenmedi.';
                    return true;
                }
                return false;
            })
            .catch(function (error) {
                if (token === state.seasonRequestToken) {
                    state.seasonTeamsLoading = false;
                    renderSeasonTeams();
                    el.seasonTeamEmpty.textContent = 'Sezon takımları yüklenemedi.';
                    setFeedback(el.seasonTeamFeedback, 'Sezon takımları yüklenemedi. ' + error.message, 'error');
                }
                return false;
            });
    }

    function selectSeason(seasonId) {
        return loadSeasonTeams(seasonId);
    }

    function refreshTeams(selectedTeamId) {
        return loadAllTeams(0, []).then(function (teams) {
            state.teams = teams;
            renderPlayerTeamOptions(selectedTeamId);
            renderSeasonTeamOptions();
        });
    }

    function refreshSeasons(selectedSeasonId) {
        return loadAllSeasons(0, [])
            .then(function (seasons) {
                state.seasons = seasons;
                renderSeasonOptions(selectedSeasonId);
            });
    }

    function setSubmitting(form, isSubmitting) {
        var button = form.querySelector('button[type="submit"]');
        button.disabled = isSubmitting;
        form.dataset.submitting = isSubmitting ? 'true' : 'false';
        button.dataset.submitting = isSubmitting ? 'true' : 'false';
        if (isSubmitting) button.textContent = 'Kaydediliyor...';
    }

    function restoreButton(form, defaultText, disabled) {
        var button = form.querySelector('button[type="submit"]');
        button.textContent = defaultText;
        button.disabled = disabled;
        form.dataset.submitting = 'false';
        button.dataset.submitting = 'false';
    }

    el.teamForm.addEventListener('submit', function (event) {
        event.preventDefault();
        var form = event.currentTarget;
        var buttonText = 'Takımı kaydet';
        setFeedback(el.teamFeedback, '', '');
        setSubmitting(form, true);

        requestJson(API_TEAMS, 'POST', {
            name: form.elements.name.value,
            city: form.elements.city.value
        }).then(function (team) {
            form.reset();
            setFeedback(el.teamFeedback, team.name + ' takımı eklendi.', 'success');
            return refreshTeams(team.id).catch(function (error) {
                setFeedback(el.teamFeedback,
                    'Takım eklendi ancak takım listesi yenilenemedi. ' + error.message, 'error');
            });
        }).catch(function (error) {
            setFeedback(el.teamFeedback, 'Takım eklenemedi. ' + error.message, 'error');
        }).finally(function () {
            restoreButton(form, buttonText, false);
        });
    });

    el.playerForm.addEventListener('submit', function (event) {
        event.preventDefault();
        var form = event.currentTarget;
        var teamId = Number(el.playerTeam.value);
        var team = state.teams.find(function (item) { return item.id === teamId; });
        var buttonText = 'Oyuncuyu kaydet';
        setFeedback(el.playerFeedback, '', '');
        setSubmitting(form, true);

        requestJson(API_TEAMS + '/' + teamId + '/players', 'POST', {
            firstName: form.elements.firstName.value,
            lastName: form.elements.lastName.value,
            jerseyNumber: Number(form.elements.jerseyNumber.value),
            position: form.elements.position.value
        }).then(function (player) {
            form.reset();
            renderPlayerTeamOptions(teamId);
            setFeedback(el.playerFeedback,
                player.firstName + ' ' + player.lastName + ' oyuncusu ' + team.name + ' kadrosuna eklendi.',
                'success');
        }).catch(function (error) {
            setFeedback(el.playerFeedback, 'Oyuncu eklenemedi. ' + error.message, 'error');
        }).finally(function () {
            restoreButton(form, buttonText, state.teams.length === 0);
        });
    });

    el.seasonForm.addEventListener('submit', function (event) {
        event.preventDefault();
        var form = event.currentTarget;
        var buttonText = 'Sezonu oluştur';
        setFeedback(el.seasonFeedback, '', '');
        setSubmitting(form, true);

        requestJson(API_SEASONS, 'POST', {
            name: form.elements.name.value,
            startDate: form.elements.startDate.value
        }).then(function (season) {
            form.reset();
            setFeedback(el.seasonFeedback, season.name + ' taslak sezonu oluşturuldu.', 'success');
            return refreshSeasons(season.id).catch(function (error) {
                setFeedback(el.seasonFeedback,
                    'Sezon oluşturuldu ancak sezon listesi yenilenemedi. ' + error.message, 'error');
            });
        }).catch(function (error) {
            setFeedback(el.seasonFeedback, 'Sezon oluşturulamadı. ' + error.message, 'error');
        }).finally(function () {
            restoreButton(form, buttonText, false);
        });
    });

    el.seasonSelect.addEventListener('change', function () {
        void selectSeason(el.seasonSelect.value);
    });

    el.seasonTeamForm.addEventListener('submit', function (event) {
        event.preventDefault();
        var form = event.currentTarget;
        var seasonId = state.selectedSeasonId;
        var teamId = Number(el.seasonTeamSelect.value);
        var team = state.teams.find(function (item) { return item.id === teamId; });
        var buttonText = 'Takımı sezona ekle';
        setFeedback(el.seasonTeamFeedback, '', '');
        setSubmitting(form, true);

        requestJson(API_SEASONS + '/' + seasonId + '/teams', 'POST', { teamId: teamId })
            .then(function () {
                return loadSeasonTeams(seasonId).then(function (loaded) {
                    if (loaded) {
                        setFeedback(el.seasonTeamFeedback, team.name + ' sezona eklendi.', 'success');
                    } else {
                        setFeedback(el.seasonTeamFeedback,
                            'Takım sezona eklendi ancak sezon listesi yenilenemedi. Sayfayı yenileyip kontrol edin.',
                            'error');
                    }
                });
            }).catch(function (error) {
                setFeedback(el.seasonTeamFeedback, 'Takım eklenemedi. ' + error.message, 'error');
            }).finally(function () {
                restoreButton(form, buttonText, false);
                renderSeasonTeamOptions();
            });
    });

    el.fixtureForm.addEventListener('submit', function (event) {
        event.preventDefault();
        var form = event.currentTarget;
        var season = state.seasons.find(function (item) { return item.id === state.selectedSeasonId; });
        var confirmationMessage = 'Fikstür oluşturulunca sezon başlayacak ve sezon takımları değiştirilemeyecek.'
            + ' Devam edilsin mi?';
        if (!window.confirm(confirmationMessage)) {
            return;
        }
        var buttonText = 'Fikstür oluştur';
        setFeedback(el.fixtureFeedback, '', '');
        setSubmitting(form, true);

        requestJson(API_SEASONS + '/' + state.selectedSeasonId + '/fixtures', 'POST')
            .then(function (fixture) {
                var gameCount = fixture.rounds.reduce(function (total, round) {
                    return total + round.games.length;
                }, 0);
                setFeedback(el.fixtureFeedback,
                    season.name + ' için ' + fixture.rounds.length + ' hafta ve ' + gameCount
                        + ' maçlık fikstür oluşturuldu.',
                    'success');
                return refreshSeasons(null).catch(function (error) {
                    setFeedback(el.fixtureFeedback,
                        'Fikstür oluşturuldu ancak sezon listesi yenilenemedi. ' + error.message, 'error');
                });
            }).catch(function (error) {
                setFeedback(el.fixtureFeedback, 'Fikstür oluşturulamadı. ' + error.message, 'error');
            }).finally(function () {
                restoreButton(form, buttonText, false);
                el.fixtureSubmit.disabled = !state.selectedSeasonId || state.seasonTeams.length < 2;
            });
    });

    // Her yükleme kendi hatasını yakalayıp formun altında gösteriyor.
    void Promise.all([
        refreshTeams().catch(function (error) {
            setFeedback(el.teamFeedback, 'Takımlar yüklenemedi. ' + error.message, 'error');
            setFeedback(el.playerFeedback, 'Takım listesi yüklenemedi. ' + error.message, 'error');
        }),
        refreshSeasons().catch(function (error) {
            setFeedback(el.seasonFeedback, 'Sezonlar yüklenemedi. ' + error.message, 'error');
            setFeedback(el.seasonTeamFeedback, 'Sezon listesi yüklenemedi. ' + error.message, 'error');
        })
    ]);
}());
