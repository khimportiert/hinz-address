package model;

public record AddressMatch(
        String formattedAddress,
        double lat,
        double lng,
        boolean isExactMatch
) {}
