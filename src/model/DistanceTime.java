package model;

public record DistanceTime(
        String id,
        String pos,
        String zos,
        double rpa,
        double rpv,
        double rpn,
        double rpf,
        int rsa,
        int rss,
        int rsv,
        int rsn,
        int rso,
        String tmp
) {
    public double getFromLon() {
        return Double.parseDouble(pos.split(";")[0].replace(",", "."));
    }

    public double getFromLat() {
        return Double.parseDouble(pos.split(";")[1].replace(",", "."));
    }

    public double getToLon() {
        return Double.parseDouble(zos.split(";")[0].replace(",", "."));
    }

    public double getToLat() {
        return Double.parseDouble(zos.split(";")[1].replace(",", "."));
    }
}
