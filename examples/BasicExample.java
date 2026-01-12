import io.translateplus.TranslatePlusClient;
import io.translateplus.TranslatePlusException;
import java.util.*;

/**
 * Basic usage examples for TranslatePlus Java client.
 */
public class BasicExample {
    public static void main(String[] args) {
        // Initialize client
        TranslatePlusClient client = new TranslatePlusClient.Builder()
            .apiKey(System.getenv("TRANSLATEPLUS_API_KEY") != null 
                ? System.getenv("TRANSLATEPLUS_API_KEY") 
                : "your-api-key-here")
            .build();

        try {
            System.out.println("=== Basic Translation ===");
            Map<String, Object> result = client.translate(
                "Hello, world!",
                "en",
                "fr"
            );
            Map<String, Object> translations = (Map<String, Object>) result.get("translations");
            System.out.println("Translation: " + translations.get("translation"));
            System.out.println();

            System.out.println("=== Batch Translation ===");
            List<String> texts = Arrays.asList("Hello", "Goodbye", "Thank you");
            Map<String, Object> batchResult = client.translateBatch(texts, "en", "fr");
            List<Map<String, Object>> batchTranslations = 
                (List<Map<String, Object>>) batchResult.get("translations");
            for (Map<String, Object> translation : batchTranslations) {
                System.out.println("- " + translation.get("translation"));
            }
            System.out.println();

            System.out.println("=== Language Detection ===");
            Map<String, Object> detectResult = client.detectLanguage("Bonjour le monde");
            Map<String, Object> detection = 
                (Map<String, Object>) detectResult.get("language_detection");
            System.out.println("Detected language: " + detection.get("language"));
            System.out.println("Confidence: " + detection.get("confidence"));
            System.out.println();

            System.out.println("=== Supported Languages ===");
            Map<String, Object> langsResult = client.getSupportedLanguages();
            Map<String, String> languages = 
                (Map<String, String>) langsResult.get("supported_languages");
            System.out.println("Total languages: " + languages.size());
            System.out.println("Sample languages:");
            languages.entrySet().stream()
                .limit(5)
                .forEach(entry -> 
                    System.out.println("  " + entry.getKey() + ": " + entry.getValue())
                );
            System.out.println();

            System.out.println("=== Account Summary ===");
            Map<String, Object> summary = client.getAccountSummary();
            System.out.println("Credits remaining: " + summary.get("credits_remaining"));
            System.out.println("Plan: " + summary.get("plan_name"));
            System.out.println("Concurrency limit: " + summary.get("concurrency_limit"));

        } catch (TranslatePlusException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
