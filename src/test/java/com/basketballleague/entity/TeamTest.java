package com.basketballleague.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TeamTest {

    @Test
    @DisplayName("Creates a team with valid name and city")
    void createsTeamSuccessfully() {
        Team team = new Team("Fenerbahçe Beko", "İstanbul");

        assertThat(team.getName()).isEqualTo("Fenerbahçe Beko");
        assertThat(team.getCity()).isEqualTo("İstanbul");
        assertThat(team.getPlayers()).isEmpty();
    }

    @Test
    @DisplayName("Rejects null or blank name and city")
    void rejectsInvalidCreationParameters() {
        assertThatThrownBy(() -> new Team(null, "İstanbul"))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new Team("   ", "İstanbul"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new Team("Anadolu Efes", null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new Team("Anadolu Efes", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Renames team and strips whitespace")
    void renamesTeam() {
        Team team = new Team("Efes Pilsen", "İstanbul");
        team.rename("  Anadolu Efes  ");

        assertThat(team.getName()).isEqualTo("Anadolu Efes");
    }

    @Test
    @DisplayName("Relocates team to another city")
    void relocatesTeam() {
        Team team = new Team("AS Monaco", "Monaco");
        team.relocateTo("Fontvieille");

        assertThat(team.getCity()).isEqualTo("Fontvieille");
    }

    @Test
    @DisplayName("getPlayers returns an unmodifiable collection")
    void playersListIsUnmodifiable() {
        Team team = new Team("Real Madrid", "Madrid");
        Player player = new Player(team, "Facundo", "Campazzo", 7, Position.PG, LocalDate.of(1991, 3, 23));

        assertThat(team.getPlayers()).containsExactly(player);
        assertThatThrownBy(() -> team.getPlayers().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
