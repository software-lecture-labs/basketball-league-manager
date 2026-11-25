package com.basketballleague.entity;

import com.basketballleague.domain.InvalidQuarterScoreException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Value object representing the points scored by both teams in a single quarter of a {@link Game} (ADR-0004).
 * Quarter 1–4 are regulation; 5 and above represent overtime periods. Stored in {@code game_quarter_scores}.
 */
@Embeddable
public class QuarterScore {

    static final int FIRST_QUARTER = 1;
    static final int OVERTIME_START_QUARTER = 5;

    @Column(name = "quarter_no", nullable = false)
    private int quarterNo;

    @Column(name = "home_points", nullable = false)
    private int homePoints;

    @Column(name = "away_points", nullable = false)
    private int awayPoints;

    /** Required by JPA. */
    protected QuarterScore() {
    }

    public QuarterScore(int quarterNo, int homePoints, int awayPoints) {
        this.quarterNo = requireValidQuarterNumber(quarterNo);
        this.homePoints = requireNonNegativePoints(homePoints, "Home points");
        this.awayPoints = requireNonNegativePoints(awayPoints, "Away points");
    }

    public boolean isOvertime() {
        return quarterNo >= OVERTIME_START_QUARTER;
    }

    private static int requireValidQuarterNumber(int quarterNo) {
        if (quarterNo < FIRST_QUARTER) {
            throw new InvalidQuarterScoreException("Quarter number must be at least 1, was " + quarterNo);
        }
        return quarterNo;
    }

    private static int requireNonNegativePoints(int points, String label) {
        if (points < 0) {
            throw new InvalidQuarterScoreException(label + " must not be negative, was " + points);
        }
        return points;
    }

    public int getQuarterNo() {
        return quarterNo;
    }

    public int getHomePoints() {
        return homePoints;
    }

    public int getAwayPoints() {
        return awayPoints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QuarterScore other = (QuarterScore) o;
        return quarterNo == other.quarterNo && homePoints == other.homePoints && awayPoints == other.awayPoints;
    }

    @Override
    public int hashCode() {
        return Objects.hash(quarterNo, homePoints, awayPoints);
    }

    @Override
    public String toString() {
        return "QuarterScore{quarterNo=" + quarterNo + ", homePoints=" + homePoints
                + ", awayPoints=" + awayPoints + '}';
    }
}
