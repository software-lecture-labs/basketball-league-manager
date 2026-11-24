package com.basketballleague.service;

import com.basketballleague.domain.Fixture;
import com.basketballleague.domain.FixtureGenerator;
import com.basketballleague.domain.Pairing;
import com.basketballleague.domain.Round;
import com.basketballleague.domain.SeasonNotFoundException;
import com.basketballleague.dto.FixtureResponse;
import com.basketballleague.entity.Game;
import com.basketballleague.entity.Season;
import com.basketballleague.repository.GameRepository;
import com.basketballleague.repository.SeasonRepository;
import com.basketballleague.repository.TeamRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates a season's double round-robin fixture and reads it back. The algorithm itself is the pure
 * {@link FixtureGenerator} (ADR-0008); this class turns its pairings into stored games.
 */
@ApplicationScoped
public class FixtureService {

    private final SeasonRepository seasonRepository;
    private final GameRepository gameRepository;
    private final TeamRepository teamRepository;
    private final FixtureGenerator fixtureGenerator;

    /** Required by CDI to create the proxy; never used to serve calls. */
    protected FixtureService() {
        this.seasonRepository = null;
        this.gameRepository = null;
        this.teamRepository = null;
        this.fixtureGenerator = null;
    }

    /**
     * The generator is created here rather than injected: it is stateless, has no dependencies and gives the same
     * fixture for the same teams, so tests use the real one instead of a mock.
     */
    @Inject
    public FixtureService(SeasonRepository seasonRepository, GameRepository gameRepository,
            TeamRepository teamRepository) {
        this.seasonRepository = seasonRepository;
        this.gameRepository = gameRepository;
        this.teamRepository = teamRepository;
        this.fixtureGenerator = new FixtureGenerator();
    }

    /**
     * Starts the season and stores all its games in one transaction. The season row is locked first, so a second
     * request for the same season waits, then finds the season started and fails with 409. If anything fails, no
     * game is stored and the season stays a draft.
     */
    @Transactional
    public FixtureResponse generateFixture(long seasonId) {
        final Season season = seasonRepository.findByIdForUpdate(seasonId)
                .orElseThrow(() -> new SeasonNotFoundException(seasonId));
        season.activate();
        final List<Long> teamIds = season.getTeamIds();
        final List<Game> games = gameRepository.saveAll(toGames(seasonId, fixtureGenerator.generate(teamIds)));
        return FixtureMapper.toResponse(seasonId, games, teamRepository.findByIds(teamIds));
    }

    /**
     * Four queries whatever the size of the fixture: the season, its team ids, its teams and its games.
     */
    public FixtureResponse getFixture(long seasonId) {
        seasonRepository.findById(seasonId).orElseThrow(() -> new SeasonNotFoundException(seasonId));
        final List<Long> teamIds = seasonRepository.findTeamIds(seasonId);
        return FixtureMapper.toResponse(seasonId, gameRepository.findBySeasonId(seasonId),
                teamRepository.findByIds(teamIds));
    }

    private static List<Game> toGames(long seasonId, Fixture fixture) {
        final List<Game> games = new ArrayList<>();
        for (Round round : fixture.rounds()) {
            for (Pairing pairing : round.pairings()) {
                games.add(new Game(seasonId, round.number(), pairing.homeTeamId(), pairing.awayTeamId()));
            }
        }
        return games;
    }
}
