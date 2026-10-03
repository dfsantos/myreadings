package dev.dfsantos.myreadings.book;

import dev.dfsantos.myreadings.common.InstantStringConverter;
import dev.dfsantos.myreadings.common.LocalDateStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "TEXT")
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", nullable = false, columnDefinition = "TEXT")
    private UUID userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    private String publisher;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(name = "page_count")
    private Integer pageCount;

    private String genre;

    @Column(name = "cover_url")
    private String coverUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReadingStatus status;

    @Convert(converter = LocalDateStringConverter.class)
    @Column(name = "start_date", columnDefinition = "TEXT")
    private LocalDate startDate;

    @Convert(converter = LocalDateStringConverter.class)
    @Column(name = "end_date", columnDefinition = "TEXT")
    private LocalDate endDate;

    private Integer rating;

    @Convert(converter = InstantStringConverter.class)
    @Column(name = "created_at", nullable = false, columnDefinition = "TEXT")
    private Instant createdAt;

    @Convert(converter = InstantStringConverter.class)
    @Column(name = "updated_at", nullable = false, columnDefinition = "TEXT")
    private Instant updatedAt;

    protected Book() {
        // exigido pelo JPA
    }

    public Book(UUID id, UUID userId, String title, String author, String publisher,
                Integer publicationYear, Integer pageCount, String genre, String coverUrl,
                ReadingStatus status, LocalDate startDate, LocalDate endDate, Integer rating,
                Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.publicationYear = publicationYear;
        this.pageCount = pageCount;
        this.genre = genre;
        this.coverUrl = coverUrl;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.rating = rating;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getPublisher() {
        return publisher;
    }

    public Integer getPublicationYear() {
        return publicationYear;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public String getGenre() {
        return genre;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public ReadingStatus getStatus() {
        return status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Integer getRating() {
        return rating;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
