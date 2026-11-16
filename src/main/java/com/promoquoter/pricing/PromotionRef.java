package com.promoquoter.pricing;

import java.util.UUID;

public record PromotionRef(UUID id, String name, int priority) {
}
