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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AddressMatch that = (AddressMatch) o;
        return Double.compare(lat, that.lat) == 0 && Double.compare(lng, that.lng) == 0 && Objects.equals(formattedAddress, that.formattedAddress) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, formattedAddress, lat, lng);
    }

    @Override
    public String toString() {
        return "AddressMatch{" + name + ", " + formattedAddress + ", " + lng + " ," + lat + "}";
    }

}
