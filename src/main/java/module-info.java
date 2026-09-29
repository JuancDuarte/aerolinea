module com.eud.uptc.co {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens com.edu.uptc.co to javafx.fxml;
    exports com.edu.uptc.co;
}
