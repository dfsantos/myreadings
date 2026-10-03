package dev.dfsantos.myreadings.auth.dto;

public record TokenResponse(String accessToken, long expiresInSeconds) {
}
