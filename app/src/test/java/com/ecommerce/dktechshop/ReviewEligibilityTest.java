package com.ecommerce.dktechshop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.ecommerce.dktechshop.data.model.Order;
import com.ecommerce.dktechshop.data.model.OrderItem;
import com.ecommerce.dktechshop.data.model.Review;
import com.ecommerce.dktechshop.data.repository.ProductRepository;
import com.ecommerce.dktechshop.data.repository.ProductRepository.ReviewEligibility;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class ReviewEligibilityTest {

    private Order buildOrder(String orderId, String status, OrderItem... items) {
        return new Order(orderId, "uid_1", "25/11/2026 • 10:00", status, "COD",
                "TP.HCM", "User", "0909123456", 1000000L, Arrays.asList(items));
    }

    @Test
    public void testNotEligibleWhenOrderNotDelivered() {
        Order processing = buildOrder("ORD-1", "Đang xử lý", new OrderItem("p1", "Mouse", "Đen", 1000000L, 1, false));
        Order delivering = buildOrder("ORD-2", "Đang giao", new OrderItem("p1", "Mouse", "Đen", 1000000L, 1, false));
        Order cancelled = buildOrder("ORD-3", "Đã hủy", new OrderItem("p1", "Mouse", "Đen", 1000000L, 1, false));

        ReviewEligibility res = ProductRepository.evaluateReviewEligibility(
                Arrays.asList(processing, delivering, cancelled), Collections.emptyList(), "p1");
        assertEquals(ReviewEligibility.Status.NOT_DELIVERED, res.status);
    }

    @Test
    public void testSingleReviewPerDeliveredOrder() {
        OrderItem item = new OrderItem("p1", "Mouse", "Đen", 1000000L, 1, false);
        Order delivered = buildOrder("ORD-1", "Đã giao", item);

        // Chưa đánh giá -> hợp lệ và gắn đúng đơn ORD-1
        ReviewEligibility first = ProductRepository.evaluateReviewEligibility(
                Collections.singletonList(delivered), Collections.emptyList(), "p1");
        assertEquals(ReviewEligibility.Status.ELIGIBLE, first.status);
        assertNotNull(first.eligibleOrder);
        assertEquals("ORD-1", first.eligibleOrder.getOrderId());

        // Sau khi đánh giá 1 lần (reviewed = true) -> không cho đánh giá lần 2
        item.setReviewed(true);
        ReviewEligibility second = ProductRepository.evaluateReviewEligibility(
                Collections.singletonList(delivered), Collections.emptyList(), "p1");
        assertEquals(ReviewEligibility.Status.ALREADY_REVIEWED, second.status);
    }

    @Test
    public void testBlockedWhenReviewDocumentAlreadyExistsForOrder() {
        OrderItem item = new OrderItem("p1", "Mouse", "Đen", 1000000L, 1, false);
        Order delivered = buildOrder("ORD-1", "Đã giao", item);
        Review existing = new Review("ORD-1_p1_Den", "p1", "Mouse", "ORD-1", "uid_1", "User", 5f, "Good", "Đen", "25/11/2026");

        ReviewEligibility res = ProductRepository.evaluateReviewEligibility(
                Collections.singletonList(delivered), Collections.singletonList(existing), "p1");
        assertEquals(ReviewEligibility.Status.ALREADY_REVIEWED, res.status);
    }

    @Test
    public void testLegacyUnlinkedReviewPreventsDuplicateForSingleOrder() {
        OrderItem item = new OrderItem("p1", "Mouse", "Đen", 1000000L, 1, false);
        Order delivered = buildOrder("ORD-1", "Đã giao", item);
        Review legacyReview = new Review("rev_123", "p1", "Mouse", "", "uid_1", "User", 5f, "Old review", "", "25/11/2026");

        ReviewEligibility res = ProductRepository.evaluateReviewEligibility(
                Collections.singletonList(delivered), Collections.singletonList(legacyReview), "p1");
        assertEquals(ReviewEligibility.Status.ALREADY_REVIEWED, res.status);
    }

    @Test
    public void testSecondDeliveredOrderAllowsOneMoreReview() {
        Order order1 = buildOrder("ORD-1", "Đã giao", new OrderItem("p1", "Mouse", "Đen", 1000000L, 1, true));
        Order order2 = buildOrder("ORD-2", "Đã giao", new OrderItem("p1", "Mouse", "Trắng", 1000000L, 1, false));

        ReviewEligibility res = ProductRepository.evaluateReviewEligibility(
                Arrays.asList(order1, order2), Collections.emptyList(), "p1");
        assertEquals(ReviewEligibility.Status.ELIGIBLE, res.status);
        assertNotNull(res.eligibleOrder);
        assertEquals("ORD-2", res.eligibleOrder.getOrderId());
        assertEquals("Trắng", res.eligibleItem.getVariant());
    }
}
