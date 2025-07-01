package service;

import config.AppConfig;
import model.Address;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class GoogleClient {
    private static final HttpClient client = HttpClient.newHttpClient();
    
    public static String queryGeocoding(Address address) throws IOException, InterruptedException {
        String query = address.toString();
        
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = AppConfig.getGoogleGeocodingUrl() +
                    "?address=" + encodedQuery +
                    "&key=" + AppConfig.getGoogleApiKey() +
                    "&language=de" +
                    "&region=de";
                    
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();
            
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }
}