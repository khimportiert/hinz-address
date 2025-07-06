package main;

import server.AddressServer;

public class Main {
    public static void main(String[] args) {
        AddressServer server = new AddressServer();
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        server.start();
    }
}