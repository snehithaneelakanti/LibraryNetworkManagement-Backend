
package com.librarynetwork.backend.repository;

import com.librarynetwork.backend.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
}
