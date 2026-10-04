package dev.dfsantos.myreadings.book;

import dev.dfsantos.myreadings.book.dto.BookCreateRequest;
import dev.dfsantos.myreadings.book.dto.BookResponse;
import dev.dfsantos.myreadings.book.dto.BookUpdateRequest;
import dev.dfsantos.myreadings.common.NotFoundException;
import dev.dfsantos.myreadings.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public BookResponse create(BookCreateRequest request) {
        UUID userId = CurrentUser.id();
        Instant now = Instant.now();

        ReadingStatus status = request.status() != null ? request.status() : ReadingStatus.QUERO_LER;

        validateDateRange(request.startDate(), request.endDate());

        Book book = new Book(
                UUID.randomUUID(),
                userId,
                request.title(),
                request.author(),
                request.publisher(),
                request.publicationYear(),
                request.pageCount(),
                request.genre(),
                request.coverUrl(),
                status,
                request.startDate(),
                request.endDate(),
                request.rating(),
                now,
                now);

        Book saved = bookRepository.save(book);

        return BookResponse.from(saved);
    }

    public BookResponse update(UUID id, BookUpdateRequest request) {
        UUID userId = CurrentUser.id();

        Book existing = bookRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Livro não encontrado"));

        LocalDate startDate = request.startDate() != null ? request.startDate() : existing.getStartDate();
        LocalDate endDate = request.endDate() != null ? request.endDate() : existing.getEndDate();

        // Valida o estado FINAL após o merge parcial, não apenas os campos enviados no
        // PATCH: um livro já persistido com uma das datas pode se tornar inconsistente
        // se só a outra data for enviada neste request.
        validateDateRange(startDate, endDate);

        Book updated = new Book(
                existing.getId(),
                existing.getUserId(),
                request.title() != null ? request.title() : existing.getTitle(),
                request.author() != null ? request.author() : existing.getAuthor(),
                request.publisher() != null ? request.publisher() : existing.getPublisher(),
                request.publicationYear() != null ? request.publicationYear() : existing.getPublicationYear(),
                request.pageCount() != null ? request.pageCount() : existing.getPageCount(),
                request.genre() != null ? request.genre() : existing.getGenre(),
                request.coverUrl() != null ? request.coverUrl() : existing.getCoverUrl(),
                request.status() != null ? request.status() : existing.getStatus(),
                startDate,
                endDate,
                request.rating() != null ? request.rating() : existing.getRating(),
                existing.getCreatedAt(),
                Instant.now());

        Book saved = bookRepository.save(updated);

        return BookResponse.from(saved);
    }

    public BookResponse findById(UUID id) {
        UUID userId = CurrentUser.id();

        Book book = bookRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Livro não encontrado"));

        return BookResponse.from(book);
    }

    public void delete(UUID id) {
        UUID userId = CurrentUser.id();

        Book existing = bookRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Livro não encontrado"));

        bookRepository.delete(existing);
    }

    public Page<BookResponse> search(BookSearchCriteria criteria, Pageable pageable) {
        UUID userId = CurrentUser.id();

        // userId nunca é opcional: é a base da Specification, aplicada sempre, independente
        // dos demais filtros presentes em BookSearchCriteria (que podem estar todos ausentes).
        Specification<Book> specification = BookSpecifications.hasUserId(userId);

        if (criteria.q() != null && !criteria.q().isBlank()) {
            specification = specification.and(BookSpecifications.matchesSearchTerm(criteria.q()));
        }

        if (criteria.status() != null) {
            specification = specification.and(BookSpecifications.hasStatus(criteria.status()));
        }

        if (criteria.genre() != null && !criteria.genre().isBlank()) {
            specification = specification.and(BookSpecifications.matchesGenre(criteria.genre()));
        }

        if (criteria.minRating() != null) {
            specification = specification.and(BookSpecifications.hasRatingGreaterThanOrEqualTo(criteria.minRating()));
        }

        if (criteria.maxRating() != null) {
            specification = specification.and(BookSpecifications.hasRatingLessThanOrEqualTo(criteria.maxRating()));
        }

        return bookRepository.findAll(specification, pageable)
                .map(BookResponse::from);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new InvalidDateRangeException(
                    "A data de término não pode ser anterior à data de início");
        }
    }
}
