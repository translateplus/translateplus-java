# TranslatePlus Java Client

[![Maven Central](https://img.shields.io/maven-central/v/io.translateplus/translateplus-java)](https://central.sonatype.com/artifact/io.translateplus/translateplus-java)
[![Java](https://img.shields.io/badge/Java-11+-blue.svg)](https://www.oracle.com/java/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Official Java client library for [TranslatePlus API](https://translateplus.io) - Professional translation service for text, HTML, emails, subtitles, and i18n files in 100+ languages.

**Version:** 2.1.3+ (Production/Stable)  
**Maven Central:** [io.translateplus:translateplus-java](https://central.sonatype.com/artifact/io.translateplus/translateplus-java)  
**GitHub:** [translateplus/translateplus-java](https://github.com/translateplus/translateplus-java)

## Features

- ✅ **Simple & Intuitive API** - Easy to use, Java-friendly interface
- ✅ **All Endpoints Supported** - Text, batch, HTML, email, subtitles, and i18n translation
- ✅ **Concurrent Requests** - Built-in support for parallel translations with configurable concurrency limits
- ✅ **Error Handling** - Comprehensive exception handling with detailed error messages
- ✅ **Production Ready** - Retry logic, rate limiting, and connection pooling
- ✅ **100+ Languages** - Support for all languages available in TranslatePlus

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.translateplus</groupId>
    <artifactId>translateplus-java</artifactId>
    <version>2.1.3</version>
</dependency>
```

### Gradle

Add the following to your `build.gradle`:

```gradle
dependencies {
    implementation 'io.translateplus:translateplus-java:2.1.3'
}
```

## Quick Start

```java
import io.translateplus.TranslatePlusClient;
import java.util.Map;

// Initialize client
TranslatePlusClient client = new TranslatePlusClient.Builder()
    .apiKey("your-api-key")
    .build();

// Translate a single text
Map<String, Object> result = client.translate(
    "Hello, world!",
    "en",
    "fr"
);

Map<String, Object> translations = (Map<String, Object>) result.get("translations");
System.out.println(translations.get("translation")); // "Bonjour le monde !"
```

## API Reference

### Client Configuration

```java
TranslatePlusClient client = new TranslatePlusClient.Builder()
    .apiKey("your-api-key")                    // Required: Your TranslatePlus API key
    .baseUrl("https://api.translateplus.io")   // Optional: API base URL
    .timeout(30)                               // Optional: Request timeout in seconds (default: 30)
    .maxRetries(3)                             // Optional: Maximum retries (default: 3)
    .maxConcurrent(5)                          // Optional: Max concurrent requests (default: 5)
    .build();
```

### Translation Methods

#### Translate Text

```java
Map<String, Object> result = client.translate(
    "Hello, world!",
    "en",  // Source language (or "auto" for auto-detection)
    "fr"   // Target language
);

Map<String, Object> translations = (Map<String, Object>) result.get("translations");
System.out.println(translations.get("translation"));
```

#### Batch Translation

```java
List<String> texts = Arrays.asList("Hello", "Goodbye", "Thank you");
Map<String, Object> result = client.translateBatch(texts, "en", "fr");

List<Map<String, Object>> translations = (List<Map<String, Object>>) result.get("translations");
for (Map<String, Object> translation : translations) {
    System.out.println(translation.get("translation"));
}
```

#### Translate HTML

```java
String html = "<p>Hello <b>world</b></p>";
Map<String, Object> result = client.translateHTML(html, "en", "fr");
System.out.println(result.get("html")); // "<p>Bonjour <b>monde</b></p>"
```

#### Translate Email

```java
Map<String, Object> result = client.translateEmail(
    "Welcome",
    "<p>Thank you for signing up!</p>",
    "en",
    "fr"
);
System.out.println(result.get("subject"));   // "Bienvenue"
System.out.println(result.get("html_body")); // "<p>Merci de vous être inscrit!</p>"
```

#### Translate Subtitles

```java
String subtitleContent = "1\n00:00:01,000 --> 00:00:02,000\nHello world\n";
Map<String, Object> result = client.translateSubtitles(
    subtitleContent,
    "srt",  // or "vtt"
    "en",
    "fr"
);
System.out.println(result.get("content"));
```

### Language Methods

#### Detect Language

```java
Map<String, Object> result = client.detectLanguage("Bonjour le monde");
Map<String, Object> detection = (Map<String, Object>) result.get("language_detection");
System.out.println(detection.get("language"));     // "fr"
System.out.println(detection.get("confidence"));   // 0.95
```

#### Get Supported Languages

```java
Map<String, Object> result = client.getSupportedLanguages();
Map<String, String> languages = (Map<String, String>) result.get("supported_languages");
languages.forEach((name, code) -> {
    System.out.println(name + ": " + code);
});
```

### Account Methods

#### Get Account Summary

```java
Map<String, Object> summary = client.getAccountSummary();
System.out.println("Credits remaining: " + summary.get("credits_remaining"));
System.out.println("Plan: " + summary.get("plan_name"));
System.out.println("Concurrency limit: " + summary.get("concurrency_limit"));
```

### i18n Translation Jobs

#### Create i18n Job

```java
File file = new File("/path/to/locales/en.json");
List<String> targetLanguages = Arrays.asList("fr", "es", "de");

Map<String, Object> result = client.createI18nJob(
    file,
    targetLanguages,
    "en",                    // Source language
    "https://example.com/webhook"  // Optional webhook URL
);

System.out.println("Job ID: " + result.get("job_id"));
```

#### Get Job Status

```java
Map<String, Object> status = client.getI18nJobStatus("job-123");
System.out.println("Status: " + status.get("status"));     // "pending", "processing", "completed", "failed"
System.out.println("Progress: " + status.get("progress")); // Percentage
```

#### List Jobs

```java
Map<String, Object> jobs = client.listI18nJobs(1, 20);  // page, pageSize
List<Map<String, Object>> results = (List<Map<String, Object>>) jobs.get("results");
for (Map<String, Object> job : results) {
    System.out.println("Job " + job.get("id") + ": " + job.get("status"));
}
```

## Error Handling

The client throws specific exceptions for different error types:

```java
import io.translateplus.*;

try {
    Map<String, Object> result = client.translate("Hello", "en", "fr");
} catch (TranslatePlusAuthenticationError e) {
    System.err.println("Authentication failed: " + e.getMessage());
    System.err.println("Status code: " + e.getStatusCode());
} catch (TranslatePlusInsufficientCreditsError e) {
    System.err.println("Insufficient credits: " + e.getMessage());
} catch (TranslatePlusRateLimitError e) {
    System.err.println("Rate limit exceeded: " + e.getMessage());
} catch (TranslatePlusAPIError e) {
    System.err.println("API error: " + e.getMessage());
    System.err.println("Status code: " + e.getStatusCode());
    System.err.println("Response: " + e.getResponse());
} catch (TranslatePlusValidationError e) {
    System.err.println("Validation error: " + e.getMessage());
} catch (TranslatePlusException e) {
    System.err.println("Error: " + e.getMessage());
}
```

## Advanced Usage

### Concurrent Translations

The client automatically handles concurrency limits. You can configure the maximum concurrent requests:

```java
TranslatePlusClient client = new TranslatePlusClient.Builder()
    .apiKey("your-api-key")
    .maxConcurrent(10)  // Allow up to 10 concurrent requests
    .build();
```

### Custom Timeout and Retries

```java
TranslatePlusClient client = new TranslatePlusClient.Builder()
    .apiKey("your-api-key")
    .timeout(60)        // 60 second timeout
    .maxRetries(5)      // Retry up to 5 times
    .build();
```

## Requirements

- Java 11 or higher
- Maven or Gradle for dependency management

## License

MIT License - see [LICENSE](LICENSE) file for details.

## Related Libraries

TranslatePlus provides official client libraries for multiple programming languages:

- **Python**: [translateplus-python](https://pypi.org/project/translateplus-python/) - Official PyPI package
- **JavaScript/TypeScript**: [translateplus-js](https://www.npmjs.com/package/translateplus-js) - Official npm package
- **PHP**: [translateplus-php](https://packagist.org/packages/translateplus/translateplus-php) - Official Composer package
- **Java**: [translateplus-java](https://central.sonatype.com/artifact/io.translateplus/translateplus-java) - Official Maven Central package (this library)

All libraries provide the same comprehensive API support and features. For complete documentation, visit [https://docs.translateplus.io/official-sdks](https://docs.translateplus.io/official-sdks).

## Support

- **Documentation**: [https://docs.translateplus.io](https://docs.translateplus.io)
- **API Reference**: [https://docs.translateplus.io/reference/v2/translation/translate](https://docs.translateplus.io/reference/v2/translation/translate)
- **Issues**: [GitHub Issues](https://github.com/translateplus/translateplus-java/issues)
- **Email**: support@translateplus.io

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
