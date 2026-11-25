package com.basketballleague.service;

import com.basketballleague.domain.SeasonNameTakenException;
import com.basketballleague.domain.SeasonNotFoundException;
import com.basketballleague.domain.TeamNotFoundException;
import com.basketballleague.dto.AddSeasonTeamRequest;
import com.basketballleague.dto.CreateSeasonRequest;
import com.basketballleague.dto.PageResponse;
import com.basketballleague.dto.SeasonResponse;
import com.basketballleague.dto.TeamResponse;
import com.basketballleague.entity.Season;
import com.basketballleague.entity.Team;
import com.basketballleague.repository.SeasonRepository;
import com.basketballleague.repository.TeamRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

/**
 * Use cases for seasons and the teams taking part in them. The season entity holds the rules (draft only,
 * no duplicates, at most 20 teams); this class checks the rules that need the database.
 */
@ApplicationScoped
public class SeasonService {

    private final SeasonRepository seasonRepository;
    private final TeamRepository teamRepository;

    /** Required by CDI to create the proxy; never used to serve calls. */
    protected SeasonService() {
        this.seasonRepository = null;
        this.teamRepository = null;
    }

    @Inject
    public SeasonService(SeasonRepository seasonRepository, TeamRepository teamRepository) {
        this.seasonRepository = seasonRepository;
        this.teamRepository = teamRepository;
    }

    public PageResponse<SeasonResponse> listSeasons(int page, int size) {
        final List<SeasonResponse> items = seasonRepository.findPage(Math.multiplyExact(page, size), size).stream()
                .map(SeasonMapper::toResponse)
                .toList();
        return new PageResponse<>(items, page, size, seasonRepository.count());
    }

    public SeasonResponse getSeason(long seasonId) {
        return SeasonMapper.toResponse(findSeason(seasonId));
    }

    @Transactional
    public SeasonResponse createSeason(CreateSeasonRequest request) {
        // The entity strips the name too; stripping here makes the duplicate check see the stored value.
        final String name = request.name().strip();
        if (seasonRepository.findByName(name).isPresent()) {
            throw new SeasonNameTakenException(name);
        }
        return SeasonMapper.toResponse(seasonRepository.save(new Season(name, request.startDate())));
    }

    /**
     * Teams of the season ordered by name. Two queries whatever the number of teams: the ids, then the teams.
     */
    public List<TeamResponse> listTeams(long seasonId) {
        findSeason(seasonId);
        return teamRepository.findByIds(seasonRepository.findTeamIds(seasonId)).stream()
                .map(TeamMapper::toResponse)
                .toList();
    }

    /**
     * Locks the season row first, so a concurrent request for the same season waits and then sees this team.
     */
    @Transactional
    public TeamResponse addTeam(long seasonId, AddSeasonTeamRequest request) {
        final Season season = seasonRepository.findByIdForUpdate(seasonId)
                .orElseThrow(() -> new SeasonNotFoundException(seasonId));
        final long teamId = request.teamId();
        final Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamNotFoundException(teamId));
        season.addTeam(teamId);
        return TeamMapper.toResponse(team);
    }

    private Season findSeason(long seasonId) {
        return seasonRepository.findById(seasonId).orElseThrow(() -> new SeasonNotFoundException(seasonId));
    }
}
