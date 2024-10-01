import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.FileReader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;

public class ShamirSecretSharing {

    private final Random random = new SecureRandom();

    // Function to split the secret into n shares
    public BigInteger[][] splitSecret(BigInteger secret, int n, int k, BigInteger prime) {
        BigInteger[] coefficients = new BigInteger[k - 1];
        BigInteger[][] shares = new BigInteger[n][2];

        // Generate random coefficients for the polynomial
        for (int i = 0; i < k - 1; i++) {
            coefficients[i] = new BigInteger(prime.bitLength(), random).mod(prime);
        }

        // Generate shares using the polynomial
        for (int i = 1; i <= n; i++) {
            BigInteger x = BigInteger.valueOf(i);
            BigInteger y = secret;
            for (int j = 0; j < k - 1; j++) {
                y = y.add(coefficients[j].multiply(x.pow(j + 1))).mod(prime);
            }
            shares[i - 1][0] = x;
            shares[i - 1][1] = y;
        }

        return shares;
    }

    // Function to reconstruct the secret using any k shares
    public BigInteger reconstructSecret(BigInteger[][] shares, int k, BigInteger prime) {
        BigInteger secret = BigInteger.ZERO;

        for (int i = 0; i < k; i++) {
            BigInteger xi = shares[i][0];
            BigInteger yi = shares[i][1];
            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;

            for (int j = 0; j < k; j++) {
                if (i != j) {
                    BigInteger xj = shares[j][0];
                    numerator = numerator.multiply(xj.negate()).mod(prime);
                    denominator = denominator.multiply(xi.subtract(xj)).mod(prime);
                }
            }

            BigInteger term = yi.multiply(numerator).multiply(denominator.modInverse(prime)).mod(prime);
            secret = secret.add(term).mod(prime);
        }

        return secret;
    }

    // Function to verify if a point is consistent with the polynomial
    public boolean verifyPoint(BigInteger x, BigInteger y, BigInteger[][] shares, int k, BigInteger prime) {
        BigInteger expectedY = BigInteger.ZERO;
        for (int i = 0; i < k; i++) {
            BigInteger xi = shares[i][0];
            BigInteger yi = shares[i][1];
            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;

            for (int j = 0; j < k; j++) {
                if (i != j) {
                    BigInteger xj = shares[j][0];
                    numerator = numerator.multiply(xj.negate()).mod(prime);
                    denominator = denominator.multiply(xi.subtract(xj)).mod(prime);
                }
            }

            BigInteger term = yi.multiply(numerator).multiply(denominator.modInverse(prime)).mod(prime);
            expectedY = expectedY.add(term).mod(prime);
        }

        return expectedY.equals(y);
    }

    public static void main(String[] args) {
        ShamirSecretSharing sss = new ShamirSecretSharing();
        Gson gson = new Gson();

        // Define a large prime number for modular arithmetic
        BigInteger prime = new BigInteger("208351617316091241234326746312124448251235562226470491514186331217050270460481");

        try {
            // Handle Test Case 1
            System.out.println("Test Case 1:");
            FileReader reader1 = new FileReader("testcase1.json");
            JsonObject jsonObject1 = JsonParser.parseReader(reader1).getAsJsonObject();
            processTestCase(sss, jsonObject1, prime);

            // Handle Test Case 2
            System.out.println("\nTest Case 2:");
            FileReader reader2 = new FileReader("testcase2.json");
            JsonObject jsonObject2 = JsonParser.parseReader(reader2).getAsJsonObject();
            processTestCaseWithValidation(sss, jsonObject2, prime);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Process each test case without validation (used for Test Case 1)
    public static void processTestCase(ShamirSecretSharing sss, JsonObject jsonObject, BigInteger prime) {
        int n = jsonObject.getAsJsonObject("keys").get("n").getAsInt();
        int k = jsonObject.getAsJsonObject("keys").get("k").getAsInt();

        BigInteger[][] shares = new BigInteger[n][2];
        int index = 0;

        // Parse the shares from the JSON
        for (Map.Entry<String, com.google.gson.JsonElement> entry : jsonObject.entrySet()) {
            if (entry.getKey().equals("keys")) continue;
            if (index >= n) break;

            JsonObject shareData = entry.getValue().getAsJsonObject();
            int base = shareData.get("base").getAsInt();
            String value = shareData.get("value").getAsString();
            BigInteger shareValue = new BigInteger(value, base);

            try {
                BigInteger xValue = new BigInteger(entry.getKey());
                shares[index][0] = xValue;
                shares[index][1] = shareValue;
                index++;
            } catch (NumberFormatException e) {
                System.err.println("Error: Invalid key format. Key: " + entry.getKey());
            }
        }

        // Reconstruct the secret
        BigInteger[][] subsetShares = new BigInteger[k][2];
        for (int i = 0; i < k; i++) {
            subsetShares[i] = shares[i];
        }
        BigInteger secret = sss.reconstructSecret(subsetShares, k, prime);
        System.out.println("Reconstructed Secret: " + secret);
    }

    // Process Test Case 2 with validation (to find wrong points)
    public static void processTestCaseWithValidation(ShamirSecretSharing sss, JsonObject jsonObject, BigInteger prime) {
        int n = jsonObject.getAsJsonObject("keys").get("n").getAsInt();
        int k = jsonObject.getAsJsonObject("keys").get("k").getAsInt();

        BigInteger[][] shares = new BigInteger[n][2];
        int index = 0;

        // Parse the shares from the JSON
        for (Map.Entry<String, com.google.gson.JsonElement> entry : jsonObject.entrySet()) {
            if (entry.getKey().equals("keys")) continue;
            if (index >= n) break;

            JsonObject shareData = entry.getValue().getAsJsonObject();
            int base = shareData.get("base").getAsInt();
            String value = shareData.get("value").getAsString();
            BigInteger shareValue = new BigInteger(value, base);

            try {
                BigInteger xValue = new BigInteger(entry.getKey());
                shares[index][0] = xValue;
                shares[index][1] = shareValue;
                index++;
            } catch (NumberFormatException e) {
                System.err.println("Error: Invalid key format. Key: " + entry.getKey());
            }
        }

        // Reconstruct the secret
        BigInteger[][] subsetShares = new BigInteger[k][2];
        for (int i = 0; i < k; i++) {
            subsetShares[i] = shares[i];
        }
        BigInteger secret = sss.reconstructSecret(subsetShares, k, prime);
        System.out.println("Reconstructed Secret: " + secret);

        // Validate the remaining points
        System.out.println("Wrong Points in Test Case 2:");
        for (int i = k; i < n; i++) {
            BigInteger x = shares[i][0];
            BigInteger y = shares[i][1];
            boolean isValid = sss.verifyPoint(x, y, subsetShares, k, prime);
            if (!isValid) {
                System.out.println("Point x = " + x + ", y = " + y + " is invalid.");
            }
        }
    }
}
