
package com.librarynetwork.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.librarynetwork.backend.entity.Book;
import com.librarynetwork.backend.repository.BookCopyRepository;
import com.librarynetwork.backend.service.BookService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;
    private final BookCopyRepository bookCopyRepository;

    public BookController(
            BookService bookService,
            BookCopyRepository bookCopyRepository
    ) {
        this.bookService = bookService;
        this.bookCopyRepository = bookCopyRepository;
    }

    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.getAllBooks();
    }

    @GetMapping("/{id}")
    public Book getBookById(@PathVariable Long id) {
        return bookService.getBookById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Book not found"
                ));
    }

    @PostMapping
    public ResponseEntity<Book> createBook(
            @Valid @RequestBody Book book
    ) {
        book.setId(null);
        Book savedBook = bookService.saveBook(book);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedBook);
    }

    @PutMapping("/{id}")
    public Book updateBook(
            @PathVariable Long id,
            @Valid @RequestBody Book request
    ) {
        Book existingBook = bookService.getBookById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Book not found"
                ));

        existingBook.setTitle(request.getTitle());
        existingBook.setAuthor(request.getAuthor());
        existingBook.setIsbn(request.getIsbn());
        existingBook.setPublicationYear(request.getPublicationYear());

        return bookService.saveBook(existingBook);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        if (bookService.getBookById(id).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Book not found"
            );
        }

        if (bookCopyRepository.existsByBookId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete a book while inventory copies reference it"
            );
        }

        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
