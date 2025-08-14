package model;

import java.io.Serializable;
import java.util.Objects;

public record AddressMatch(
        String name,
        String formattedAddress,
        double lat,
        double lng,
        boolean isExactMatch
) implements Serializable {

    private static final double TOLERANCE = 0.001; // ca. 100 Meter, je nach Breitengrad

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AddressMatch that = (AddressMatch) o;

        boolean latClose = Math.abs(this.lat - that.lat) < TOLERANCE;
        boolean lngClose = Math.abs(this.lng - that.lng) < TOLERANCE;

        return latClose &&
                lngClose &&
                Objects.equals(this.formattedAddress, that.formattedAddress) &&
                Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        long latRounded = Math.round(lat / TOLERANCE);
        long lngRounded = Math.round(lng / TOLERANCE);
        return Objects.hash(name, formattedAddress, latRounded, lngRounded);
    }

    @Override
    public String toString() {
        return "AddressMatch{" + name + ", " + formattedAddress + ", " + lng + " ," + lat + "}";
    }

}
