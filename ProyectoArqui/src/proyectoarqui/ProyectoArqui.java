/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proyectoarqui;


import java.util.concurrent.Semaphore;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author gabobermudez
 */
public class ProyectoArqui extends Thread{
    
    //Esto es lo que no se comparte
    int PC;
    int IR;
    int[] registros = new int [32];
    int[][] cacheIntrucciones = new int [25][8];
    int[][] cacheDatos = new int [6][8];
    
    // Esto es todo lo que se comparte
    public static Semaphore bus;
    public static int[] memoriaInstrucciones = new int[160];
    public static int[] memoriaDatos = new int [88];
    public static CyclicBarrier barrier = new CyclicBarrier(3); 
    
    /**
     *  
     */
    public static synchronized void traerDatosMemoria( int numeroBloque ){
    
    }
    
    public static synchronized void cambioContexto (){
    
    }
    
    /**
     * 
     */
    public static synchronized void  ejecutarInstruccion(){
    
    }
    
    /**
     * 
     */
    public static synchronized void pedirBus(){
    
    }
    
    /**
     * 
     */
    public static synchronized void resolverFalloCache(){
    
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        /* 
        
        Codigo del hilo principal 
        
        */
        
        for (int i = 1; i <=2; i++){
          new Thread ("Thread "+ i){
            public void run(){
            
            }
          }.start();
        }

        
        
        
    }
    
}

/**
 * EJEMPLO DE COMO SINCRONIZAR VARAS JAJAJA
 * 
package pruebashilos;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PruebasHilos extends Thread{

    
    public static int [] m_vector = new int[2];
    public static CyclicBarrier barrier = new CyclicBarrier(3);
    
    public static synchronized void ponerNumeroEnVector(int i ){
        m_vector[i] = i;
    }
    
    public static void imprimirVector(){
        System.out.println("Imprimiendo vector");
        for(int i =0; i< m_vector.length;++i){
            System.out.print(m_vector[i]+", ");
        }
    }
    public static void  barrera() throws InterruptedException, BrokenBarrierException{
        barrier.await();
    }
    public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
          
        System.out.println(Thread.currentThread().getName());
        for(int i=0; i<2; i++){
          new Thread("" + i){
            public void run(){
              System.out.println("Thread: " + getName() + " running");
              ponerNumeroEnVector(Integer.parseInt(getName()));
                try {
                    barrera();
                } catch (InterruptedException | BrokenBarrierException ex) {
                    Logger.getLogger(PruebasHilos.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

          }.start();

        }
        barrier.await();
        imprimirVector();

    }
    
}

 */
