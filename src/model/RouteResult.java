package model;

public record RouteResult(
    String id,
    double distanceKm,
    int durationSec
) {
    @Override
    public String toString() {
        return "RouteResult{" +
                "id='" + id + '\'' +
                ", distanceKm=" + distanceKm +
                ", durationSec=" + durationSec +
                '}';
    }
}
