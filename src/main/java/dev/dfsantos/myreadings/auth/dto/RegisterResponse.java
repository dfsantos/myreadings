package dev.dfsantos.myreadings.auth.dto;

import java.util.UUID;

public record RegisterResponse(UUID id, String email) {
}
