package com.edu.uptc.co.registro;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

import com.edu.uptc.co.Modelo.Avion;
import com.edu.uptc.co.Modelo.EstadoAvion;
import com.edu.uptc.co.Modelo.Pista;

public class GestorAeropuerto {

    private final int numPistas;
    private final int numPuertas;
 
    private final Semaphore[] semaforosPista;
    private final Semaphore semaforoPuertas;
 
    private final ReentrantLock lockEstado = new ReentrantLock();
    private final List<Pista> pistas = new ArrayList<>();
 
    private final RegistroEventos registro;
 
    public GestorAeropuerto(int numPistas, int numPuertas, RegistroEventos registro) {
        this.numPistas = numPistas;
        this.numPuertas = numPuertas;
        this.registro = registro;
 
        this.semaforosPista = new Semaphore[numPistas];
        for (int i = 0; i < numPistas; i++) {
            // Semáforo BINARIO: 1 permiso = solo un avión a la vez en esta pista
            this.semaforosPista[i] = new Semaphore(1, true); // 'true' = orden FIFO, evita inanición
            this.pistas.add(new Pista(i));
        }
 
        // Semáforo de CONTEO: tantos permisos como puertas disponibles
        this.semaforoPuertas = new Semaphore(numPuertas, true);
    }
 
    /**
     * Reserva primero una puerta y luego una pista, en ese orden fijo,
     * y ejecuta el aterrizaje. Bloquea al avión (hilo) hasta que ambos
     * recursos estén disponibles, actualizando su estado en cada fase.
     */
    public void aterrizar(Avion avion) throws InterruptedException {
        String nombreAvion = avion.getNombre();
 
        avion.setEstado(EstadoAvion.ESPERANDO_PUERTA);
        registro.log(nombreAvion + " solicitando aterrizaje (esperando puerta)...");
 
        // Paso 1 del orden global: puerta
        semaforoPuertas.acquire();
        registro.log(nombreAvion + " reservó una puerta de embarque.");
 
        // Paso 2 del orden global: pista (bloquea hasta que alguna quede libre)
        avion.setEstado(EstadoAvion.ESPERANDO_PISTA_ATERRIZAJE);
        int idPista = ocuparPistaLibre(nombreAvion);
        avion.setPistaActual(idPista);
        avion.setEstado(EstadoAvion.ATERRIZANDO);
        registro.log(nombreAvion + " aterrizando en pista " + idPista + "...");
 
        simularOperacion(800, 1500); // tiempo de aterrizaje
 
        // El avión ya salió de la pista y rodó hacia la puerta:
        // libera la pista (otros aviones pueden aterrizar/despegar)
        liberarPista(idPista, nombreAvion);
        avion.setPistaActual(-1);
        registro.log(nombreAvion + " liberó la pista " + idPista + " y llegó a la puerta.");
    }
 
    /** Simula el tiempo de embarque/desembarque mientras ocupa la puerta. */
    public void ocuparPuerta(Avion avion) throws InterruptedException {
        String nombreAvion = avion.getNombre();
        avion.setEstado(EstadoAvion.EN_PUERTA);
        registro.log(nombreAvion + " embarcando/desembarcando en puerta...");
        simularOperacion(1000, 2000);
        semaforoPuertas.release();
        registro.log(nombreAvion + " liberó la puerta.");
    }
 
    /**
     * Despegue: solo necesita una pista (no puerta), así que no participa
     * en el riesgo de deadlock de doble recurso, pero igual usa el mismo
     * semáforo binario por pista para la exclusión mutua.
     */
    public void despegar(Avion avion) throws InterruptedException {
        String nombreAvion = avion.getNombre();
        avion.setEstado(EstadoAvion.ESPERANDO_PISTA_DESPEGUE);
        registro.log(nombreAvion + " solicitando pista para despegar...");
 
        int idPista = ocuparPistaLibre(nombreAvion);
        avion.setPistaActual(idPista);
        avion.setEstado(EstadoAvion.DESPEGANDO);
        registro.log(nombreAvion + " despegando por pista " + idPista + "...");
 
        simularOperacion(800, 1500);
 
        liberarPista(idPista, nombreAvion);
        avion.setPistaActual(-1);
        registro.log(nombreAvion + " despegó y liberó la pista " + idPista + ".");
    }
 
    // ---- Manejo interno de pistas (sección crítica sobre el arreglo semaforosPista) ----
 
    private int ocuparPistaLibre(String nombreAvion) throws InterruptedException {
        while (true) {
            for (int i = 0; i < numPistas; i++) {
                if (semaforosPista[i].tryAcquire()) {
                    lockEstado.lock();
                    try {
                        pistas.get(i).ocupar(nombreAvion);
                    } finally {
                        lockEstado.unlock();
                    }
                    return i;
                }
            }
            // Ninguna pista libre en este instante: esperar un poco y reintentar
            // en vez de bloquear indefinidamente en un solo semáforo.
            Thread.sleep(50);
        }
    }
 
    private void liberarPista(int idPista, String nombreAvion) {
        lockEstado.lock();
        try {
            pistas.get(idPista).liberar();
        } finally {
            lockEstado.unlock();
        }
        semaforosPista[idPista].release();
    }
 
    private void simularOperacion(int minMs, int maxMs) throws InterruptedException {
        int duracion = minMs + (int) (Math.random() * (maxMs - minMs));
        Thread.sleep(duracion);
    }
 
    // ---- Getters para la interfaz gráfica (lectura protegida por el lock) ----
 
    public List<Pista> estadoPistas() {
        lockEstado.lock();
        try {
            return new ArrayList<>(pistas);
        } finally {
            lockEstado.unlock();
        }
    }
 
    public int puertasDisponibles() {
        return semaforoPuertas.availablePermits();
    }
 
    public int totalPuertas() {
        return numPuertas;
    }
 
    public int totalPistas() {
        return numPistas;
    }
 
    /**
     * SOLO PARA DEMOSTRACIÓN (ver com.aeropuerto.demo.DemostracionDeadlock).
     * Entrega el semáforo binario real de una pista para que la demo pueda
     * tomarlo directamente, IGNORANDO a propósito el orden global seguro
     * (puerta -> pista) que sí respetan aterrizar()/despegar(). Así se
     * puede provocar un interbloqueo real y verificar que existe, antes
     * de mostrar por qué el orden global lo evita en el resto del sistema.
     */
    public Semaphore semaforoPistaCrudo(int idPista) {
        return semaforosPista[idPista];
    }
}
 