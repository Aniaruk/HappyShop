module ci553.happyshop {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires javafx.graphics;

    // Test-only requirement: IntelliJ patches src/test/java's compiled classes into this
    // same module when running tests (module ci553.happyshop, see --patch-module in the
    // test run command), so the module needs to explicitly read org.junit.jupiter.api or
    // JUnit assertions throw IllegalAccessError at runtime. This does not affect the
    // shipped application, since JUnit stays a test-scope-only Maven dependency and never
    // ends up on the runtime module path for Launcher/javafx:run.
    requires org.junit.jupiter.api;

    opens ci553.happyshop to javafx.fxml;
    opens ci553.happyshop.client to javafx.fxml;
    opens ci553.happyshop.client.customer;
    opens ci553.happyshop.client.picker;
    opens ci553.happyshop.client.orderTracker;
    opens ci553.happyshop.client.warehouse;
    opens ci553.happyshop.client.emergency;

    exports ci553.happyshop;
    exports ci553.happyshop.client;
    exports ci553.happyshop.utility;
    exports ci553.happyshop.client.customer;
    exports ci553.happyshop.client.orderTracker;
    exports ci553.happyshop.client.emergency;
    exports ci553.happyshop.systemSetup;

}