package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Stock Shortage extension: removing insufficient-stock products
 * from the trolley at checkout.
 *
 * These exercise CustomerModel's package-private removeInsufficientProducts()/
 * getTrolley() directly, so they run without a JavaFX view or a database connection
 * (unlike a full checkOut() call, which also needs a live DatabaseRW and OrderHub).
 */
class StockShortageTest {

    private CustomerModel model;

    @BeforeEach
    void setUp() {
        model = new CustomerModel();
    }

    private Product product(String id) {
        return new Product(id, "Test product " + id, id + ".jpg", 9.99, 50);
    }

    @Test
    void removeInsufficientProductsDropsOnlyTheAffectedLines() {
        model.addOrganized(product("0001"));
        model.addOrganized(product("0002"));
        model.addOrganized(product("0003"));

        ArrayList<Product> insufficient = new ArrayList<>();
        insufficient.add(product("0002")); // pretend 0002 didn't have enough stock

        model.removeInsufficientProducts(insufficient);

        ArrayList<Product> trolley = model.getTrolley();
        assertEquals(2, trolley.size());
        assertTrue(trolley.stream().noneMatch(p -> p.getProductId().equals("0002")),
                "0002 should have been removed from the trolley");
        assertTrue(trolley.stream().anyMatch(p -> p.getProductId().equals("0001")));
        assertTrue(trolley.stream().anyMatch(p -> p.getProductId().equals("0003")));
    }

    @Test
    void removeInsufficientProductsWithEmptyListLeavesTrolleyUnchanged() {
        model.addOrganized(product("0001"));
        model.removeInsufficientProducts(new ArrayList<>());

        assertEquals(1, model.getTrolley().size());
    }

    @Test
    void removeInsufficientProductsCanRemoveMultipleLines() {
        model.addOrganized(product("0001"));
        model.addOrganized(product("0002"));
        model.addOrganized(product("0003"));

        ArrayList<Product> insufficient = new ArrayList<>();
        insufficient.add(product("0001"));
        insufficient.add(product("0003"));

        model.removeInsufficientProducts(insufficient);

        ArrayList<Product> trolley = model.getTrolley();
        assertEquals(1, trolley.size());
        assertEquals("0002", trolley.get(0).getProductId());
    }
}
