package dev.dfsantos.myreadings.book.dto;

import dev.dfsantos.myreadings.book.ReadingStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record BookCreateRequest(

        @NotBlank(message = "O título é obrigatório")
        @Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
        String title,

        @NotBlank(message = "O autor é obrigatório")
        @Size(max = 255, message = "O autor deve ter no máximo 255 caracteres")
        String author,

        @Size(max = 255, message = "A editora deve ter no máximo 255 caracteres")
        String publisher,

        Integer publicationYear,

        @Min(value = 1, message = "O número de páginas deve ser maior que zero")
        Integer pageCount,

        @Size(max = 100, message = "O gênero deve ter no máximo 100 caracteres")
        String genre,

        @Size(max = 2048, message = "A URL da capa deve ter no máximo 2048 caracteres")
        String coverUrl,

        ReadingStatus status,

        LocalDate startDate,

        LocalDate endDate,

        @Min(value = 1, message = "A avaliação deve ser entre 1 e 5")
        @Max(value = 5, message = "A avaliação deve ser entre 1 e 5")
        Integer rating
) {
}
