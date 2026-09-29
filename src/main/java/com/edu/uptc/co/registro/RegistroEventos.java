package com.edu.uptc.co.registro;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class RegistroEventos {
    private final ReentrantLock lock = new ReentrantLock();
    private final List<String> eventos = new LinkedList<>();
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
 
    /**
     * Registra un evento de forma segura entre hilos.
     * La sección entre lock() y unlock() es la sección crítica.
     */
    public void log(String mensaje) {
        lock.lock();
        try {
            String linea = "[" + LocalTime.now().format(FORMATO) + "] "
                    + Thread.currentThread().getName() + " -> " + mensaje;
            eventos.add(linea);
            System.out.println(linea);
        } finally {
            // El unlock() SIEMPRE va en finally: si algo lanza una excepción
            // dentro de la sección crítica, el lock igual debe liberarse,
            // o el aeropuerto entero queda bloqueado (deadlock por descuido).
            lock.unlock();
        }
    }
 
    /** Copia defensiva de los eventos, para mostrarlos en la interfaz gráfica. */
    public List<String> obtenerEventos() {
        lock.lock();
        try {
            return new LinkedList<>(eventos);
        } finally {
            lock.unlock();
        }
    }
}
 