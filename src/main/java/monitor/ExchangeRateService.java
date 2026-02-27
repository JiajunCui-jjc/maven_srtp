package monitor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Fetches CNY -> GBP exchange rate from the free open.er-api.com API.
 * No API key required; rate limit: 1500 requests/month on free plan.
 */
public class ExchangeRateService {

    // Free API – no key needed, returns live rates
    private static final String API_URL =
            "https://open.er-api.com/v6/latest/CNY";

    /**
     * Returns the current 1 CNY -> GBP rate, or -1 if the fetch failed.
     */
    public static double fetchCnyToGbp() {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);
            conn.setRequestProperty("Accept", "application/json");

            int status = conn.getResponseCode();
            if (status != 200) {
                System.err.println("[ExchangeRateService] HTTP " + status);
                return -1;
            }

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            conn.disconnect();

            return parseGbp(sb.toString());

        } catch (Exception e) {
            System.err.println("[ExchangeRateService] Error: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Minimal JSON parser – extracts the GBP value from the rates object
     * without needing an external library.
     *
     * Example fragment: ..."GBP":0.1083...
     */
    static double parseGbp(String json) {
        String key = "\"GBP\":";
        int idx = json.indexOf(key);
        if (idx == -1) return -1;
        int start = idx + key.length();
        int end = start;
        while (end < json.length() &&
               (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) {
            end++;
        }
        try {
            return Double.parseDouble(json.substring(start, end));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
