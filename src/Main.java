import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static final String PHOTON_BASE_URL = ConfigLoader.getProperty("photon.base.url");
    public static final int PORT = Integer.parseInt(ConfigLoader.getProperty("server.port"));
    private static final int RESULT_LIMIT = Integer.parseInt(ConfigLoader.getProperty("photon.result.limit"));;
    private static final String GOOGLE_API_KEY = ConfigLoader.getProperty("google.api.key");
    private static final String GOOGLE_GEOCODING_URL = ConfigLoader.getProperty("google.geocoding.url");


    public static void main(String[] args) {
        System.out.println("Starte TCP-Server auf Port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Starten des Servers: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        try (
                socket;
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
        ) {
            String tcpIn = in.readLine();
            if (tcpIn == null || tcpIn.isBlank()) {
                return;
            }

            String photonUrl = createPhotonUrl(tcpIn);

            // HTTP POST mit JSON als Body senden
            HttpResponse<String> photonResponse;
            try (HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build()) {

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(photonUrl)) // Ziel-URL
                        .timeout(Duration.ofSeconds(15))
                        .build();

                photonResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            }

            Address inputAddress = decodeTcpStringToAddress(tcpIn);

            // Bei Unstimmigkeiten wird Google angefragt
            ValidationResult result = validatePhotonAddressAndQueryGoogle(photonResponse.body(), inputAddress);

            if (result.isExactMatch()) {
                // Exact match gefunden
                AddressMatch exactMatch = result.possibleMatches().stream()
                        .filter(AddressMatch::isExactMatch)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Kein exakter Match gefunden"));

                out.write("EXAKTER MATCH GEFUNDEN:");
                out.newLine();
                out.write(result.source);
                out.newLine();
                out.write(String.format("Adresse: %s", exactMatch.formattedAddress()));
                out.newLine();
                out.write(String.format("Koordinaten: %.6f, %.6f", exactMatch.lat(), exactMatch.lng()));
                out.newLine();
            } else {
                // Kein exakter Match, zeige mögliche Adressen
                out.write("KEIN EXAKTER MATCH. MÖGLICHE ADRESSEN:");
                out.newLine();
                for (AddressMatch match : result.possibleMatches()) {
                    out.write(String.format("- %s (%.6f, %.6f)",
                            match.formattedAddress(),
                            match.lat(),
                            match.lng()));
                    out.newLine();
                }
            }
            out.flush();


        } catch (IOException | InterruptedException e) {
            System.err.println("Fehler beim Bearbeiten eines Clients: " + e.getMessage());
        }
    }

    private static ValidationResult validatePhotonAddressAndQueryGoogle(String photonResponse, Address inputAddress) {
        // Erst Photon prüfen
        ValidationResult photonResult = validatePhotonResponse(photonResponse, inputAddress);
        if (photonResult.isExactMatch()) {
            return photonResult;
        }

        // Wenn Photon nicht exakt matched, Google als Backup verwenden
        try {
            String googleResponse = queryGoogleGeocoding(inputAddress);
            return validateGoogleResponse(googleResponse, inputAddress);
        } catch (Exception e) {
            return new ValidationResult("Exception", false, new ArrayList<>());
        }
    }


    private record Address(
            String plz,
            String city,
            String street,
            String houseNumber
    ) {
        public String toString() {
            return String.format("%s %s, %s %s", street, houseNumber, plz, city);
        }
    }

    public record AddressMatch(
            String formattedAddress,
            double lat,
            double lng,
            boolean isExactMatch
    ) {}

    public record ValidationResult(
            String source,
            boolean isExactMatch,
            List<AddressMatch> possibleMatches
    ) {}


    private static Address decodeTcpStringToAddress(String tcpIn) {
        String plz = null;
        String city = null;
        String street = null;
        String houseNumber = null;

        int i = 0;
        while (i < tcpIn.length()) {
            String type = tcpIn.substring(i, i + 2);
            i += 2;
            int length = Integer.parseInt(tcpIn.substring(i, i + 2), 16);
            i += 2;
            String value = tcpIn.substring(i, i + length);
            i += length;

            switch (type) {
                case "0A" -> plz = value;
                case "0B" -> city = value;
                case "0C" -> street = value;
                case "0D" -> houseNumber = value;
            }
        }

        return new Address(plz, city, street, houseNumber);
    }

    public static String createPhotonUrl(String encodedAddress) {
        Address address = decodeTcpStringToAddress(encodedAddress);

        String query = address.toString();

        return PHOTON_BASE_URL + "?" +
                "q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) +
                "&limit=" + RESULT_LIMIT + // Limitiere auf einen Ergebnis
                "&lang=de"; // Deutsche Ergebnisse bevorzugen
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

    private static String queryGoogleGeocoding(Address address) throws IOException, InterruptedException {
        String query = address.toString();

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = GOOGLE_GEOCODING_URL +
                "?address=" + encodedQuery +
                "&key=" + GOOGLE_API_KEY +
                "&language=de" +
                "&region=de";

        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        return response.body();
    }

    private static ValidationResult validatePhotonResponse(String jsonResponse, Address inputAddress) {
        try {
            JSONObject root = new JSONObject(jsonResponse);
            JSONArray features = root.getJSONArray("features");

            if (features.isEmpty()) {
                return new ValidationResult("Photon", false, new ArrayList<>());
            }

            JSONObject feature = features.getJSONObject(0);
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

            AddressMatch match = new AddressMatch(
                    String.format("%s %s, %s %s", photonStreet, photonHouseNumber, photonPostcode, photonCity),
                    coordinates.getDouble(1), // Photon returns [lon, lat]
                    coordinates.getDouble(0),
                    isExactMatch
            );

            return new ValidationResult("Photon", isExactMatch, List.of(match));
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
        boolean hasExactMatch = matches.stream().anyMatch(AddressMatch::isExactMatch);

        return new ValidationResult("Google", hasExactMatch, matches);
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