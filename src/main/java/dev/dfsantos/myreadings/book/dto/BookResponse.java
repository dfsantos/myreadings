package dev.dfsantos.myreadings.book.dto;

import dev.dfsantos.myreadings.book.Book;
import dev.dfsantos.myreadings.book.ReadingStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BookResponse(
        UUID id,
        String title,
        String author,
        String publisher,
        Integer publicationYear,
        Integer pageCount,
        String genre,
        String coverUrl,
        ReadingStatus status,
        LocalDate startDate,
        LocalDate endDate,
        Integer rating,
        Instant createdAt,
        Instant updatedAt
) {

    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher(),
                book.getPublicationYear(),
                book.getPageCount(),
                book.getGenre(),
                book.getCoverUrl(),
                book.getStatus(),
                book.getStartDate(),
                book.getEndDate(),
                book.getRating(),
                book.getCreatedAt(),
                book.getUpdatedAt());
    }
}
