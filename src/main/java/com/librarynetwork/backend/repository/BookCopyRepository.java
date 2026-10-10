
package com.librarynetwork.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.librarynetwork.backend.entity.BookCopy;

public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    List<BookCopy> findByBookId(Long bookId);

    List<BookCopy> findByBranchId(Long branchId);

    List<BookCopy> findByStatus(BookCopy.Status status);

    List<BookCopy> findByBookIdAndStatus(
            Long bookId,
            BookCopy.Status status
    );

    List<BookCopy> findByBranchIdAndStatus(
            Long branchId,
            BookCopy.Status status
    );

    boolean existsByBookId(Long bookId);

    boolean existsByBranchId(Long branchId);
}
