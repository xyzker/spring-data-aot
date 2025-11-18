package dev.danvega.coffee.coffee;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for Coffee management.
 * Demonstrates Spring Data AOT repository operations via REST API.
 */
@RestController
@RequestMapping("/api/coffees")
public class CoffeeController {

    private final CoffeeRepository coffeeRepository;

    public CoffeeController(CoffeeRepository coffeeRepository) {
        this.coffeeRepository = coffeeRepository;
    }

    @GetMapping("/name")
    public List<Coffee> findByName() {
        return coffeeRepository.findByNaame("Cappuccino");
    }

    /**
     * Get all coffees
     */
    @GetMapping
    public List<Coffee> getAllCoffees() {
        return coffeeRepository.findAll();
    }

    /**
     * Get coffee by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Coffee> getCoffeeById(@PathVariable Long id) {
        return coffeeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Search coffees by name pattern (demonstrates AOT LIKE query derivation)
     * Example: GET /api/coffees/search?pattern=latte
     */
    @GetMapping("/search")
    public List<Coffee> searchCoffeesByName(@RequestParam String pattern) {
        return coffeeRepository.findByNameContainingIgnoreCase(pattern);
    }

    /**
     * Find coffees by size and minimum price (demonstrates AOT multi-property query)
     * Example: GET /api/coffees/filter?size=MEDIUM&minPrice=5.00
     */
    @GetMapping("/filter")
    public List<Coffee> getCoffeesBySizeAndPrice(
            @RequestParam Size size,
            @RequestParam BigDecimal minPrice) {
        return coffeeRepository.findBySizeAndPriceGreaterThan(size, minPrice);
    }

    /**
     * Find affordable coffees by size (demonstrates AOT custom @Query validation)
     * Example: GET /api/coffees/affordable?size=LARGE&maxPrice=6.00
     */
    @GetMapping("/affordable")
    public List<Coffee> getAffordableCoffees(
            @RequestParam Size size,
            @RequestParam BigDecimal maxPrice) {
        return coffeeRepository.findAffordableCoffeesBySize(size.name(), maxPrice);
    }

    /**
     * Create new coffee
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Coffee createCoffee(@RequestBody CoffeeRequest request) {
        Coffee coffee = new Coffee(
                request.name(),
                request.description(),
                request.price(),
                request.size()
        );
        return coffeeRepository.save(coffee);
    }

    /**
     * Update existing coffee
     */
    @PutMapping("/{id}")
    public ResponseEntity<Coffee> updateCoffee(@PathVariable Long id, @RequestBody CoffeeRequest request) {
        return coffeeRepository.findById(id)
                .map(existing -> {
                    Coffee updated = new Coffee(
                            id,
                            request.name(),
                            request.description(),
                            request.price(),
                            request.size()
                    );
                    return ResponseEntity.ok(coffeeRepository.save(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete coffee
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoffee(@PathVariable Long id) {
        if (coffeeRepository.existsById(id)) {
            coffeeRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Request DTO for creating/updating coffee
     */
    record CoffeeRequest(
            String name,
            String description,
            BigDecimal price,
            Size size
    ) {}
}
