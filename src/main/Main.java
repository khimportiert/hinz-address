package main;

import server.AddressServer;

public class Main {
    public static void main(String[] args) {
        AddressServer server = new AddressServer();
        server.start();
    }
}