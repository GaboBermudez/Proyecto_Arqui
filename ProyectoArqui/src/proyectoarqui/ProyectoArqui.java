/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proyectoarqui;


import java.util.concurrent.Semaphore;
import java.util.concurrent.CyclicBarrier;
/**
 *
 * @author gabobermudez
 */
public class ProyectoArqui {
    
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
    
    /**
     *  
     */
    public void traerDatosMemoria( int numeroBloque ){}
    
    /**
     * 
     */
    public void ejecutarInstruccion(){}
    
    /**
     * 
     */
    public void pedirBus(){}
    
    /**
     * 
     */
    public void resolverFalloCache(){}
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        /* 
        
        Codigo del hilo principal 
        
        */
        
        
        //Codigo de cada hilo
        
        
        
    }
    
}
