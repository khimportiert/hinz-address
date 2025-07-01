package model;

import java.util.List;

public record ValidationResult(
        String source,
        boolean isExactMatch,
        List<AddressMatch> possibleMatches
) {}
