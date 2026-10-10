
package com.librarynetwork.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.librarynetwork.backend.service.OllamaService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final OllamaService ollamaService;

    public AiChatController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    public record ChatRequest(
            @NotBlank(message = "Please enter a message.")
            @Size(max = 2000, message = "Messages must not exceed 2000 characters.")
            String message) {
    }

    public record ChatResponse(String reply) {
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request) {

        try {
            String reply = ollamaService.chat(request.message().trim());
            return ResponseEntity.ok(new ChatResponse(reply));

        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "The local AI service is currently unavailable.",
                    exception);
        }
    }
}
