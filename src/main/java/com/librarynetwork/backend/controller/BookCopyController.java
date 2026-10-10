
package com.librarynetwork.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.librarynetwork.backend.entity.Book;
import com.librarynetwork.backend.entity.BookCopy;
import com.librarynetwork.backend.entity.Branch;
import com.librarynetwork.backend.repository.BookCopyRepository;
import com.librarynetwork.backend.repository.BookRepository;
import com.librarynetwork.backend.repository.BranchRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/book-copies")
public class BookCopyController {

    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;
    private final BranchRepository branchRepository;

    public BookCopyController(
            BookCopyRepository bookCopyRepository,
            BookRepository bookRepository,
            BranchRepository branchRepository
    ) {
        this.bookCopyRepository = bookCopyRepository;
        this.bookRepository = bookRepository;
        this.branchRepository = branchRepository;
    }

    @GetMapping
    public List<BookCopy> getAllCopies(
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) BookCopy.Status status
    ) {
        if (bookId != null && branchId != null && status != null) {
            return bookCopyRepository.findByBookIdAndStatus(bookId, status)
                    .stream()
                    .filter(copy -> copy.getBranch().getId().equals(branchId))
                    .toList();
        }

        if (bookId != null && status != null) {
            return bookCopyRepository.findByBookIdAndStatus(bookId, status);
        }

        if (branchId != null && status != null) {
            return bookCopyRepository.findByBranchIdAndStatus(branchId, status);
        }

        if (bookId != null) {
            return bookCopyRepository.findByBookId(bookId);
        }

        if (branchId != null) {
            return bookCopyRepository.findByBranchId(branchId);
        }

        if (status != null) {
            return bookCopyRepository.findByStatus(status);
        }

        return bookCopyRepository.findAll();
    }

    @GetMapping("/{id}")
    public BookCopy getCopyById(@PathVariable Long id) {
        return bookCopyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Book copy not found"
                ));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookCopy createCopy(
            @Valid @RequestBody CreateBookCopyRequest request
    ) {
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Book not found"
                ));

        Branch branch = branchRepository.findById(request.branchId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Branch not found"
                ));

        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setBranch(branch);
        copy.setStatus(BookCopy.Status.AVAILABLE);

        return bookCopyRepository.save(copy);
    }

    public record CreateBookCopyRequest(
            @NotNull(message = "Book ID is required")
            Long bookId,

            @NotNull(message = "Branch ID is required")
            Long branchId
    ) {}
}
