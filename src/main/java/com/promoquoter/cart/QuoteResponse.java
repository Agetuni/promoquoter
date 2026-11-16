package com.promoquoter.cart;

import com.promoquoter.pricing.AppliedPromotion;
import com.promoquoter.pricing.CustomerSegment;
import com.promoquoter.pricing.LineItem;
import com.promoquoter.pricing.Quote;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record QuoteResponse(
        CustomerSegment customerSegment,
        List<Line> lines,
        List<Applied> appliedPromotions,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total) {

    public record Line(UUID productId, String productName, BigDecimal unitPrice, int quantity, BigDecimal subtotal,
                       BigDecimal discount, BigDecimal total) {

        static Line from(LineItem line) {
            return new Line(line.productId(), line.productName(), line.unitPrice().amount(), line.quantity(),
                    line.subtotal().amount(), line.discount().amount(), line.total().amount());
        }
    }

    public record Applied(UUID promotionId, String name, BigDecimal discount) {

        static Applied from(AppliedPromotion applied) {
            return new Applied(applied.promotionId(), applied.name(), applied.discount().amount());
        }
    }

    static QuoteResponse from(CustomerSegment segment, Quote quote) {
        return new QuoteResponse(segment,
                quote.lines().stream().map(Line::from).toList(),
                quote.appliedPromotions().stream().map(Applied::from).toList(),
                quote.subtotal().amount(),
                quote.discount().amount(),
                quote.total().amount());
    }
}
