package com.metaformsystems.redline.model;

/**
 * Client credentials for accessing a protected resource.
 */
public record ClientCredentials(String clientId, String clientSecret) {
}
