/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proyectoarqui;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
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
    public static int[] memoriaInstrucciones = new int[640];
    public static int[] memoriaDatos = new int [88];
    public static CyclicBarrier barrier = new CyclicBarrier(3);
    public static int[] vectorPCs = new int[2];
    public static int clock;
    public static int[][] cacheInstruccionesNucleo1 = new int [25][8];
    public static int[][] cacheDatosNucleo1 = new int [6][8];
    public static int[][] cacheInstruccionesNucleo2 = new int [25][8];
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


    public static void cargarMemoriaInstrucciones(){
        
        int bloqueMemoria = 0;
        for(int i = 1; i <= 6; ++i){
            try {
            Path filePath = Paths.get("C:\\Users\\Ricardo Aguilar\\Desktop\\Universidad\\2015\\2-2015\\Arqui\\Proyecto_Arqui\\ProyectoArqui\\src\\proyectoarqui\\"+i+".txt");
            Scanner scanner = new Scanner(filePath);
            List<Integer> instrucciones = new ArrayList<>();
            while (scanner.hasNext()) {
                if (scanner.hasNextInt()) {
                    instrucciones.add(scanner.nextInt());
                } else {
                scanner.next();
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
        
        for(int i =0 ; i < memoriaInstrucciones.length; ++i){
            System.out.print(memoriaInstrucciones[i]+" ");
            if(i%16 == 0){System.out.println();}
        }
        
    }


    public static void iniciarNucleos(){
        //Carga Nucleo 1
        Contexto contextoNucleo1 = colaContextos.poll();
        PCN1 = contextoNucleo1.getPC();
        System.arraycopy(contextoNucleo1.getRegistros(), 0, registrosNucleo1, 0, 32);
        //Carga Nucleo 2
        Contexto contextoNucleo2 = colaContextos.poll();
        PCN1 = contextoNucleo2.getPC();
        System.arraycopy(contextoNucleo2.getRegistros(), 0, registrosNucleo1, 0, 32);
    }
    
    
    public static void cambioDeContexto(int nucleo){
        if (nucleo == 0){ //Nucleo 1
            Contexto contexto = new Contexto(registrosNucleo1,PCN1);
            colaContextos.add(contexto);
            colaProcesos.add(ProcessIDN1);
            
            Contexto contextoNuevo = colaContextos.poll();
            PCN1 = contextoNuevo.getPC();
            System.arraycopy(contextoNuevo.getRegistros(), 0, registrosNucleo1, 0, 32);
        }
        else { //Nucleo 2
            Contexto contexto = new Contexto(registrosNucleo2,PCN2);
            colaContextos.add(contexto);
            colaProcesos.add(ProcessIDN2);
            
            Contexto contextoNuevo = colaContextos.poll();
            PCN2 = contextoNuevo.getPC();
            System.arraycopy(contextoNuevo.getRegistros(), 0, registrosNucleo2, 0, 32);
        }
        try {
            barrera();
        } catch (InterruptedException | BrokenBarrierException ex) {
            Logger.getLogger(ProyectoArqui.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void  barrera() throws InterruptedException, BrokenBarrierException{
        barrier.await();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        cargarMemoriaInstrucciones();
        iniciarNucleos();
        for (int i = 0; i <2; i++){
          new Thread (""+i){

            private int miQuantum;
            
            private boolean estaEnCache( int etiqueta, int nucleo ){
                System.out.println("etiqueta "+etiqueta+" Nucleo "+nucleo);
                boolean estaBloqueEnCache = false;
                int bloque = etiqueta%8;
                if(nucleo == 0 ){
                    if(cacheInstruccionesNucleo1[bloque][24] == etiqueta ){
                        estaBloqueEnCache = true;
                     }
                }
                if(nucleo == 1){
                    if(cacheInstruccionesNucleo2[bloque][24] == etiqueta ){
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
                        cacheInstruccionesNucleo1[i][j] = memoriaInstrucciones[n];
                        j++;
                    }
                    cacheInstruccionesNucleo1[i][j] = etiqueta;
                }
                else if (nucleo == 1){
                    for(int n = etiqueta; n < etiqueta+24;n++){
                        cacheInstruccionesNucleo2[i][j] = memoriaInstrucciones[n];
                        j++;
                    }
                    cacheInstruccionesNucleo2[i][j] = etiqueta;
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
                this.miQuantum--;

            }
            private int[] obtenerInstruccion( int PC, int bloque ,int self){
                int numPalabra;
                numPalabra = (PC%16)/4;
                int[] palabra = new int[4] ;
                
                if(self == 0){ //Es el Nucleo 1
                    System.arraycopy(cacheInstruccionesNucleo1[bloque], numPalabra, palabra, 0, 4);
                }
                else{
                    System.arraycopy(cacheInstruccionesNucleo2[bloque], numPalabra, palabra, 0, 4);
                }
                return palabra;
            }

            
            public void run(){
                int numNucleo = Integer.parseInt(this.getName());
                while(!colaProcesos.isEmpty()){
                    if (numNucleo == 0){
                        IRN1 = PCN1;
                        int bloque = PCN1/16;
                        PCN1 +=4;
                        if(!estaEnCache(bloque, numNucleo)){
                           resolverFalloCache(bloque, numNucleo); 
                        }

                        int [] instruccion;
                        instruccion = obtenerInstruccion(PCN1, bloque, numNucleo);
                        ejecutarInstruccion( instruccion  , numNucleo );

                    }else{
                         IRN1 = PCN1;
                        int bloque = PCN1/16;
                        PCN1 +=4;
                        if(!estaEnCache(bloque, numNucleo)){
                           resolverFalloCache(bloque, numNucleo); 
                        }

                        int [] instruccion;
                        instruccion = obtenerInstruccion(PCN1, bloque, numNucleo);
                        ejecutarInstruccion( instruccion  , numNucleo );
                    }
                    if(this.miQuantum == 0){
                        cambioDeContexto(Integer.parseInt(this.getName()));
                    }
                    try {
                        barrera();
                    } catch (InterruptedException | BrokenBarrierException ex) {
                        Logger.getLogger(ProyectoArqui.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

              //  }while(true);

            }
          }.start();
        }

    }

}

