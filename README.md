# HappyShop

A JavaFX project developed in IntelliJ using Maven and JDK 21 (the latest LTS version at development in 2025), designed for CI553 coursework.

## ➡️ Setup

After opening the project in IntelliJ:

- Go to the `systemSetup` package.
- Run `SetOrderFileSystem` and `SetDatabase` **once only** to set up initial files and database.

## 🚀 Running the System

- Run `Launcher` each time you want to start the system.

## 📌 Summary

✅ JavaFX project  
✅ Developed in IntelliJ with Maven  
✅ Designed for CI553 coursework  
✅ Easy setup and clean structure

## 🧩 Coursework Extensions (CI553, Anna Maria Karatas)

This fork extends the base HappyShop codebase with two features, each developed on
its own branch (`feature/organized-trolley`, `feature/stock-shortage-handling`)
and merged into `master`:

### 1. Organized Trolley
`CustomerModel.addOrganized()` replaces the plain `trolley.add(theProduct)` call in
`addToTrolley()`:
- **Merging duplicate items** — adding a product already in the trolley increases its
  `orderedQuantity` instead of creating a second line, even if other products were
  added in between.
- **Sorting by product ID** — the trolley is kept in ascending product-ID order at
  all times, using `Product`'s existing `Comparable` implementation.

### 2. Stock Shortage Handling at Checkout
`CustomerModel.checkOut()` now handles the "insufficient stock" branch fully:
- `removeInsufficientProducts()` removes the affected lines from the trolley.
- The customer is notified via the provided `RemoveProductNotifier` message window
  (wired into `CustomerClient` and `Main`, where it was previously left commented
  out), instead of only an inline label.
- The notifier window is closed on successful checkout and on cancel.

### Tests
JUnit 5 tests for both features live under `src/test/java/ci553/happyshop/client/customer/`:
- `OrganizedTrolleyTest` — merge and sort behaviour.
- `StockShortageTest` — trolley removal behaviour.

Run with `mvn test`, or via IntelliJ's built-in test runner.