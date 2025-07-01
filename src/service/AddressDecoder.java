package service;

import model.Address;

public class AddressDecoder {
    public static Address decodeTcpStringToAddress(String tcpIn) {
        String plz = null;
        String city = null;
        String street = null;
        String houseNumber = null;

        int i = 0;
        while (i < tcpIn.length()) {
            String type = tcpIn.substring(i, i + 2);
            i += 2;
            int length = Integer.parseInt(tcpIn.substring(i, i + 2), 16);
            i += 2;
            String value = tcpIn.substring(i, i + length);
            i += length;

            switch (type) {
                case "0A" -> plz = value;
                case "0B" -> city = value;
                case "0C" -> street = value;
                case "0D" -> houseNumber = value;
            }
        }

        return new Address(plz, city, street, houseNumber);
    }
}