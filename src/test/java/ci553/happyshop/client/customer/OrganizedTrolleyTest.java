package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Organized Trolley extension: merging duplicate product-ID lines
 * and keeping the trolley sorted by product ID.
 *
 * These exercise CustomerModel's package-private addOrganized()/getTrolley() directly,
 * so they run without a JavaFX view or a database connection.
 */
class OrganizedTrolleyTest {

    private CustomerModel model;

    @BeforeEach
    void setUp() {
        model = new CustomerModel();
    }

    private Product product(String id) {
        return new Product(id, "Test product " + id, id + ".jpg", 9.99, 50);
    }

    @Test
    void addingTheSameProductTwiceMergesIntoOneLineWithCombinedQuantity() {
        model.addOrganized(product("0002")); // qty 1
        model.addOrganized(product("0002")); // qty 1 -> should merge to qty 2

        ArrayList<Product> trolley = model.getTrolley();
        assertEquals(1, trolley.size(), "duplicate product IDs should collapse into a single line");
        assertEquals(2, trolley.get(0).getOrderedQuantity());
    }

    @Test
    void mergingStillHappensWhenOtherProductsWereAddedInBetween() {
        model.addOrganized(product("0002")); // first watch
        model.addOrganized(product("0001")); // a different product in between
        model.addOrganized(product("0002")); // second watch, added later

        ArrayList<Product> trolley = model.getTrolley();
        assertEquals(2, trolley.size(), "should still only be two distinct product lines");

        Product merged = trolley.stream()
                .filter(p -> p.getProductId().equals("0002"))
                .findFirst()
                .orElseThrow();
        assertEquals(2, merged.getOrderedQuantity(),
                "0002 should have merged quantity even though 0001 was added in between");
    }

    @Test
    void trolleyIsKeptSortedAscendingByProductIdAsItemsAreAdded() {
        model.addOrganized(product("0003"));
        model.addOrganized(product("0001"));
        model.addOrganized(product("0002"));

        ArrayList<Product> trolley = model.getTrolley();
        assertEquals("0001", trolley.get(0).getProductId());
        assertEquals("0002", trolley.get(1).getProductId());
        assertEquals("0003", trolley.get(2).getProductId());
    }

    @Test
    void trolleyStaysSortedAfterAMergeThatAddsNoNewLine() {
        model.addOrganized(product("0003"));
        model.addOrganized(product("0001"));
        model.addOrganized(product("0003")); // merge, no new line, order must be unaffected

        ArrayList<Product> trolley = model.getTrolley();
        assertEquals(2, trolley.size());
        assertEquals("0001", trolley.get(0).getProductId());
        assertEquals("0003", trolley.get(1).getProductId());
        assertEquals(2, trolley.get(1).getOrderedQuantity());
    }

    @Test
    void trolleyPreservesMergedQuantityForCheckOut() {
        // Regression test for a bug found during manual testing: the original checkOut()
        // called a groupProductsById() step that rebuilt each Product via the 5-arg
        // constructor, silently resetting orderedQuantity back to its class-default of 1
        // and hiding real insufficient-stock cases whenever a line's quantity was > 1.
        // addOrganized() must keep the *same* merged quantity in the trolley (no silent
        // copy-and-reset), so whatever checkOut() passes to purchaseStocks() reflects the
        // real requested quantity.
        model.addOrganized(product("0002"));
        model.addOrganized(product("0002"));

        ArrayList<Product> trolley = model.getTrolley();
        assertEquals(1, trolley.size());
        assertEquals(2, trolley.get(0).getOrderedQuantity(),
                "merged quantity must still be 2 right before checkout would read it");
    }
}
