package server;

import config.AppConfig;
import model.Address;
import model.ValidationResult;
import service.AddressDecoder;
import service.AddressEncoder;
import service.PhotonClient;
import validation.AddressValidator;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
        ) {
            socket.setSoTimeout(AppConfig.getSocketTimeout()); // 5 Sekunden Timeout
            
            String tcpIn = in.readLine();
            if (tcpIn == null || tcpIn.isBlank()) {
                return;
            }

            Address inputAddress = AddressDecoder.decodeTcpStringToAddress(tcpIn);
            ValidationResult result = AddressValidator.validateWithBackup(inputAddress);

            synchronized (out) {
                if (result.isExactMatch()) {
                    AddressEncoder.writeSingleMatchTcpResponse(result, out);
                } else if (!result.possibleMatches().isEmpty()) {
                    AddressEncoder.writeMultiMatchTcpResponse(result, out);
                } else {
                    AddressEncoder.writeNoMatchTcpResponse(result, out);
                }
                out.flush();
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Bearbeiten eines Clients: " + e);
        }
    }
}