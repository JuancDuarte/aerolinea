package com.edu.uptc.co.Modelo;

import com.edu.uptc.co.registro.GestorAeropuerto;
import com.edu.uptc.co.registro.RegistroEventos;

public class Avion implements Runnable {
 
    private final String nombre;
    private final GestorAeropuerto gestor;
    private final RegistroEventos registro;

    private volatile EstadoAvion estado = EstadoAvion.ESPERANDO_PUERTA;
    private volatile int pistaActual = -1; // -1 = no está en ninguna pista ahora mismo
 
    public Avion(String nombre, GestorAeropuerto gestor, RegistroEventos registro) {
        this.nombre = nombre;
        this.gestor = gestor;
        this.registro = registro;
    }
 
    public String getNombre() {
        return nombre;
    }
    public EstadoAvion getEstado() {
        return estado;
    }
    public void setEstado(EstadoAvion estado) {
        this.estado = estado;
    }
    public int getPistaActual() {
        return pistaActual;
    }
    public void setPistaActual(int pistaActual) {
        this.pistaActual = pistaActual;
    }
 
    @Override
    public void run() {
        try {
            // 1) Aterrizaje: requiere puerta + pista simultáneamente disponibles
            gestor.aterrizar(this);
 
            // 2) Ocupa la puerta para embarque/desembarque
            gestor.ocuparPuerta(this);
 
            // 3) Despegue: requiere solo una pista
            gestor.despegar(this);
 
            registro.log(nombre + " completó su operación en el aeropuerto.");
        } catch (InterruptedException e) {
            // Buena práctica al capturar InterruptedException: restaurar el
            // estado de interrupción del hilo en vez de tragarse el error.
            Thread.currentThread().interrupt();
            registro.log(nombre + " fue interrumpido y no completó su operación.");
        }
    }

}
 