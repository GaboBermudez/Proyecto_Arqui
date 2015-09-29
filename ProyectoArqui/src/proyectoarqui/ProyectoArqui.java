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
    
    
    
    // Esto es todo lo que se comparte
    public static Semaphore bus;
    public static int[] memoriaInstrucciones = new int[160];
    public static int[] memoriaDatos = new int [88];
    public static CyclicBarrier barrier = new CyclicBarrier(3); 
    public static int[] vectorPCs = new int[2];
    public static int clock;
    public static int[][] cacheIntruccionesNucleo1 = new int [25][8];
    public static int[][] cacheDatosNucleo1 = new int [6][8];
    public static int[][] cacheIntruccionesNucleo2 = new int [25][8];
    public static int[][] cacheDatosNucleo2 = new int [6][8];
    
    /**
     *  
     */
    public static synchronized void traerDatosMemoria( int numeroBloque ){
    
    }
    

    
    /**
     * 
     */
    public static synchronized void  ejecutarInstruccion(){
    
    }

    
    /**
     * 
     * @param elPC 
     */
    public static synchronized int obtenerPC(){
        
        return 0;
    }
    
    public static void  barrera() throws InterruptedException, BrokenBarrierException{
        barrier.await();
    }
        
    public static void ponerPCs(){
    
    }    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {


        
        for (int i = 0; i <=2; i++){
          new Thread (""+i){
              
            private int miQuantum;
            private int[] registros = new int [32];

            private int PC;
            private int IR;
              
            public boolean estaEnCache( int etiqueta ){
                boolean esta = false;
        
        
                return esta;
            }
            
    
            /**
             * 
             */
            public synchronized void resolverFalloCache(int etiqueta, int nucleo){
                while(!bus.tryAcquire()){
                    this.miQuantum--;
                    try {
                        barrera();
                    } catch (InterruptedException | BrokenBarrierException ex) {
                        Logger.getLogger(ProyectoArqui.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                
                int i = etiqueta % 8;
                int j = 0;
                
                if (nucleo == 0){
                    for(int n = etiqueta; n < etiqueta+24;n++){
                        cacheIntruccionesNucleo1[i][j] = memoriaInstrucciones[n];
                        j++;
                    }
                    cacheIntruccionesNucleo1[i][j] = etiqueta;
                }
                else if (nucleo == 1){
                    for(int n = etiqueta; n < etiqueta+24;n++){
                        cacheIntruccionesNucleo2[i][j] = memoriaInstrucciones[n];
                        j++;
                    }
                    cacheIntruccionesNucleo2[i][j] = etiqueta;
                }
               
        
            }
            
            private  void cambioContexto (){
    
            }
            
            public void run(){
                this.PC = obtenerPC();
                this.IR = this.PC;
                this.PC = this.PC+4;
                if(!estaEnCache(this.IR)){
                    resolverFalloCache(this.IR, Integer.parseInt(this.getName()));
                }
                ejecutarInstruccion();
                try {
                    barrera();
                } catch (InterruptedException | BrokenBarrierException ex) {
                    Logger.getLogger(ProyectoArqui.class.getName()).log(Level.SEVERE, null, ex);
                }
                
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
