package model;

import java.util.List;

public record ValidationResult(
        Address requestedAddress,
        String source,
        boolean isExactMatch,
        List<AddressMatch> possibleMatches
) {}
