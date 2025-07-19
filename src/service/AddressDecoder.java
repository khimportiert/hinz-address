package service;

import model.Address;
import model.DistanceTime;

public class AddressDecoder {
    public static Address decodeTcpStringToAddress(String tcpIn) {

//        #0ER0415R16:53:28, 12.07.20250907Germany0A05133570B06Berlin0C0EStettiner Str.0D0255

        String msgId = null;
        String id = null;
        String country = null;
        String plz = null;
        String city = null;
        String street = null;
        String houseNumber = null;

        msgId = tcpIn.substring(2, 4);

        int i = 5;
        while (i < tcpIn.length()) {
            String type = tcpIn.substring(i, i + 2);
            i += 2;
            int length = Integer.parseInt(tcpIn.substring(i, i + 2), 16);
            i += 2;
            String value = tcpIn.substring(i, i + length);
            i += length;

            switch (type) {
                case "04" -> id = value;
                case "09" -> country = value;
                case "0A" -> plz = value;
                case "0B" -> city = value;
                case "0C" -> street = value;
                case "0D" -> houseNumber = value;
            }
        }

        return new Address(id, country, plz, city, street, houseNumber);
    }
}