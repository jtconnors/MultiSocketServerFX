
module MultiSocketServerFX {
    requires com.jtconnors.socket;
    requires java.base;
    requires java.logging;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    exports multisocketserverfx;
    opens multisocketserverfx to javafx.fxml;
}
