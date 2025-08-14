package validation;

import config.AppConfig;
import model.Address;
import model.AddressMatch;
import model.ValidationResult;
import org.json.JSONArray;
import org.json.JSONObject;
import service.GoogleCache;
import service.GoogleClient;
import service.PhotonClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddressValidator {

    public static int Google_Count = 0;

    public static boolean Use_Cache = false;
    public static GoogleCache Google_Cache;

    static {
        if (AppConfig.getGoogleUseCache()) {
            Use_Cache = true;
            Google_Cache = new GoogleCache(AppConfig.getGoogleCacheUrl());
        }
    }

    public static ValidationResult validateWithBackup(Address inputAddress) {
        ValidationResult photonResult = null;
        ValidationResult googleResult = null;

        boolean isPhotonAllowed = AppConfig.getPhotonApiAllowed();
        boolean isGoogleAllowed = AppConfig.getGoogleApiAllowed();

        if (!isPhotonAllowed && !isGoogleAllowed) {
            throw new IllegalArgumentException("Keine API wurde angefragt. Überprüfe config.properties.");
        }

        // Versuche Photon-Abfrage
        if (isPhotonAllowed) {
            try {
                String photonResponse = PhotonClient.queryPhoton(inputAddress);
                photonResult = validatePhotonResponse(photonResponse, inputAddress);
            } catch (Exception e) {
                System.err.println("Photon-Abfrage fehlgeschlagen: " + e);
                e.printStackTrace();
            }
        }


        // Wenn Photon ein exaktes Ergebnis liefert oder Google nicht erlaubt ist, gebe dieses zurück
        if ((photonResult != null && photonResult.isExactMatch()) || !isGoogleAllowed) {
            return (photonResult != null) ? photonResult : new ValidationResult(inputAddress, "Photon", false, new ArrayList<>());
        }

        // Versuche Google-Abfrage als Backup
        try {

            AddressMatch cacheMatch;
            if (Use_Cache && (cacheMatch = Google_Cache.find(inputAddress.toString())) != null) {
                List<AddressMatch> matches = new ArrayList<>();
                matches.add(cacheMatch);
                googleResult = new ValidationResult(inputAddress, "Google Cache", true, matches);
            }
            else {
                String googleResponse = GoogleClient.queryGeocoding(inputAddress);
                googleResult = validateGoogleResponse(googleResponse, inputAddress, Google_Cache);
                Google_Count++;
            }

        } catch (Exception e) {
            System.err.println("Google-Abfrage fehlgeschlagen: " + e);
        }

        // Priorisiere Google-Ergebnisse, wenn die Photon-Ergebnisse nicht genau waren
        if (googleResult != null && googleResult.isExactMatch()) {
            return googleResult;
        }

        // Fallback: Wenn beide keine exakten Ergebnisse liefern, kombiniere die möglichen Matches
        List<AddressMatch> combinedMatches = new ArrayList<>();
        if (googleResult != null) {
            combinedMatches.addAll(googleResult.possibleMatches());
        }
        if (photonResult != null) {
            combinedMatches.addAll(photonResult.possibleMatches());
        }

        String source = (photonResult != null) ? "Photon + Google" : "Google";

        return new ValidationResult(inputAddress, source, false, combinedMatches);
    }

    private static ValidationResult validatePhotonResponse(String jsonResponse, Address inputAddress) throws Exception {
        JSONObject root = new JSONObject(jsonResponse);
        JSONArray features = root.getJSONArray("features");

        if (features.isEmpty()) {
            return new ValidationResult(inputAddress, "Photon", false, new ArrayList<>());
        }

        List<AddressMatch> matches = new ArrayList<>();

        for (int i = 0; i < features.length(); i++) {
            JSONObject feature = features.getJSONObject(i);
            JSONObject properties = feature.getJSONObject("properties");
            JSONArray coordinates = feature.getJSONObject("geometry").getJSONArray("coordinates");

            String name = properties.optString("name", "").trim();

            String photonPostcode = properties.optString("postcode", "");
            String photonCity = properties.optString("city", "");
            String photonStreet = properties.optString("street", "");
            String photonHouseNumber = properties.optString("housenumber", "");

            boolean isExactMatch =
                    normalizeNames(photonPostcode).equals(normalizeNames(inputAddress.plz())) &&
                    normalizeNames(photonCity).equals(normalizeNames(inputAddress.city())) &&
                    compareStreetNames(photonStreet, inputAddress.street()) &&
                    compareHouseNumbers(photonHouseNumber, inputAddress.houseNumber());

            matches.add(new AddressMatch(
                    name,
                    String.format("%s %s, %s %s", photonStreet, photonHouseNumber, photonPostcode, photonCity),
                    coordinates.getDouble(1), // Photon returns [lon, lat]
                    coordinates.getDouble(0),
                    isExactMatch
            ));
        }

        // Sortierung bleibt stabil
        matches.sort((a, b) -> Boolean.compare(!a.isExactMatch(), !b.isExactMatch()));

        // Wenn es mindestens einen exakten Match gibt
        boolean firstMatchesExactly = matches.getFirst().isExactMatch();

        return new ValidationResult(inputAddress, "Photon", firstMatchesExactly, matches);
    }

    private static ValidationResult validateGoogleResponse(String jsonResponse, Address inputAddress, GoogleCache cache) throws Exception {
        JSONObject root = new JSONObject(jsonResponse);

        if (!"OK".equals(root.getString("status"))) {
            return new ValidationResult(inputAddress, "Google", false, new ArrayList<>());
        }

        List<AddressMatch> matches = new ArrayList<>();
        JSONArray results = root.getJSONArray("results");

        for (int i = 0; i < results.length(); i++) {
            JSONObject result = results.getJSONObject(i);

            String locationType = result.getJSONObject("geometry").getString("location_type");
            boolean isPartialMatch = result.optBoolean("partial_match", false);

            JSONObject location = result.getJSONObject("geometry").getJSONObject("location");
            double lat = location.getDouble("lat");
            double lng = location.getDouble("lng");

            Map<String, String> addressParts = extractAddressParts(result);
            boolean isExactMatch =
                    !isPartialMatch
                    && !"APPROXIMATE".equals(locationType)
                    && addressParts.getOrDefault("postal_code", "").equals(inputAddress.plz())
                    && addressParts.getOrDefault("locality", "").equals(inputAddress.city())
                    && normalizeNumbers(addressParts.getOrDefault("street_number", "")).equals(normalizeNumbers(inputAddress.houseNumber()))
//                    && compareAddressParts(addressParts, inputAddress)
                    ;

            String formattedAddress = result.optString("formatted_address", null);
            if (formattedAddress != null) {
                formattedAddress = formattedAddress
                        .replace(", Deutschland", "")
                        .replace(", Germany", "");

                matches.add(new AddressMatch(
                        "",
                        formattedAddress,
                        lat,
                        lng,
                        isExactMatch
                ));
            }
        }

        // Sortierung bleibt stabil
        matches.sort((a, b) -> Boolean.compare(!a.isExactMatch(), !b.isExactMatch()));

        // Wenn es mindestens einen exakten Match gibt
        boolean firstMatchesExactly = matches.getFirst().isExactMatch();

        if (Use_Cache && firstMatchesExactly) {
            cache.save(inputAddress.toString(), matches.getFirst());
        }

        return new ValidationResult(inputAddress, "Google", firstMatchesExactly, matches);
    }

    private static String normalizeNumbers(String input) {
        return input
                .replaceAll("\\s", "")
                .replaceAll("[\\-/]", " ")
                .toLowerCase();
    }

    private static String normalizeNames(String input) {
        return input.trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");
    }

    public static boolean compareHouseNumbers(String a, String b) {
        String normalizedA = normalizeNumbers(a);
        String normalizedB = normalizeNumbers(b);

        return normalizedA.equals(normalizedB);
    }

    private static boolean compareStreetNames(String a, String b) {
        String s1 = normalizeNames(a);
        String s2 = normalizeNames(b);

        // Gängige Straßen-Abkürzungen am Wortende ersetzen
        Map<String, String> replacements = Map.of(
                "str\\.$", "straße",
                "str$", "straße",
                "pl\\.$", "platz",
                "al\\.$", "allee"
        );

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            s1 = s1.replaceAll(entry.getKey(), entry.getValue());
            s2 = s2.replaceAll(entry.getKey(), entry.getValue());
        }

        return s1.equals(s2);
    }

    private static Map<String, String> extractAddressParts(JSONObject result) throws NullPointerException {
        JSONArray components = result.getJSONArray("address_components");
        Map<String, String> addressParts = new HashMap<>();

        for (int i = 0; i < components.length(); i++) {
            JSONObject component = components.getJSONObject(i);
            JSONArray types = component.getJSONArray("types");
            String longName = component.getString("long_name");

            if (types.toString().contains("street_number")) {
                addressParts.put("street_number", longName);
            } else if (types.toString().contains("route")) {
                addressParts.put("street_name", longName);
            } else if (types.toString().contains("postal_code")) {
                addressParts.put("postal_code", longName);
            } else if (types.toString().contains("locality")) {
                addressParts.put("locality", longName);
            }
        }

        return addressParts;
    }

    private static boolean compareAddressParts(Map<String, String> addressParts, Address inputAddress) {
        return compareStreetNames(addressParts.get("street_name"), inputAddress.street()) &&
                compareHouseNumbers(addressParts.get("street_number"), inputAddress.houseNumber()) &&
                normalizeNames(addressParts.get("postal_code")).equals(normalizeNames(inputAddress.plz())) &&
                normalizeNames(addressParts.get("locality")).equals(normalizeNames(inputAddress.city()));
    }

}
