
package com.librarynetwork.backend.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarynetwork.backend.entity.Book;
import com.librarynetwork.backend.entity.BookCopy;
import com.librarynetwork.backend.repository.BookCopyRepository;
import com.librarynetwork.backend.repository.BookRepository;

@Service
public class OllamaService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:llama3.2:3b}")
    private String ollamaModel;

    public OllamaService(
            BookRepository bookRepository,
            BookCopyRepository bookCopyRepository,
            ObjectMapper objectMapper) {

        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.objectMapper = objectMapper;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String chat(String message) {
        String userMessage = message == null ? "" : message.trim();

        if (userMessage.isBlank()) {
            return "Please enter a message.";
        }

        List<Book> books = bookRepository.findAll();

        // Verify an exact title match before asking the AI.
        Book matchedBook = books.stream()
                .filter(book -> normalize(book.getTitle())
                        .equals(normalize(userMessage)))
                .findFirst()
                .orElse(null);

        if (matchedBook != null) {
            return describeBook(matchedBook);
        }

        try {
            String catalogue = buildCatalogueContext(books);

            String systemPrompt = """
                    You are Calico's AI Librarian.
                    Help users discover books and understand the library catalogue.
                    Be friendly, clear, and concise.

                    The catalogue data below comes from Calico's database.

                    Rules:
                    - Never invent a book, author, ISBN, branch, address, or copy status.
                    - A book record and a physical copy record are different things.
                    - If a book exists but has no copy records, say that no physical
                      copies are recorded and availability cannot be confirmed.
                    - Only call a copy available when its status is AVAILABLE.
                    - Clearly distinguish catalogue facts from general recommendations.
                    - If the catalogue does not establish an answer, say so.

                    CATALOGUE DATA:
                    """ + catalogue;

            Map<String, Object> payload = Map.of(
                    "model", ollamaModel,
                    "stream", false,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", systemPrompt),
                            Map.of(
                                    "role", "user",
                                    "content", userMessage)));

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            ollamaBaseUrl.replaceAll("/+$", "") + "/api/chat"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Ollama returned HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String reply = root.path("message").path("content").asText("");

            if (reply.isBlank()) {
                throw new IllegalStateException(
                        "Ollama returned an empty response.");
            }

            return reply;

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "The AI request was interrupted.", exception);

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not communicate with local Ollama. "
                    + "Check that Ollama is running and the model is available.",
                    exception);
        }
    }

    private String describeBook(Book book) {
        List<BookCopy> copies = bookCopyRepository.findByBookId(book.getId());

        StringBuilder result = new StringBuilder()
                .append("Yes, ")
                .append(book.getTitle())
                .append(" by ")
                .append(book.getAuthor())
                .append(" is listed in Calico's catalogue.");

        if (copies.isEmpty()) {
            result.append(
                    " However, no physical copy records are currently "
                    + "registered, so availability cannot be confirmed.");
            return result.toString();
        }

        long available = copies.stream()
                .filter(copy -> copy.getStatus() == BookCopy.Status.AVAILABLE)
                .count();

        result.append(" The catalogue records ")
                .append(copies.size())
                .append(copies.size() == 1 ? " physical copy" : " physical copies")
                .append(", of which ")
                .append(available)
                .append(available == 1 ? " is marked" : " are marked")
                .append(" AVAILABLE.");

        for (BookCopy copy : copies) {
            result.append("\n- Branch: ")
                    .append(copy.getBranch().getName())
                    .append("; status: ")
                    .append(copy.getStatus());
        }

        return result.toString();
    }

    private String buildCatalogueContext(List<Book> books) {
        List<BookCopy> copies = bookCopyRepository.findAll();

        Map<Long, List<BookCopy>> copiesByBook = copies.stream()
                .collect(Collectors.groupingBy(
                        copy -> copy.getBook().getId()));

        StringBuilder context = new StringBuilder();

        for (Book book : books) {
            context.append("\nBook: ")
                    .append(book.getTitle())
                    .append(" | Author: ")
                    .append(book.getAuthor())
                    .append(" | Publication year: ")
                    .append(book.getPublicationYear())
                    .append(" | ISBN: ")
                    .append(book.getIsbn())
                    .append('\n');

            List<BookCopy> bookCopies = copiesByBook.getOrDefault(
                    book.getId(), List.of());

            if (bookCopies.isEmpty()) {
                context.append("  No copy records found.\n");
                continue;
            }

            for (BookCopy copy : bookCopies) {
                context.append("  Copy status: ")
                        .append(copy.getStatus())
                        .append(" | Branch: ")
                        .append(copy.getBranch().getName())
                        .append(" | Address: ")
                        .append(copy.getBranch().getAddress())
                        .append('\n');
            }
        }

        if (books.isEmpty()) {
            context.append("The catalogue currently contains no book records.\n");
        }

        return context.toString();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(
                value, Normalizer.Form.NFD);

        return normalized
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}
