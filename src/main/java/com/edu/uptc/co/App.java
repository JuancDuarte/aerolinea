package com.edu.uptc.co;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.edu.uptc.co.Modelo.Avion;
import com.edu.uptc.co.Modelo.EstadoAvion;
import com.edu.uptc.co.Modelo.Pista;
import com.edu.uptc.co.registro.DemostracionCarrera;
import com.edu.uptc.co.registro.DemostracionDeadLock;
import com.edu.uptc.co.registro.GestorAeropuerto;
import com.edu.uptc.co.registro.RegistroEventos;

/**
 * JavaFX App
 */
public class App extends Application {
 
    private static final int NUM_PISTAS = 2;
    private static final int NUM_PUERTAS = 3;
 
    private final RegistroEventos registro = new RegistroEventos();
    private final GestorAeropuerto gestor = new GestorAeropuerto(NUM_PISTAS, NUM_PUERTAS, registro);
    private final ExecutorService pool = Executors.newCachedThreadPool();
 
    // Lista de aviones activos, leída por el hilo de la GUI y escrita por el
    // hilo de la GUI también (al lanzar); CopyOnWriteArrayList es segura para
    // el caso típico "se agrega poco, se recorre mucho".
    private final List<Avion> aviones = new CopyOnWriteArrayList<>();
 
    private final List<Rectangle> indicadoresPista = new ArrayList<>();
    private Label labelPuertas;
    private FlowPane panelAviones;
    private TextArea areaLog;
    private int contadorAviones = 0;
 
    private Button btnProvocarDeadlock;
    private Button btnResolverDeadlock;
    private DemostracionDeadLock deadlockActivo;
 
    @Override
    public void start(Stage stage) {
        BorderPane raiz = new BorderPane();
        raiz.setPadding(new Insets(15));
 
        raiz.setTop(construirEncabezado());
 
        VBox centro = new VBox(15, construirPanelPistas(), construirPanelAviones(), construirPanelVerificacion());
        raiz.setCenter(centro);
 
        raiz.setBottom(construirPanelLog());
 
        Scene escena = new Scene(raiz, 780, 620);
        stage.setTitle("Simulación de Aeropuerto Inteligente");
        stage.setScene(escena);
        stage.show();
 
        iniciarRefrescoPeriodico();
    }
 
    private HBox construirEncabezado() {
        Spinner<Integer> spinnerAviones = new Spinner<>(1, 20, 3);
        Button btnIniciar = new Button("Lanzar avión(es)");
        btnIniciar.setOnAction(e -> {
            int cantidad = spinnerAviones.getValue();
            for (int i = 0; i < cantidad; i++) {
                lanzarAvion();
            }
        });
 
        HBox caja = new HBox(10, new Label("Cantidad:"), spinnerAviones, btnIniciar);
        caja.setAlignment(Pos.CENTER_LEFT);
        caja.setPadding(new Insets(0, 0, 10, 0));
        return caja;
    }
 
    private VBox construirPanelPistas() {
        VBox contenedor = new VBox(10);
        contenedor.setAlignment(Pos.CENTER);
 
        HBox filaPistas = new HBox(20);
        filaPistas.setAlignment(Pos.CENTER);
        for (int i = 0; i < NUM_PISTAS; i++) {
            VBox caja = new VBox(5);
            caja.setAlignment(Pos.CENTER);
            Rectangle rect = new Rectangle(90, 60);
            rect.setFill(Color.LIGHTGREEN);
            rect.setArcWidth(10);
            rect.setArcHeight(10);
            indicadoresPista.add(rect);
            caja.getChildren().addAll(new Label("Pista " + i), rect);
            filaPistas.getChildren().add(caja);
        }
 
        labelPuertas = new Label();
        contenedor.getChildren().addAll(filaPistas, labelPuertas);
        return contenedor;
    }
 
    /** Panel con una "tarjeta" (emoji + nombre + estado) por cada avión activo. */
    private VBox construirPanelAviones() {
        panelAviones = new FlowPane(10, 10);
        panelAviones.setPadding(new Insets(10));
 
        ScrollPane scroll = new ScrollPane(panelAviones);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(180);
 
        VBox caja = new VBox(5, new Label("Aviones:"), scroll);
        return caja;
    }
 
    /**
     * Panel con las dos demostraciones que pide el enunciado como
     * "verificación": condición de carrera real y deadlock real.
     * Ninguna de las dos usa el flujo normal de aviones; son experimentos
     * aparte que igual escriben en el mismo log compartido.
     */
    private VBox construirPanelVerificacion() {
        Button btnCarrera = new Button("Simular condición de carrera");
        btnCarrera.setOnAction(e -> ejecutarDemoCarrera());
 
        btnProvocarDeadlock = new Button("Provocar deadlock");
        btnProvocarDeadlock.setOnAction(e -> ejecutarDemoDeadlock());
 
        btnResolverDeadlock = new Button("Forzar recuperación del deadlock");
        btnResolverDeadlock.setDisable(true);
        btnResolverDeadlock.setOnAction(e -> resolverDemoDeadlock());
 
        HBox botones = new HBox(10, btnCarrera, btnProvocarDeadlock, btnResolverDeadlock);
        botones.setAlignment(Pos.CENTER_LEFT);
 
        VBox caja = new VBox(5, new Label("Verificación de conceptos:"), botones);
        caja.setPadding(new Insets(5, 0, 0, 0));
        return caja;
    }
 
    /** Corre en un hilo aparte para no congelar la GUI; el resultado se muestra en un diálogo. */
    private void ejecutarDemoCarrera() {
        registro.log("[DEMO-CARRERA] Iniciando demostración de condición de carrera...");
        pool.submit(() -> {
            try {
                String resultado = DemostracionCarrera.ejecutar(registro);
                Platform.runLater(() -> {
                    Alert alerta = new Alert(Alert.AlertType.INFORMATION);
                    alerta.setTitle("Condición de carrera");
                    alerta.setHeaderText("Contador incrementado por 50 hilos x 2000 veces cada uno");
                    alerta.setContentText(resultado.replace(" | ", "\n"));
                    alerta.showAndWait();
                });
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
    }
 
    /** Lanza los dos hilos que se interbloquean a propósito. */
    private void ejecutarDemoDeadlock() {
        deadlockActivo = new DemostracionDeadLock(gestor, registro);
        deadlockActivo.iniciar();
        btnProvocarDeadlock.setDisable(true);
        btnResolverDeadlock.setDisable(false);
        registro.log("[DEMO-DEADLOCK] Avión-Demo-A y Avión-Demo-B lanzados; deberían quedar "
                + "esperando uno al otro indefinidamente (revisa el log).");
    }
 
    private void resolverDemoDeadlock() {
        if (deadlockActivo != null) {
            deadlockActivo.forzarRecuperacion();
            registro.log("[DEMO-DEADLOCK] Se forzó la interrupción de ambos hilos para salir del interbloqueo.");
        }
        btnResolverDeadlock.setDisable(true);
        btnProvocarDeadlock.setDisable(false);
    }
 
    private VBox construirPanelLog() {
        areaLog = new TextArea();
        areaLog.setEditable(false);
        areaLog.setPrefRowCount(10);
 
        VBox caja = new VBox(5, new Label("Registro de eventos:"), areaLog);
        caja.setPadding(new Insets(10, 0, 0, 0));
        return caja;
    }
 
    private void lanzarAvion() {
        contadorAviones++;
        String nombre = "Avión-" + contadorAviones;
        Avion avion = new Avion(nombre, gestor, registro);
        aviones.add(avion);
        // Un hilo por avión (java.lang.Thread, gestionado aquí vía ExecutorService)
        pool.submit(avion);
    }
 
    /** Refresca la GUI cada 300 ms leyendo el estado actual del gestor y de cada avión. */
    private void iniciarRefrescoPeriodico() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(300), e -> refrescarVista()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
 
    private void refrescarVista() {
        List<Pista> estado = gestor.estadoPistas();
        for (Pista p : estado) {
            Rectangle rect = indicadoresPista.get(p.getId());
            rect.setFill(p.isOcupada() ? Color.INDIANRED : Color.LIGHTGREEN);
        }
 
        labelPuertas.setText("Puertas disponibles: " + gestor.puertasDisponibles()
                + " / " + gestor.totalPuertas());
 
        refrescarPanelAviones();
 
        areaLog.setText(String.join("\n", registro.obtenerEventos()));
        areaLog.setScrollTop(Double.MAX_VALUE);
    }
 
    private void refrescarPanelAviones() {
        panelAviones.getChildren().clear();
        for (Avion avion : aviones) {
            panelAviones.getChildren().add(construirTarjetaAvion(avion));
        }
    }
 
    /** Construye la "tarjeta" visual de un avión: emoji + nombre + estado, coloreada. */
    private VBox construirTarjetaAvion(Avion avion) {
        EstadoAvion estado = avion.getEstado();
 
        Label emoji = new Label("✈");
        emoji.setStyle("-fx-font-size: 28px; -fx-text-fill: " + colorTexto(estado) + ";"
                + " -fx-rotate: " + anguloEmoji(estado) + ";");
 
        Label nombre = new Label(avion.getNombre());
        nombre.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
 
        String detalleEstado = estado.getDescripcion()
                + (avion.getPistaActual() >= 0 ? " (pista " + avion.getPistaActual() + ")" : "");
        Label estadoLabel = new Label(detalleEstado);
        estadoLabel.setStyle("-fx-font-size: 10px;");
        estadoLabel.setWrapText(true);
        estadoLabel.setMaxWidth(90);
        estadoLabel.setAlignment(Pos.CENTER);
 
        VBox tarjeta = new VBox(3, emoji, nombre, estadoLabel);
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPrefWidth(100);
        tarjeta.setPadding(new Insets(8));
        tarjeta.setStyle("-fx-background-color: " + colorFondo(estado) + ";"
                + " -fx-background-radius: 8; -fx-border-radius: 8;"
                + " -fx-border-color: derive(" + colorFondo(estado) + ", -20%);");
        return tarjeta;
    }
 
    /** Fondo de la tarjeta según lo que esté haciendo el avión. */
    private String colorFondo(EstadoAvion estado) {
        return switch (estado) {
            case ESPERANDO_PUERTA, ESPERANDO_PISTA_ATERRIZAJE, ESPERANDO_PISTA_DESPEGUE -> "#f5e6a8"; // amarillo = espera
            case ATERRIZANDO, DESPEGANDO -> "#f5c6a8"; // naranja = en pista, en movimiento
            case EN_PUERTA -> "#a8c6f5"; // azul = en puerta
            case FINALIZADO -> "#c8f5a8"; // verde = terminó
        };
    }
 
    private String colorTexto(EstadoAvion estado) {
        return estado == EstadoAvion.FINALIZADO ? "#2e7d32" : "#333333";
    }
 
    /** Solo un detalle visual: el emoji "mira hacia arriba" al despegar/aterrizar. */
    private int anguloEmoji(EstadoAvion estado) {
        return switch (estado) {
            case DESPEGANDO -> -30;
            case ATERRIZANDO -> 30;
            default -> 0;
        };
    }
 
    @Override
    public void stop() {
        pool.shutdownNow();
    }
 
    public static void main(String[] args) {
        launch(args);
    }
}
 