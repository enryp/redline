package com.metaformsystems.redline.model;

import jakarta.persistence.Embeddable;

/**
 * A reference to a partner organization. The identifier is the participant identifier such as a DID.
 */
@Embeddable
public record PartnerReference(String identifier, String nickname) {
}
