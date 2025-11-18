package dev.danvega.coffee.coffee;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * CoffeeRepository demonstrating Spring Data AOT capabilities.
 *
 * WHY AOT MATTERS:
 * - Compile-time Safety: Method names are validated during build. A typo like
 *   "findByNamme" would fail compilation, not at runtime in production.
 * - Performance: Query parsing happens once at build time, not on every startup.
 *   No reflection overhead means faster startup and lower memory usage.
 * - Native Image Ready: Works seamlessly with GraalVM native compilation for
 *   sub-second startup times perfect for serverless and cloud-native deployments.
 */
@Repository
public interface CoffeeRepository extends ListCrudRepository<Coffee, Long> {

    List<Coffee> findByNaame(String name);

    /**
     * DEMONSTRATES: Complex derived query method with multiple keywords.
     *
     * AOT analyzes this method name at compile time and generates optimized SQL:
     * SELECT * FROM coffee WHERE UPPER(name) LIKE UPPER('%' || ? || '%')
     *
     * Benefit: If you typo the method name (e.g., "findByNammeContaining"),
     * the build fails immediately rather than failing at runtime.
     */
    List<Coffee> findByNameContainingIgnoreCase(String name);

    /**
     * DEMONSTRATES: Multi-property query with comparison operators.
     *
     * AOT generates: SELECT * FROM coffee WHERE size = ? AND price > ?
     *
     * Benefit: Complex AND conditions are parsed and validated at build time.
     * Performance-wise, there's zero runtime overhead for method parsing.
     */
    List<Coffee> findBySizeAndPriceGreaterThan(Size size, BigDecimal price);

    /**
     * DEMONSTRATES: Custom SQL with compile-time validation.
     *
     * AOT validates this SQL query syntax during compilation. A typo in the
     * SQL (e.g., "SELCT" instead of "SELECT") fails the build immediately.
     *
     * Benefit: Catch SQL errors at compile time, not when a customer hits
     * this endpoint in production. Also validates parameter binding.
     */
    @Query("""
        SELECT * FROM coffee
        WHERE size = :size
        AND price <= :maxPrice
        ORDER BY price DESC
        """)
    List<Coffee> findAffordableCoffeesBySize(@Param("size") String size,
                                              @Param("maxPrice") BigDecimal maxPrice);
}
