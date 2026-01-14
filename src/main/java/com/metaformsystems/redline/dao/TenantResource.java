package com.metaformsystems.redline.dao;

import java.util.List;

/**
 *
 */
public record TenantResource(Long id, String name, List<ParticipantResource> participants) {
}
