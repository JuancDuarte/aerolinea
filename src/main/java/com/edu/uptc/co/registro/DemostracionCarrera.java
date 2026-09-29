package com.edu.uptc.co.registro;

import java.util.concurrent.locks.ReentrantLock;

public class DemostracionCarrera {
 
    private static final int HILOS = 50;
    private static final int INCREMENTOS_POR_HILO = 2000;
 
    private DemostracionCarrera() {
    }
 
    public static String ejecutar(RegistroEventos registro) throws InterruptedException {
        int esperado = HILOS * INCREMENTOS_POR_HILO;
 
        int sinProteccion = correr(false);
        int conProteccion = correr(true);
 
        String resultado =
                "Esperado: " + esperado
                        + " | SIN exclusión mutua: " + sinProteccion
                        + " (se perdieron " + (esperado - sinProteccion) + " incrementos)"
                        + " | CON ReentrantLock: " + conProteccion;
 
        registro.log("[DEMO-CARRERA] " + resultado);
        return resultado;
    }
 
    private static int correr(boolean protegido) throws InterruptedException {
        int[] contador = {0}; // arreglo de 1 elemento para poder mutarlo desde las lambdas
        ReentrantLock lock = new ReentrantLock();
        Thread[] hilos = new Thread[HILOS];
 
        for (int i = 0; i < HILOS; i++) {
            hilos[i] = new Thread(() -> {
                for (int j = 0; j < INCREMENTOS_POR_HILO; j++) {
                    if (protegido) {
                        lock.lock();
                        try {
                            contador[0]++; // sección crítica protegida
                        } finally {
                            lock.unlock();
                        }
                    } else {
                        contador[0]++; // sección crítica SIN protección -> condición de carrera
                    }
                }
            });
        }
 
        for (Thread h : hilos) h.start();
        for (Thread h : hilos) h.join();
        return contador[0];
    }
}