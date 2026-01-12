import io.translateplus.TranslatePlusClient;
import io.translateplus.*;
import java.util.*;

/**
 * Advanced usage examples for TranslatePlus Java client.
 */
public class AdvancedExample {
    public static void main(String[] args) {
        // Initialize client with custom options
        TranslatePlusClient client = new TranslatePlusClient.Builder()
            .apiKey(System.getenv("TRANSLATEPLUS_API_KEY") != null 
                ? System.getenv("TRANSLATEPLUS_API_KEY") 
                : "your-api-key-here")
            .timeout(60)
            .maxRetries(5)
            .maxConcurrent(10)
            .build();

        try {
            System.out.println("=== HTML Translation ===");
            String html = "<div><h1>Welcome</h1><p>This is a <strong>test</strong>.</p></div>";
            Map<String, Object> htmlResult = client.translateHTML(html, "en", "fr");
            System.out.println("Translated HTML:\n" + htmlResult.get("html"));
            System.out.println();

            System.out.println("=== Email Translation ===");
            Map<String, Object> emailResult = client.translateEmail(
                "Welcome to our service",
                "<p>Thank you for signing up! We are excited to have you.</p>",
                "en",
                "es"
            );
            System.out.println("Subject: " + emailResult.get("subject"));
            System.out.println("Body: " + emailResult.get("html_body"));
            System.out.println();

            System.out.println("=== Error Handling Example ===");
            try {
                Map<String, Object> result = client.translate("Hello", "en", "invalid-language");
            } catch (TranslatePlusAPIError e) {
                System.out.println("API Error: " + e.getMessage());
                System.out.println("Status Code: " + e.getStatusCode());
            } catch (TranslatePlusValidationError e) {
                System.out.println("Validation Error: " + e.getMessage());
            } catch (TranslatePlusException e) {
                System.out.println("Error: " + e.getMessage());
            }

        } catch (TranslatePlusException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
