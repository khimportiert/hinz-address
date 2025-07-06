package test;

import java.io.*;
import java.net.Socket;

public class TcpTestClient {
    public static void main(String[] args) {
        try (
                Socket socket = new Socket("localhost", 12345);
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            String tcpMessage = "0A05131590B06Berlin0C0BKürbissteig0D018";
//            String tcpMessage = "0A05101000B06Berlin0C0BHauptstraße0D017";
//            String tcpMessage = "0A06!$&234";

            out.write(tcpMessage);
            out.newLine();
            out.flush();

            String response;
            while ((response = in.readLine()) != null) {
                System.out.println(response);
            }

        } catch (IOException e) {
            System.err.println("Fehler im Client: " + e);
        }
    }
}
