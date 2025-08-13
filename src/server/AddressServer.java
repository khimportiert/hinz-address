package server;

import config.AppConfig;
import model.Address;
import model.DistanceTime;
import model.RouteResult;
import model.ValidationResult;
import service.*;
import validation.AddressValidator;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static service.LogManager.getCurrentTimestamp;

public class AddressServer {
    private final ExecutorService threadPool;
    private volatile boolean isRunning;
    private ServerSocket serverSocket;

    public AddressServer() {
        // Erstelle einen Thread-Pool mit einer fixen Anzahl von Threads
        this.threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        this.isRunning = true;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(AppConfig.getServerPort());
            System.out.println("Server gestartet auf Port " + AppConfig.getServerPort());

            // Load Cache
            new AddressValidator();

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.execute(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("Fehler bei Client-Verbindung: " + e);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server konnte nicht gestartet werden: " + e);
        }
    }

    public void shutdown() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Schließen des ServerSocket: " + e);
        }

        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Thread-Pool konnte nicht beendet werden");
                }
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.ISO_8859_1))
        ) {
            socket.setSoTimeout(AppConfig.getSocketTimeout()); // 5 Sekunden Timeout

            int port = socket.getPort();

//            String tcpIn = in.readLine();
//            if (tcpIn == null || tcpIn.isBlank()) {
//                return;
//            }

            StringBuilder sb = new StringBuilder();
            int c;
            while ((c = in.read()) != -1) {  // -1 bedeutet EOF
                if (c == 3) {  // 3 entspricht ETX
                    break;  // Stoppt das Lesen bei ETX
                }
                sb.append((char) c);
            }

            String tcpIn = sb.toString();

            LogManager.logMessage(port, "(=>)", tcpIn);

            if (tcpIn.startsWith("\u0002#0ER")) { // Koordinaten Request
                Address inputAddress = AddressDecoder.decodeTcpStringToAddress(tcpIn);
                ValidationResult result = AddressValidator.validateWithBackup(inputAddress);

                synchronized (out) {
                    String res;
                    if (result.isExactMatch()) {
                        res = AddressEncoder.writeSingleMatchTcpResponse(result, out);
                    } else if (!result.possibleMatches().isEmpty()) {
                        res = AddressEncoder.writeMultiMatchTcpResponse(result, out);
                    } else {
                        res = AddressEncoder.writeNoMatchTcpResponse(result, out);
                    }
                    out.flush();

                    LogManager.logMessage(port, "(<=)", res);
                }
            }

            else if (tcpIn.startsWith("\u0002#0FR")) { // Distance Time Request
                DistanceTime distanceTime = RouteDecoder.decodeTcpStringToDistanceTime(tcpIn);
                RouteResult routeResult = OrsClient.getRoute(distanceTime);

                synchronized (out) {
                    String res = RouteEncoder.writeResponse(routeResult, out);
                    out.flush();

                    LogManager.logMessage(port, "(<=)", res);
                }
            }

            else {
                String timestamp = getCurrentTimestamp();
                System.err.printf("[%s] %s%n", timestamp, "Unerwarteter TPC Request: " + tcpIn);
            }

        }
        catch (SocketTimeoutException e) {
//            String timestamp = getCurrentTimestamp();
//            System.err.printf("[%s] %s%n", timestamp, e.getMessage());
        }
        catch (Exception e) {
            String timestamp = getCurrentTimestamp();
            System.err.printf("[%s] %s%n", timestamp, "Fehler beim Bearbeiten eines Clients: " + e);
        }
    }
}