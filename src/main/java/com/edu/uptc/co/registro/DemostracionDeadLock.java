package com.edu.uptc.co.registro;

import java.util.concurrent.Semaphore;

public class DemostracionDeadLock {
   private final Thread avionA;
    private final Thread avionB;
 
    public DemostracionDeadLock(GestorAeropuerto gestor, RegistroEventos registro) {
        Semaphore pista0 = gestor.semaforoPistaCrudo(0);
        Semaphore pista1 = gestor.semaforoPistaCrudo(1);
 
        avionA = new Thread(() -> {
            try {
                registro.log("[DEADLOCK] Avión-A quiere pista 0...");
                pista0.acquire();
                registro.log("[DEADLOCK] Avión-A tomó pista 0. Ahora quiere pista 1...");
                Thread.sleep(300);
                pista1.acquire(); // se queda esperando para siempre si B ya tomó pista1
                registro.log("[DEADLOCK] Avión-A tomó pista 1 (no debería llegar aquí).");
                pista1.release();
                pista0.release();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                pista0.release();
                registro.log("[DEADLOCK] Avión-A fue interrumpido y liberó pista 0.");
            }
        }, "Avion-A");
 
        avionB = new Thread(() -> {
            try {
                registro.log("[DEADLOCK] Avión-B quiere pista 1...");
                pista1.acquire();
                registro.log("[DEADLOCK] Avión-B tomó pista 1. Ahora quiere pista 0...");
                Thread.sleep(300);
                pista0.acquire(); // se queda esperando para siempre si A ya tomó pista0
                registro.log("[DEADLOCK] Avión-B tomó pista 0 (no debería llegar aquí).");
                pista0.release();
                pista1.release();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                pista1.release();
                registro.log("[DEADLOCK] Avión-B fue interrumpido y liberó pista 1.");
            }
        }, "Avion-B");
    }
 
    public void iniciar() {
        avionA.start();
        avionB.start();
    }
 
    /** Única forma de salir del interbloqueo: interrumpir los hilos desde afuera. */
    public void forzarRecuperacion() {
        avionA.interrupt();
        avionB.interrupt();
    }
 
    /** true mientras ambos hilos sigan vivos (probablemente bloqueados entre sí). */
    public boolean sigueActiva() {
        return avionA.isAlive() || avionB.isAlive();
    }
}
 
