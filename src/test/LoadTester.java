package test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadTester {
    private static final int PORT = 2323;
    private static final int NUM_THREADS = 30;
    private static final int REQUESTS_PER_THREAD = 100;
    private static final Random RANDOM = new Random();
    private static final AtomicInteger successfulRequests = new AtomicInteger(0);
    private static final AtomicInteger failedRequests = new AtomicInteger(0);

    // Realistische Testdaten
    private static final List<String> CITIES = List.of(
            "Berlin", "Hamburg", "München", "Köln", "Frankfurt", "Stuttgart"
    );
    private static final List<String> STREETS = List.of(
            "Hauptstraße", "Bahnhofstraße", "Schulstraße", "Gartenstraße",
            "Kirchstraße", "Lindenweg", "Berliner Straße"
    );

    public static void main(String[] args) {
        System.out.println("Starte Lasttest mit " + NUM_THREADS + " Threads und " +
                REQUESTS_PER_THREAD + " Anfragen pro Thread");

        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        long startTime = System.currentTimeMillis();

        // Starte Threads
        for (int i = 0; i < NUM_THREADS; i++) {
            executor.execute(LoadTester::sendRequests);
        }

        // Shutdown und warte auf Beendigung
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long duration = System.currentTimeMillis() - startTime;

        // Ausgabe der Statistiken
        printStatistics(duration);
    }

    private static void sendRequests() {
        for (int i = 0; i < REQUESTS_PER_THREAD; i++) {
            String request = generateRequest();
            sendSingleRequest(request);

            // Kurze Pause zwischen Anfragen
            try {
                Thread.sleep(RANDOM.nextInt(100));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static String generateRequest() {
        int requestType = RANDOM.nextInt(10);

        switch (requestType) {
//            case 0: // Leere Anfrage
//                return "";
//            case 1: // Ungültiges Format
//                return "InvalidRequest123";
//            case 2: // Fehlerhafte Syntax
//                return "0A051315900B06Berlin0C0BKürbissteig"; // Fehlender Teil
//            case 3: // Sonderzeichen
//                return "0A05!@#$%0B06Test0C0BTest0D001";
//            case 4: // Zu lange Werte
//                return "0A05" + "9".repeat(20) + "0B06Berlin0C0BTeststraße0D001";
            default: // Gültige Anfrage
                return generateValidRequest();
        }
    }

    private static String generateValidRequest() {
        String plz = String.format("%05d", RANDOM.nextInt(99999));
        String city = CITIES.get(RANDOM.nextInt(CITIES.size()));
        String street = STREETS.get(RANDOM.nextInt(STREETS.size()));
        String houseNumber = String.format("%03d", RANDOM.nextInt(200));

        return String.format("0A05%s0B%02X%s0C%02X%s0D%02X%s",
                plz, city.length(), city, street.length(), street, houseNumber.length(), houseNumber);
    }

    private static void sendSingleRequest(String request) {
        try (Socket socket = new Socket("localhost", PORT);
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.write(request);
            out.newLine();
            out.flush();

            // Lese Antwort
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line).append("\n");
            }

            successfulRequests.incrementAndGet();
            System.out.printf("Thread %d - Erfolgreiche Anfrage: %s%n",
                    Thread.currentThread().getId(), response);

        } catch (Exception e) {
            failedRequests.incrementAndGet();
            System.err.printf("Thread %d - Fehler bei Anfrage: %s - %s%n",
                    Thread.currentThread().getId(), request, e.getMessage());
        }
    }

    private static void printStatistics(long duration) {
        int total = successfulRequests.get() + failedRequests.get();
        double successRate = (double) successfulRequests.get() / total * 100;
        double requestsPerSecond = (double) total / (duration / 1000.0);

        System.out.println("\n=== Lasttest-Statistiken ===");
        System.out.printf("Gesamtdauer: %.2f Sekunden%n", duration / 1000.0);
        System.out.printf("Erfolgreiche Anfragen: %d%n", successfulRequests.get());
        System.out.printf("Fehlgeschlagene Anfragen: %d%n", failedRequests.get());
        System.out.printf("Erfolgsrate: %.2f%%%n", successRate);
        System.out.printf("Durchschnittliche Anfragen pro Sekunde: %.2f%n", requestsPerSecond);
    }

}
