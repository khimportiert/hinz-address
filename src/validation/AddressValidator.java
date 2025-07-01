package validation;

import config.AppConfig;
import model.Address;
import model.AddressMatch;
import model.ValidationResult;
import org.json.JSONArray;
import org.json.JSONObject;
import service.GoogleClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddressValidator {
    public static ValidationResult validatePhotonAddressAndQueryGoogle(String photonResponse, Address inputAddress) {
        ValidationResult photonResult = validatePhotonResponse(photonResponse, inputAddress);

        if (photonResult.isExactMatch() || !AppConfig.getGoogleApiAllowed()) {
            return photonResult;
        }

        try {
            String googleResponse = GoogleClient.queryGeocoding(inputAddress);
            return validateGoogleResponse(googleResponse, inputAddress);
        } catch (Exception e) {
            return new ValidationResult("Exception", false, new ArrayList<>());
        }
    }

    private static ValidationResult validatePhotonResponse(String jsonResponse, Address inputAddress) {
        try {
            JSONObject root = new JSONObject(jsonResponse);
            JSONArray features = root.getJSONArray("features");

            if (features.isEmpty()) {
                return new ValidationResult("Photon", false, new ArrayList<>());
            }

            List<AddressMatch> matches = new ArrayList<>();

            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject properties = feature.getJSONObject("properties");
                JSONArray coordinates = feature.getJSONObject("geometry").getJSONArray("coordinates");

                String photonPostcode = properties.optString("postcode", "");
                String photonCity = properties.optString("city", "");
                String photonStreet = properties.optString("street", "");
                String photonHouseNumber = properties.optString("housenumber", "");

                boolean isExactMatch = normalizeString(photonPostcode).equals(normalizeString(inputAddress.plz())) &&
                        normalizeString(photonCity).equals(normalizeString(inputAddress.city())) &&
                        compareStreetNames(photonStreet, inputAddress.street()) &&
                        normalizeString(photonHouseNumber).equals(normalizeString(inputAddress.houseNumber()));

                matches.add(new AddressMatch(
                        String.format("%s %s, %s %s", photonStreet, photonHouseNumber, photonPostcode, photonCity),
                        coordinates.getDouble(1), // Photon returns [lon, lat]
                        coordinates.getDouble(0),
                        isExactMatch
                ));
            }

            boolean firstMatchesExactly = matches.getFirst().isExactMatch();
            return new ValidationResult("Photon", firstMatchesExactly, matches);
        } catch (Exception e) {
            return new ValidationResult("Exception", false, new ArrayList<>());
        }
    }

    private static ValidationResult validateGoogleResponse(String jsonResponse, Address inputAddress) {
        JSONObject root = new JSONObject(jsonResponse);

        if (!"OK".equals(root.getString("status"))) {
            return new ValidationResult("Google", false, new ArrayList<>());
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
            boolean isExactMatch = !isPartialMatch &&
                    !"APPROXIMATE".equals(locationType) &&
                    compareAddressParts(addressParts, inputAddress);

            matches.add(new AddressMatch(
                    result.getString("formatted_address")
                            .replace(", Deutschland", "")
                            .replace(", Germany", ""),
                    lat,
                    lng,
                    isExactMatch
            ));
        }
        // Wenn es mindestens einen exakten Match gibt
        boolean firstMatchesExactly = matches.getFirst().isExactMatch();

        return new ValidationResult("Google", firstMatchesExactly, matches);
    }

    private static String normalizeString(String input) {
        if (input == null) return "";
        return input.trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");
    }

    private static boolean compareStreetNames(String street1, String street2) {
        String s1 = normalizeString(street1);
        String s2 = normalizeString(street2);

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

    private static Map<String, String> extractAddressParts(JSONObject result) {
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
                normalizeString(addressParts.get("street_number")).equals(normalizeString(inputAddress.houseNumber())) &&
                normalizeString(addressParts.get("postal_code")).equals(normalizeString(inputAddress.plz())) &&
                normalizeString(addressParts.get("locality")).equals(normalizeString(inputAddress.city()));
    }

}
