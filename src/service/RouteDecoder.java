package service;

import model.DistanceTime;

public class RouteDecoder {
    public static DistanceTime decodeTcpStringToDistanceTime(String tcpIn) {
        String msgId = "";
        String id = "";
        String pos = "";
        String zos = "";
        double rpa = 0;
        double rpv = 0;
        double rpn = 0;
        double rpf = 0;
        int rsa = 0;
        int rss = 0;
        int rsv = 0;
        int rsn = 0;
        int rso = 0;
        String tmp = "";

        // 04 : Id String
        // 07 : PEO (pos) String
        // 08 : PZO (zos) String
        // 19 : RPA (rpa) Double
        // 1A : PRV (rpv) Double
        // 1B : RPN (rpn) Double
        // 1C : RPF (rpf) Double
        // 1D : RSA (rsa) Integer
        // 1E : RSS (rss) Integer
        // 1F : RSV (rsv) Integer
        // 20 : RSN (rsn) Integer
        // 21 : RSO (rso) Integer
        // sonst : tmp String

        // #0FR0415R16:53:28, 12.07.202507...

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
                case "07" -> pos = value;
                case "08" -> zos = value;
                case "19" -> rpa = parseDoubleOrDefault(value, 0);
                case "1A" -> rpv = parseDoubleOrDefault(value, 0);
                case "1B" -> rpn = parseDoubleOrDefault(value, 0);
                case "1C" -> rpf = parseDoubleOrDefault(value, 0);
                case "1D" -> rsa = parseIntOrDefault(value, 0);
                case "1E" -> rss = parseIntOrDefault(value, 0);
                case "1F" -> rsv = parseIntOrDefault(value, 0);
                case "20" -> rsn = parseIntOrDefault(value, 0);
                case "21" -> rso = parseIntOrDefault(value, 0);
                default -> tmp = value;
            }
        }

        return new DistanceTime(id, pos, zos, rpa, rpv, rpn, rpf, rsa, rss, rsv, rsn, rso, tmp);
    }

    private static double parseDoubleOrDefault(String s, double fallback) {
        try {
            return Double.parseDouble(s.replace(",", "."));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int parseIntOrDefault(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
