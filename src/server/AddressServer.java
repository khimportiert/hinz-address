package server;

import config.AppConfig;
import model.Address;
import model.AddressMatch;
import model.ValidationResult;
import service.PhotonClient;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static service.AddressDecoder.decodeTcpStringToAddress;
import static validation.AddressValidator.validatePhotonAddressAndQueryGoogle;

public class AddressServer {

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(AppConfig.getServerPort())) {
            System.out.println("Server gestartet auf Port " + AppConfig.getServerPort());

            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    handleClient(clientSocket);
                } catch (IOException e) {
                    System.err.println("Fehler bei Client-Verbindung: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Server konnte nicht gestartet werden: " + e.getMessage());
        }
    }

    private void handleClient(Socket socket) {
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

            // HTTP POST PHOTON
            Address inputAddress = decodeTcpStringToAddress(tcpIn);
            String photonResponse = PhotonClient.queryPhoton(inputAddress);

            // Bei Unstimmigkeiten wird Google angefragt
            ValidationResult result = validatePhotonAddressAndQueryGoogle(photonResponse, inputAddress);

            if (result.isExactMatch()) {
                // Exact match gefunden
                AddressMatch exactMatch = result.possibleMatches().stream()
                        .filter(AddressMatch::isExactMatch)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Kein exakter Match gefunden"));

                out.write("EXAKTER MATCH GEFUNDEN:");
                out.newLine();
                out.write(result.source());
                out.newLine();
                out.write(String.format("Adresse: %s", exactMatch.formattedAddress()));
                out.newLine();
                out.write(String.format("Koordinaten: %.6f, %.6f", exactMatch.lat(), exactMatch.lng()));
                out.newLine();
            } else {
                // Kein exakter Match, zeige mögliche Adressen
                out.write("KEIN EXAKTER MATCH. MÖGLICHE ADRESSEN:");
                out.newLine();
                out.write(result.source());
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

    private static String createPhotonUrl(String encodedAddress) {
        Address address = decodeTcpStringToAddress(encodedAddress);

        String query = address.toString();

        return AppConfig.getPhotonBaseUrl() + "?" +
                "q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) +
                "&limit=" + AppConfig.getPhotonResultLimit() + // Limitiere Anzahl Ergebnis
                "&lang=de"; // Deutsche Ergebnisse bevorzugen
    }
}
