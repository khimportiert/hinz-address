package service;

import config.AppConfig;
import model.Address;
import model.DistanceTime;
import model.RouteResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OrsClient {
    private static final HttpClient client = HttpClient.newHttpClient();

    public static RouteResult getRoute(DistanceTime distanceTime) throws IOException, InterruptedException {

        boolean avoidHighways = distanceTime.rpa() < 0.5;
        boolean avoidFerries = distanceTime.rpf() < 0.5;
        int maxSpeed = distanceTime.rsa(); // Speed Autobahn

        JSONArray coordinates = new JSONArray()
                .put(new JSONArray().put(distanceTime.getFromLon()).put(distanceTime.getFromLat()))
                .put(new JSONArray().put(distanceTime.getToLon()).put(distanceTime.getToLat()));

        JSONObject options = new JSONObject();


        JSONArray avoidFeatures = new JSONArray();
        if (avoidFerries) { // Fähren vermeiden
            avoidFeatures.put("ferries");
        }
        if (avoidHighways) { // Autobahnen vermeiden
            avoidFeatures.put("highways");
        }
        if (!avoidFeatures.isEmpty()) {
            options.put("avoid_features", avoidFeatures);
        }

        JSONObject requestBody = new JSONObject()
                .put("coordinates", coordinates)
                .put("options", options)
                .put("instructions", false)
                .put("geometry", false)
                .put("maximum_speed", maxSpeed);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.getOrsBaseUrl() + "/v2/directions/driving-car"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            return new RouteResult(distanceTime.id(), 0, 0);
        }

        JSONObject json = new JSONObject(response.body());

        if (!json.has("routes") || json.getJSONArray("routes").isEmpty()) {
            return new RouteResult(distanceTime.id(), 0, 0);
        }

        JSONObject summary = json.getJSONArray("routes").getJSONObject(0).getJSONObject("summary");

        double distanceKm = summary.optDouble("distance", 0) / 1000.0;
        double durationSec = summary.optDouble("duration", 0);

        return new RouteResult(distanceTime.id(), distanceKm, (int) durationSec);
    }
}
