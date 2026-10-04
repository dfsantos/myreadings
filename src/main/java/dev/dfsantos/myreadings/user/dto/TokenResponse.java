package dev.dfsantos.myreadings.user.dto;

public record TokenResponse(String accessToken, long expiresInSeconds) {
}
