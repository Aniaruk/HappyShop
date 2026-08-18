package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Order;
import ci553.happyshop.catalogue.Product;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.orderManagement.OrderHub;
import ci553.happyshop.utility.StorageLocation;
import ci553.happyshop.utility.ProductListFormatter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Organized Trolley + Stock Shortage extensions implemented here.
 * - addToTrolley() merges duplicate product IDs and keeps the trolley sorted by product ID.
 * - checkOut() removes any product with insufficient stock from the trolley and notifies
 *   the customer via RemoveProductNotifier instead of just showing a text message.
 */
public class CustomerModel {
    public CustomerView cusView;
    public DatabaseRW databaseRW; //Interface type, not specific implementation
                                  //Benefits: Flexibility: Easily change the database implementation.
    public RemoveProductNotifier removeProductNotifier; // injected by CustomerClient; notifies the
                                                          // customer when items are removed at checkout

    private Product theProduct =null; // product found from search
    private ArrayList<Product> trolley =  new ArrayList<>(); // a list of products in trolley

    // Four UI elements to be passed to CustomerView for display updates.
    private String imageName = "imageHolder.jpg";                // Image to show in product preview (Search Page)
    private String displayLaSearchResult = "No Product was searched yet"; // Label showing search result message (Search Page)
    private String displayTaTrolley = "";                                // Text area content showing current trolley items (Trolley Page)
    private String displayTaReceipt = "";                                // Text area content showing receipt after checkout (Receipt Page)

    //SELECT productID, description, image, unitPrice,inStock quantity
    void search() throws SQLException {
        String productId = cusView.tfId.getText().trim();
        if(!productId.isEmpty()){
            theProduct = databaseRW.searchByProductId(productId); //search database
            if(theProduct != null && theProduct.getStockQuantity()>0){
                double unitPrice = theProduct.getUnitPrice();
                String description = theProduct.getProductDescription();
                int stock = theProduct.getStockQuantity();

                String baseInfo = String.format("Product_Id: %s\n%s,\nPrice: £%.2f", productId, description, unitPrice);
                String quantityInfo = stock < 100 ? String.format("\n%d units left.", stock) : "";
                displayLaSearchResult = baseInfo + quantityInfo;
                System.out.println(displayLaSearchResult);
            }
            else{
                theProduct=null;
                displayLaSearchResult = "No Product was found with ID " + productId;
                System.out.println("No Product was found with ID " + productId);
            }
        }else{
            theProduct=null;
            displayLaSearchResult = "Please type ProductID";
            System.out.println("Please type ProductID.");
        }
        updateView();
    }

    void addToTrolley(){
        if(theProduct!= null){
            // Organized Trolley extension: merge duplicate product IDs and keep the
            // trolley sorted by product ID (ascending), instead of just appending.
            addOrganized(theProduct);
            displayTaTrolley = ProductListFormatter.buildString(trolley); //build a String for trolley so that we can show it
        }
        else{
            displayLaSearchResult = "Please search for an available product before adding it to the trolley";
            System.out.println("must search and get an available product before add to trolley");
        }
        displayTaReceipt=""; // Clear receipt to switch back to trolleyPage (receipt shows only when not empty)
        updateView();
    }

    /**
     * Adds a product to the trolley in an "organized" way:
     * 1. Merging Duplicate Items — if a product with the same productId is already in the
     *    trolley, its orderedQuantity is increased instead of adding a second line. This
     *    still applies even if other, different products were added in between.
     * 2. Sorting Items by Product ID — the trolley is always kept in ascending productId
     *    order, so newly merged/inserted items appear in the right place.
     *
     * Package-private (not private) so it can be exercised directly by unit tests without
     * needing a JavaFX view/search step.
     *
     * @param product the product (with orderedQuantity already set, default 1) to add
     */
    void addOrganized(Product product) {
        for (Product existing : trolley) {
            if (existing.getProductId().equals(product.getProductId())) {
                existing.setOrderedQuantity(existing.getOrderedQuantity() + product.getOrderedQuantity());
                return; // trolley order is unaffected by a quantity-only change
            }
        }
        // New product ID: insert then re-sort using Product's natural ordering (by productId).
        trolley.add(product);
        trolley.sort(Product::compareTo);
    }

    void checkOut() throws IOException, SQLException {
        if(!trolley.isEmpty()){
            // The trolley is already merged and sorted by product ID (Organized Trolley
            // extension, via addOrganized()), so no separate grouping step is needed here.
            // NOTE: the previous groupProductsById() step was removed after manual testing
            // revealed a bug — it rebuilt each Product via the 5-arg constructor, which
            // resets orderedQuantity to its class-default of 1, silently discarding the
            // real merged quantity before the stock check. That masked insufficient-stock
            // cases whenever a trolley line had quantity > 1. Passing the trolley straight
            // through avoids the bug entirely instead of just working around it.
            ArrayList<Product> insufficientProducts= databaseRW.purchaseStocks(trolley);

            if(insufficientProducts.isEmpty()){ // If stock is sufficient for all products
                //get OrderHub and tell it to make a new Order
                OrderHub orderHub =OrderHub.getOrderHub();
                Order theOrder = orderHub.newOrder(trolley);
                trolley.clear();
                displayTaTrolley ="";
                displayTaReceipt = String.format(
                        "Order_ID: %s\nOrdered_Date_Time: %s\n%s",
                        theOrder.getOrderId(),
                        theOrder.getOrderedDateTime(),
                        ProductListFormatter.buildString(theOrder.getProductList())
                );
                System.out.println(displayTaReceipt);
                closeNotifierIfOpen(); // successful checkout: any earlier shortage notice is no longer relevant
            }
            else{ // Some products have insufficient stock — build an error message to inform the customer
                StringBuilder errorMsg = new StringBuilder();
                for(Product p : insufficientProducts){
                    errorMsg.append("\u2022 "+ p.getProductId()).append(", ")
                            .append(p.getProductDescription()).append(" (Only ")
                            .append(p.getStockQuantity()).append(" available, ")
                            .append(p.getOrderedQuantity()).append(" requested)\n");
                }
                theProduct=null;

                // Stock Shortage extension:
                // 1. Remove the insufficient-stock products from the trolley.
                // 2. Notify the customer via the RemoveProductNotifier window (rather than
                //    only the inline search-result label), listing what was removed and
                //    what they can do next (checkout as-is, re-add up to available stock,
                //    or cancel).
                removeInsufficientProducts(insufficientProducts);
                displayTaTrolley = ProductListFormatter.buildString(trolley);

                if (removeProductNotifier != null) {
                    removeProductNotifier.showRemovalMsg(errorMsg.toString());
                } else {
                    // Fallback so the customer is still informed even if the notifier
                    // was not wired in (e.g. standalone CustomerClient in tests).
                    displayLaSearchResult = "Checkout failed due to insufficient stock for the following products:\n" + errorMsg;
                }
                System.out.println("stock is not enough");
            }
        }
        else{
            displayTaTrolley = "Your trolley is empty";
            System.out.println("Your trolley is empty");
        }
        updateView();
    }

    /**
     * Removes every product that had insufficient stock from the trolley entirely
     * (rather than merely capping its quantity), matching the original TODO guidance.
     * Package-private for direct unit testing.
     */
    void removeInsufficientProducts(ArrayList<Product> insufficientProducts) {
        for (Product insufficient : insufficientProducts) {
            trolley.removeIf(p -> p.getProductId().equals(insufficient.getProductId()));
        }
    }

    private void closeNotifierIfOpen() {
        if (removeProductNotifier != null) {
            removeProductNotifier.closeNotifierWindow();
        }
    }

    void cancel(){
        trolley.clear();
        displayTaTrolley="";
        closeNotifierIfOpen(); // discard any pending shortage notice when the customer cancels
        updateView();
    }
    void closeReceipt(){
        displayTaReceipt="";
    }

    void updateView() {
        if(theProduct != null){
            imageName = theProduct.getProductImageName();
            String relativeImageUrl = StorageLocation.imageFolder +imageName; //relative file path, eg images/0001.jpg
            // Get the full absolute path to the image
            Path imageFullPath = Paths.get(relativeImageUrl).toAbsolutePath();
            imageName = imageFullPath.toUri().toString(); //get the image full Uri then convert to String
            System.out.println("Image absolute path: " + imageFullPath); // Debugging to ensure path is correct
        }
        else{
            imageName = "imageHolder.jpg";
        }
        cusView.update(imageName, displayLaSearchResult, displayTaTrolley,displayTaReceipt);
    }
     // extra notes:
     //Path.toUri(): Converts a Path object (a file or a directory path) to a URI object.
     //File.toURI(): Converts a File object (a file on the filesystem) to a URI object

    //for test only
    public ArrayList<Product> getTrolley() {
        return trolley;
    }
}
