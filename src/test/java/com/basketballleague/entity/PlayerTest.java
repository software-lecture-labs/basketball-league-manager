package com.basketballleague.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlayerTest {

    @Test
    @DisplayName("Creates a free agent player")
    void createsFreeAgent() {
        Player player = new Player("Nigel", "Hayes-Davis", Position.SF, LocalDate.of(1994, 12, 16));

        assertThat(player.getFirstName()).isEqualTo("Nigel");
        assertThat(player.getLastName()).isEqualTo("Hayes-Davis");
        assertThat(player.getFullName()).isEqualTo("Nigel Hayes-Davis");
        assertThat(player.getPosition()).isEqualTo(Position.SF);
        assertThat(player.getBirthDate()).isEqualTo(LocalDate.of(1994, 12, 16));
        assertThat(player.getTeam()).isNull();
        assertThat(player.getJerseyNumber()).isNull();
        assertThat(player.isFreeAgent()).isTrue();
    }

    @Test
    @DisplayName("Creates a player assigned to a team")
    void createsPlayerWithTeam() {
        Team team = new Team("Fenerbahçe Beko", "İstanbul");
        Player player = new Player(team, "Nigel", "Hayes-Davis", 11, Position.SF, LocalDate.of(1994, 12, 16));

        assertThat(player.getTeam()).isEqualTo(team);
        assertThat(player.getJerseyNumber()).isEqualTo(11);
        assertThat(player.isFreeAgent()).isFalse();
        assertThat(team.getPlayers()).containsExactly(player);
    }

    @Test
    @DisplayName("Rejects jersey number outside 0-99 range")
    void validatesJerseyNumberRange() {
        Team team = new Team("Fenerbahçe Beko", "İstanbul");

        assertThatThrownBy(() -> new Player(team, "Scottie", "Wilbekin", 100, Position.PG, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 0 and 99");

        assertThatThrownBy(() -> new Player(team, "Scottie", "Wilbekin", -1, Position.PG, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 0 and 99");
    }

    @Test
    @DisplayName("Transfers player to another team and maintains bidirectional relationship")
    void transfersPlayerToNewTeam() {
        Team oldTeam = new Team("FC Barcelona", "Barcelona");
        Team newTeam = new Team("Fenerbahçe Beko", "İstanbul");
        Player player = new Player(oldTeam, "Nick", "Calathes", 15, Position.PG, LocalDate.of(1989, 2, 7));

        assertThat(oldTeam.getPlayers()).contains(player);

        player.transferTo(newTeam, 33);

        assertThat(player.getTeam()).isEqualTo(newTeam);
        assertThat(player.getJerseyNumber()).isEqualTo(33);
        assertThat(oldTeam.getPlayers()).doesNotContain(player);
        assertThat(newTeam.getPlayers()).contains(player);
    }

    @Test
    @DisplayName("Releases player to become a free agent")
    void releasesPlayer() {
        Team team = new Team("Olympiacos", "Pireas");
        Player player = new Player(team, "Sasha", "Vezenkov", 14, Position.PF, LocalDate.of(1995, 8, 6));

        player.releaseFromTeam();

        assertThat(player.isFreeAgent()).isTrue();
        assertThat(player.getTeam()).isNull();
        assertThat(player.getJerseyNumber()).isNull();
        assertThat(team.getPlayers()).doesNotContain(player);
    }

    @Test
    @DisplayName("Assigns new jersey number with validation")
    void assignsJerseyNumber() {
        Player player = new Player("Marko", "Guduric", Position.SG, null);
        player.assignJerseyNumber(23);

        assertThat(player.getJerseyNumber()).isEqualTo(23);

        assertThatThrownBy(() -> player.assignJerseyNumber(105))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Changes playing position")
    void changesPosition() {
        Player player = new Player("John", "Doe", Position.PG, null);
        player.changePosition(Position.SG);

        assertThat(player.getPosition()).isEqualTo(Position.SG);
    }
}
