package com.promoquoter.pricing;

import java.util.UUID;

public record AppliedPromotion(UUID promotionId, String name, Money discount) {
}
