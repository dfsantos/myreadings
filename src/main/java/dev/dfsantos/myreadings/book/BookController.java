package dev.dfsantos.myreadings.book;

import dev.dfsantos.myreadings.book.dto.BookCreateRequest;
import dev.dfsantos.myreadings.book.dto.BookResponse;
import dev.dfsantos.myreadings.book.dto.BookUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/books")
@Validated
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    public ResponseEntity<BookResponse> create(@Valid @RequestBody BookCreateRequest request) {
        BookResponse response = bookService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> get(@PathVariable UUID id) {
        BookResponse response = bookService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BookResponse> update(@PathVariable UUID id, @Valid @RequestBody BookUpdateRequest request) {
        BookResponse response = bookService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public Page<BookResponse> list(
            BookSearchCriteria criteria,
            // minRating/maxRating não vêm do bind implícito de BookSearchCriteria: validação
            // Bean (@Min/@Max) em parâmetro de método de controller exige a anotação no
            // próprio @RequestParam (com a classe anotada @Validated), não em um campo de
            // record bindado implicitamente — por isso são recebidos separados e usados para
            // reconstruir o criteria completo abaixo.
            @RequestParam(required = false) @Min(1) @Max(5) Integer minRating,
            @RequestParam(required = false) @Min(1) @Max(5) Integer maxRating,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        BookSearchCriteria completeCriteria =
                new BookSearchCriteria(criteria.q(), criteria.status(), criteria.genre(), minRating, maxRating);
        return bookService.search(completeCriteria, pageable);
    }
}
