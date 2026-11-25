package com.basketballleague.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.basketballleague.domain.InvalidPlayerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PlayerTest {

    private static final Class<InvalidPlayerException> INVALID = InvalidPlayerException.class;

    private final Team team = new Team("Fenerbahçe Beko", "İstanbul");

    @Test
    @DisplayName("Creates a player assigned to a team")
    void createsPlayerWithTeam() {
        Player player = new Player(team, "Nigel", "Hayes-Davis", 11, Position.SF);

        assertThat(player.getFirstName()).isEqualTo("Nigel");
        assertThat(player.getLastName()).isEqualTo("Hayes-Davis");
        assertThat(player.getFullName()).isEqualTo("Nigel Hayes-Davis");
        assertThat(player.getJerseyNumber()).isEqualTo(11);
        assertThat(player.getPosition()).isEqualTo(Position.SF);
        assertThat(player.getTeam()).isEqualTo(team);
        assertThat(team.getPlayers()).containsExactly(player);
    }

    @Test
    @DisplayName("Keeps Turkish characters and strips surrounding whitespace in names")
    void keepsTurkishCharactersInNames() {
        Player player = new Player(team, "  Şükrü ", " Işık  ", 7, Position.PG);

        assertThat(player.getFullName()).isEqualTo("Şükrü Işık");
    }

    @Test
    @DisplayName("Rejects a player without team, position or name")
    void rejectsMissingRequiredFields() {
        assertThatThrownBy(() -> new Player(null, "Scottie", "Wilbekin", 0, Position.PG)).isInstanceOf(INVALID);
        assertThatThrownBy(() -> new Player(team, "Scottie", "Wilbekin", 0, null)).isInstanceOf(INVALID);
        assertThatThrownBy(() -> new Player(team, null, "Wilbekin", 0, Position.PG)).isInstanceOf(INVALID);
        assertThatThrownBy(() -> new Player(team, "Scottie", "  ", 0, Position.PG)).isInstanceOf(INVALID);
    }

    @ParameterizedTest
    @ValueSource(ints = {Player.MIN_JERSEY_NUMBER, Player.MAX_JERSEY_NUMBER})
    @DisplayName("Accepts jersey numbers on the 0-99 boundaries")
    void acceptsBoundaryJerseyNumbers(int number) {
        Player player = new Player(team, "Scottie", "Wilbekin", number, Position.PG);

        assertThat(player.getJerseyNumber()).isEqualTo(number);
    }

    @ParameterizedTest
    @ValueSource(ints = {Player.MIN_JERSEY_NUMBER - 1, Player.MAX_JERSEY_NUMBER + 1, Integer.MAX_VALUE})
    @DisplayName("Rejects jersey numbers outside 0-99")
    void rejectsJerseyNumbersOutsideRange(int number) {
        assertThatThrownBy(() -> new Player(team, "Scottie", "Wilbekin", number, Position.PG))
                .isInstanceOf(INVALID)
                .hasMessageContaining("between 0 and 99");
    }

    @Test
    @DisplayName("Transfers player to another team and maintains bidirectional relationship")
    void transfersPlayerToNewTeam() {
        Team oldTeam = new Team("FC Barcelona", "Barcelona");
        Player player = new Player(oldTeam, "Nick", "Calathes", 15, Position.PG);

        player.transferTo(team, 33);

        assertThat(player.getTeam()).isEqualTo(team);
        assertThat(player.getJerseyNumber()).isEqualTo(33);
        assertThat(oldTeam.getPlayers()).doesNotContain(player);
        assertThat(team.getPlayers()).contains(player);
    }

    @Test
    @DisplayName("Rejected transfer leaves the player on the old team")
    void rejectedTransferKeepsOldTeam() {
        Team oldTeam = new Team("FC Barcelona", "Barcelona");
        Player player = new Player(oldTeam, "Nick", "Calathes", 15, Position.PG);

        assertThatThrownBy(() -> player.transferTo(team, Player.MAX_JERSEY_NUMBER + 1)).isInstanceOf(INVALID);
        assertThatThrownBy(() -> player.transferTo(null, 33)).isInstanceOf(INVALID);

        assertThat(player.getTeam()).isEqualTo(oldTeam);
        assertThat(player.getJerseyNumber()).isEqualTo(15);
        assertThat(oldTeam.getPlayers()).containsExactly(player);
    }

    @Test
    @DisplayName("Assigns a new jersey number with validation")
    void assignsJerseyNumber() {
        Player player = new Player(team, "Marko", "Guduric", 23, Position.SG);

        player.assignJerseyNumber(5);

        assertThat(player.getJerseyNumber()).isEqualTo(5);
        assertThatThrownBy(() -> player.assignJerseyNumber(105)).isInstanceOf(INVALID);
        assertThat(player.getJerseyNumber()).isEqualTo(5);
    }

    @Test
    @DisplayName("Changes playing position and rejects null")
    void changesPosition() {
        Player player = new Player(team, "John", "Doe", 1, Position.PG);

        player.changePosition(Position.SG);

        assertThat(player.getPosition()).isEqualTo(Position.SG);
        assertThatThrownBy(() -> player.changePosition(null)).isInstanceOf(INVALID);
    }

    @Test
    @DisplayName("Renames player and rejects blank names")
    void renamesPlayer() {
        Player player = new Player(team, "John", "Doe", 1, Position.PG);

        player.rename("Kostas", "Sloukas");

        assertThat(player.getFullName()).isEqualTo("Kostas Sloukas");
        assertThatThrownBy(() -> player.rename("", "Sloukas")).isInstanceOf(INVALID);
    }
}
