/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proyectoarqui;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
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
    public static int[] registrosNucleo1 = new int [32];
    public static int[] registrosNucleo2 = new int [32];
    private static int PCN1;
    private static int IRN1;
    private static int PCN2;
    private static int IRN2;
    private static int ProcessIDN1;
    private static int ProcessIDN2;
    private static Queue<Contexto> colaContextos = new LinkedList<>();
    private static Queue<Integer> colaProcesos = new LinkedList<>();

    private static class Contexto {
        private int PC;
        private int[] registros = new int[32];

        public Contexto(int[] registros, int PC){
            this.PC = PC;
            System.arraycopy(registros, 0, this.registros, 0, 32);
        }

        public int getPC(){
            return this.PC;
        }

        public int[] getRegistros(){
            return this.registros;
        }
    }

    /**
     *
     */
    public static synchronized void traerDatosMemoria( int numeroBloque ){

    }

    public void cargarMemoriaInstrucciones(){
        
        int bloqueMemoria = 0;
        for(int i = 1; i <= 6; ++i){
            try {
            List<Integer> instrucciones = new ArrayList<>();
            for (String line : Files.readAllLines(Paths.get(i+".txt"))) {
                for (String part : line.split("\\s+")) {
                    Integer inst = Integer.valueOf(part);
                    instrucciones.add(inst);
                }
            }
            
            for (int ins : instrucciones){
                memoriaInstrucciones[bloqueMemoria] = ins;
                ++bloqueMemoria;
            }
            
            } catch (IOException ex) {
                Logger.getLogger(ProyectoArqui.class.getName()).log(Level.SEVERE, null, ex);
            }
            int[] reg = new int[32];
            Arrays.fill(reg,0);
            Contexto contexto = new Contexto(reg, bloqueMemoria);
            colaContextos.add(contexto);
            colaProcesos.add(i);
        }
        
    }

    /**
     *
     * @param elPC
     */
    public static void cambioDeContexto(int nucleo){
        if (nucleo ==0){ //Nucleo 1
            Contexto contexto = new Contexto(registrosNucleo1,PCN1);
            colaContextos.add(contexto);
            colaProcesos.add(ProcessIDN1);
            
            Contexto contextoNuevo = colaContextos.poll();
            PCN1 = contextoNuevo.getPC();
            System.arraycopy(contextoNuevo.getRegistros(), 0, registrosNucleo1, 0, 32);
        }
        else {
            Contexto contexto = new Contexto(registrosNucleo2,PCN2);
            colaContextos.add(contexto);
            colaProcesos.add(ProcessIDN2);
            
            Contexto contextoNuevo = colaContextos.poll();
            PCN2 = contextoNuevo.getPC();
            System.arraycopy(contextoNuevo.getRegistros(), 0, registrosNucleo2, 0, 32);
        }
    }

    public static void  barrera() throws InterruptedException, BrokenBarrierException{
        barrier.await();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        for (int i = 0; i <=2; i++){
          new Thread (""+i){

            private int miQuantum;
            private int[] registros = new int [32];




            private boolean estaEnCache( int etiqueta, int nucleo ){
                boolean estaBloqueEnCache = false;
                int bloque = etiqueta%8;
                if(nucleo == 0 ){
                    if(cacheIntruccionesNucleo1[bloque][24] == etiqueta ){
                        estaBloqueEnCache = true;
                     }
                }
                if(nucleo == 1){
                    if(cacheIntruccionesNucleo2[bloque][24] == etiqueta ){
                        estaBloqueEnCache = true;
                     }
                }

                return estaBloqueEnCache;
            }


            /**
             *
             */
            public synchronized void resolverFalloCache(int etiqueta, int nucleo){
                while(!bus.tryAcquire()){
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

            public  void  ejecutarInstruccion(int [] instruccion, int nucleo){
                if(nucleo == 0){
                    switch(instruccion[0]){
                        case 8:
                            registrosNucleo1[instruccion[1]] = registrosNucleo1[instruccion[2]]+instruccion[3];
                        case 32:
                            registrosNucleo1[instruccion[1]] = registrosNucleo1[instruccion[2]]+registrosNucleo1[instruccion[3]];
                        case 34:
                            registrosNucleo1[instruccion[1]] = registrosNucleo1[instruccion[2]]-registrosNucleo1[instruccion[3]];
                        case 12:
                            registrosNucleo1[instruccion[1]] = registrosNucleo1[instruccion[2]]*registrosNucleo1[instruccion[3]];
                        case 14:
                            registrosNucleo1[instruccion[1]] = registrosNucleo1[instruccion[2]]/registrosNucleo1[instruccion[3]];
                        case 4:
                            if(instruccion[1]==0){
                                PCN1 = instruccion[3];
                            }
                        case 5:
                            if(instruccion[1]!=0){
                                PCN1 = instruccion[3];
                            }
                        case 3:
                            registrosNucleo1[31] = PCN1;
                            PCN1+=instruccion[3];
                        case 2:
                            PCN1 = registrosNucleo1[instruccion[1]];
                        default:
                            System.out.println("Fallo al ejecutar instruccion "+instruccion[0]);
                    }
                }
                else{
                    switch(instruccion[0]){
                        case 8:
                            registrosNucleo2[instruccion[1]] = registrosNucleo2[instruccion[2]]+instruccion[3];
                        case 32:
                            registrosNucleo2[instruccion[1]] = registrosNucleo2[instruccion[2]]+registrosNucleo2[instruccion[3]];
                        case 34:
                            registrosNucleo2[instruccion[1]] = registrosNucleo2[instruccion[2]]-registrosNucleo2[instruccion[3]];
                        case 12:
                            registrosNucleo2[instruccion[1]] = registrosNucleo2[instruccion[2]]*registrosNucleo2[instruccion[3]];
                        case 14:
                            registrosNucleo2[instruccion[1]] = registrosNucleo2[instruccion[2]]/registrosNucleo2[instruccion[3]];
                        case 4:
                            if(instruccion[1]==0){
                                PCN2 = instruccion[3];
                            }
                        case 5:
                            if(instruccion[1]!=0){
                                PCN2 = instruccion[3];
                            }
                        case 3:
                            registrosNucleo2[31] = PCN2;
                            PCN2+=instruccion[3];
                        case 2:
                            PCN2 = registrosNucleo2[instruccion[1]];
                        default:
                            System.out.println("Fallo al ejecutar instruccion "+instruccion[0]);
                    }
                }

            }
            private  void cambioContexto (){

            }

            @Override
            public void run(){
                int numNucleo = Integer.parseInt(this.getName());

                if (numNucleo == 0){


                }
                /*
                this.PC = obtenerPC();
                do{
                this.IR = this.PC;
                this.PC = this.PC+4;

                if(!estaEnCache(this.IR,Integer.parseInt(this.getName()))){
                    resolverFalloCache(this.IR,Integer.parseInt(this.getName()));
                }*/
               // ejecutarInstruccion();
                try {
                    barrera();
                } catch (InterruptedException | BrokenBarrierException ex) {
                    Logger.getLogger(ProyectoArqui.class.getName()).log(Level.SEVERE, null, ex);
                }


              //  }while(true);

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
