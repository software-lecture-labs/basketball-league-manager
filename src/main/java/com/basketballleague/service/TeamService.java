package com.basketballleague.service;

import com.basketballleague.domain.JerseyNumberTakenException;
import com.basketballleague.domain.TeamNameTakenException;
import com.basketballleague.domain.TeamNotFoundException;
import com.basketballleague.dto.CreatePlayerRequest;
import com.basketballleague.dto.CreateTeamRequest;
import com.basketballleague.dto.PageResponse;
import com.basketballleague.dto.PlayerResponse;
import com.basketballleague.dto.TeamResponse;
import com.basketballleague.entity.Player;
import com.basketballleague.entity.Position;
import com.basketballleague.entity.Team;
import com.basketballleague.repository.PlayerRepository;
import com.basketballleague.repository.TeamRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

/**
 * Use cases for teams and their players. Requests are already validated by the resource; this class checks the
 * rules that need the database (unknown team, duplicate name, duplicate jersey number).
 */
@ApplicationScoped
public class TeamService {

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    /** Required by CDI to create the proxy; never used to serve calls. */
    protected TeamService() {
        this.teamRepository = null;
        this.playerRepository = null;
    }

    @Inject
    public TeamService(TeamRepository teamRepository, PlayerRepository playerRepository) {
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
    }

    public PageResponse<TeamResponse> listTeams(int page, int size) {
        final List<TeamResponse> items = teamRepository.findPage(firstResult(page, size), size).stream()
                .map(TeamMapper::toResponse)
                .toList();
        return new PageResponse<>(items, page, size, teamRepository.count());
    }

    public TeamResponse getTeam(long teamId) {
        return TeamMapper.toResponse(findTeam(teamId));
    }

    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request) {
        // The entity strips the name too; stripping here makes the duplicate check see the stored value.
        final String name = request.name().strip();
        if (teamRepository.findByName(name).isPresent()) {
            throw new TeamNameTakenException(name);
        }
        final Team team = teamRepository.save(new Team(name, request.city()));
        return TeamMapper.toResponse(team);
    }

    public PageResponse<PlayerResponse> listPlayers(long teamId, int page, int size) {
        findTeam(teamId);
        final List<PlayerResponse> items = playerRepository.findPageByTeamId(teamId, firstResult(page, size), size)
                .stream()
                .map(TeamMapper::toResponse)
                .toList();
        return new PageResponse<>(items, page, size, playerRepository.countByTeamId(teamId));
    }

    @Transactional
    public PlayerResponse addPlayer(long teamId, CreatePlayerRequest request) {
        final Team team = findTeam(teamId);
        final int jerseyNumber = request.jerseyNumber();
        if (playerRepository.existsByTeamIdAndJerseyNumber(teamId, jerseyNumber)) {
            throw new JerseyNumberTakenException(teamId, jerseyNumber);
        }
        final Player player = new Player(team, request.firstName(), request.lastName(), jerseyNumber,
                Position.valueOf(request.position()));
        return TeamMapper.toResponse(playerRepository.save(player));
    }

    private Team findTeam(long teamId) {
        return teamRepository.findById(teamId).orElseThrow(() -> new TeamNotFoundException(teamId));
    }

    /**
     * {@code multiplyExact} fails loudly instead of wrapping around; the resource keeps page and size small
     * enough that this never happens for a valid request.
     */
    private static int firstResult(int page, int size) {
        return Math.multiplyExact(page, size);
    }
}
