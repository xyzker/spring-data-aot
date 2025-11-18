package dev.danvega.coffee.order;

import dev.danvega.coffee.coffee.Coffee;
import dev.danvega.coffee.coffee.CoffeeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for Order management.
 * Demonstrates Spring Data AOT repository operations for orders.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CoffeeRepository coffeeRepository;

    public OrderController(OrderRepository orderRepository,
                          OrderItemRepository orderItemRepository,
                          CoffeeRepository coffeeRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.coffeeRepository = coffeeRepository;
    }

    /**
     * Get all orders
     */
    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    /**
     * Get order by ID with items
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderWithItems> getOrderById(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrderId(id);
                    return ResponseEntity.ok(new OrderWithItems(order, items));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Find orders by customer name (demonstrates AOT simple derived query)
     * Example: GET /api/orders/customer/Alice Johnson
     */
    @GetMapping("/customer/{customerName}")
    public List<Order> getOrdersByCustomer(@PathVariable String customerName) {
        return orderRepository.findByCustomerName(customerName);
    }

    /**
     * Find recent orders by status (demonstrates AOT multi-property query with dates)
     * Example: GET /api/orders/recent?status=PENDING&since=2024-01-01T00:00:00
     */
    @GetMapping("/recent")
    public List<Order> getRecentOrdersByStatus(
            @RequestParam OrderStatus status,
            @RequestParam LocalDateTime since) {
        return orderRepository.findByStatusAndOrderDateAfter(status, since);
    }

    /**
     * Find orders containing a specific coffee (demonstrates AOT custom @Query with JOINs)
     * Example: GET /api/orders/by-coffee?coffeeName=Cappuccino
     */
    @GetMapping("/by-coffee")
    public List<Order> getOrdersByCoffee(@RequestParam String coffeeName) {
        return orderRepository.findOrdersByCoffeeName(coffeeName);
    }

    /**
     * Create new order with items
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public OrderWithItems createOrder(@RequestBody OrderRequest request) {
        // Calculate total amount
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItemRequest itemRequest : request.items()) {
            Coffee coffee = coffeeRepository.findById(itemRequest.coffeeId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Coffee not found: " + itemRequest.coffeeId()));
            BigDecimal itemTotal = coffee.price().multiply(BigDecimal.valueOf(itemRequest.quantity()));
            totalAmount = totalAmount.add(itemTotal);
        }

        // Create order
        Order order = new Order(
                request.customerId(),
                request.customerName(),
                LocalDateTime.now(),
                totalAmount,
                OrderStatus.PENDING
        );
        Order savedOrder = orderRepository.save(order);

        // Create order items
        List<OrderItem> items = request.items().stream()
                .map(itemRequest -> {
                    Coffee coffee = coffeeRepository.findById(itemRequest.coffeeId()).orElseThrow();
                    return new OrderItem(
                            savedOrder.id(),
                            itemRequest.coffeeId(),
                            itemRequest.quantity(),
                            coffee.price()
                    );
                })
                .toList();

        List<OrderItem> savedItems = orderItemRepository.saveAll(items);
        return new OrderWithItems(savedOrder, savedItems);
    }

    /**
     * Update order status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status) {
        return orderRepository.findById(id)
                .map(existing -> {
                    Order updated = new Order(
                            id,
                            existing.customerId(),
                            existing.customerName(),
                            existing.orderDate(),
                            existing.totalAmount(),
                            status
                    );
                    return ResponseEntity.ok(orderRepository.save(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete order (cascades to items via database constraints)
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        if (orderRepository.existsById(id)) {
            orderRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Request DTO for creating orders
     */
    record OrderRequest(
            Long customerId,
            String customerName,
            List<OrderItemRequest> items
    ) {}

    /**
     * Request DTO for order items
     */
    record OrderItemRequest(
            Long coffeeId,
            Integer quantity
    ) {}

    /**
     * Response DTO combining order with its items
     */
    record OrderWithItems(
            Order order,
            List<OrderItem> items
    ) {}
}
