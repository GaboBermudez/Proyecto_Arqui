/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proyectoarqui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import java.io.IOException;
import java.io.PrintWriter;
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
public class ProyectoArqui implements Runnable{

    //Esto es lo que no se comparte



    // Esto es todo lo que se comparte
    public static Semaphore bus = new Semaphore(1);
    public static Semaphore procesos = new Semaphore(1);
    public static int[] memoriaInstrucciones = new int[640];
    public static int[] memoriaDatos = new int [1408];
    public static CyclicBarrier barrier = new CyclicBarrier(4);
    public static int[] vectorPCs = new int[2];
    public static int clock;
    public static String path;
    public static boolean estaOcupadoN1;
    public static boolean estaOcupadoN2;
    public static int[][] cacheInstruccionesNucleo1 = new int [8][17];
    public static int[][] cacheDatosNucleo1 = new int [6][8];
    public static int[][] cacheInstruccionesNucleo2 = new int [8][17];
    public static int[][] cacheDatosNucleo2 = new int [6][8];
    public static int[] registrosNucleo1 = new int [32];
    public static int[] registrosNucleo2 = new int [32];
    public static int QuantumIngresado;
    private static int PCN1;
    private static int IRN1;
    private static int PCN2;
    private static int IRN2;
    private static int hiloN1;
    private static int hiloN2;
    private static int ProcessIDN1;
    private static int ProcessIDN2;
    private static Queue<Contexto> colaContextos = new LinkedList<>();
    private static Queue<Integer> colaProcesos = new LinkedList<>();
    public static int m;
    public static int b;
    
    
    
    
    public ProyectoArqui(int quantum, int m, int b, String path){
        QuantumIngresado = quantum;
        this.m = m;
        this.b = b;
        this.path = path;
    }

    @Override
    public void run() {
        try {
            iniciar();
        } catch (IOException ex) {
            Logger.getLogger(ProyectoArqui.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
        
    
    private static class Contexto {
        private int numHilo;
        private int PC;
        private int[] registros = new int[32];

        public Contexto(int[] registros, int PC, int hiloID){
            this.PC = PC;
            System.arraycopy(registros, 0, this.registros, 0, 32);
            this.numHilo = hiloID;
        }

        public int getPC(){
            return this.PC;
        }
        
        public int getNumHilo(){
            return this.numHilo;
        }
        
        
        public void setNumHilo(int hiloID){
            this.numHilo = hiloID;
        }

        public int[] getRegistros(){
            return this.registros;
        }
    }
    
    
    /**
     * Carga todos los archivos de instrucciones a la memoria de instrucciones 
     * compartida por los dos nucleos
     */
    public static void cargarMemoriaInstrucciones(){
        
        int bloqueMemoria = 0;
        int PC = 0;
        for(int i = 1; i <= 6; ++i){
            try {
            Path filePath = Paths.get(path+"/"+i+".txt");
            Scanner scanner = new Scanner(filePath);
            List<Integer> instrucciones = new ArrayList<>();
            while (scanner.hasNext()) {
                if (scanner.hasNextInt()) {
                    instrucciones.add(scanner.nextInt());
                } else {
                scanner.next();
              }
            }
            
            String palabra = "";
            for(int ins : instrucciones){
                palabra +=+ins+" ";
                if(((bloqueMemoria+1)%4)==0){
                  //  System.out.println("palabra " + palabra);
                    palabra = "";
                }
                memoriaInstrucciones[bloqueMemoria] = ins;
                ++bloqueMemoria;
            }
            
            } catch (IOException ex) {
                Logger.getLogger(ProyectoArqui.class.getName()).log(Level.SEVERE, null, ex);
            }
            int[] reg = new int[32];
            Arrays.fill(reg,0);
            Contexto contexto = new Contexto(reg, PC,i);
           // System.out.println("PC contexto "+i+": "+bloqueMemoria);
            colaContextos.add(contexto);
            colaProcesos.add(i);
            PC = bloqueMemoria;
        }
        
        
        for(int i =0 ; i < memoriaInstrucciones.length; ++i){
            System.out.print(memoriaInstrucciones[i]+" ");
            if(i%16 == 0 && i!=0 ){System.out.println();}
        }
        System.out.println();
        
    }
    
    /**
     * Inicializa los nucleos para la primer corrida
     */
    public static void iniciarNucleos(){
        //Carga Nucleo 1
        Contexto contextoNucleo1 = colaContextos.poll();
        PCN1 = contextoNucleo1.getPC();
        hiloN1 = contextoNucleo1.getNumHilo();
        //System.out.println("PC Nucleo 1: "+Integer.toString(contextoNucleo1.getPC()));
        System.arraycopy(contextoNucleo1.getRegistros(), 0, registrosNucleo1, 0, 32);
        for(int j = 0; j<8; ++j){
            cacheInstruccionesNucleo1[j][16] = -1;
        }
        estaOcupadoN1=true;
        
        ///Preguntar si hay mas hilos
        //Carga Nucleo 2
        Contexto contextoNucleo2 = colaContextos.poll();
        PCN2 = contextoNucleo2.getPC();
        hiloN2 = contextoNucleo2.getNumHilo();
        //System.out.println("PC Nucleo 2: "+Integer.toString(contextoNucleo2.getPC()));
        System.arraycopy(contextoNucleo2.getRegistros(), 0, registrosNucleo1, 0, 32);
        for(int j = 0; j<8; ++j){
            cacheInstruccionesNucleo2[j][16] = -1;
        }
        estaOcupadoN2 = true;
    }
    
    
    /**
     * Barrera para poder sincronizar el paso de ciclo de reloj
     * @throws InterruptedException
     * @throws BrokenBarrierException 
     */
    public static void  barrera() throws InterruptedException, BrokenBarrierException{
        barrier.await();
    }
    
    /**
     * Verifica si hay hilos en cola
     * @return 
     */
    public static boolean hayHilosEnCola(){
        boolean hay = true;
            if(colaContextos.isEmpty()){
                hay = false;
            }
        return hay;
    }
    
    /**
     * 
     * @param nucleo nucleo al que se le va enviar el siguiente hilo
     */
    public static void traerSiguenteHilo(int nucleo){
        if (nucleo == 0){ //Nucleo 1
            Contexto contextoNuevo = colaContextos.poll();
            PCN1 = contextoNuevo.getPC();
            System.arraycopy(contextoNuevo.getRegistros(), 0, registrosNucleo1, 0, 32);
        }
        else { //Nucleo 2
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
    
    
    /**
     * obtiene el cache correspondiente a el nucleo que lo llamo
     * @param nucleo numero de hilo que lo invoco
     * @return la cache en forma de string
     */
    public static String obtenerCache( int nucleo){
        String str ="";
        str+="Imprimiendo Cache\n";
        if(nucleo ==0 ){
            for(int i=0; i<cacheInstruccionesNucleo1.length; ++i){
                for(int j=0; j<cacheInstruccionesNucleo1[i].length;++j){
                    str+=""+cacheInstruccionesNucleo1[i][j]+" ";
                }
                str+="\n";
            }
        }
        else{
            for(int i=0; i<cacheInstruccionesNucleo2.length; ++i){
                for(int j=0; j<cacheInstruccionesNucleo2[i].length;++j){
                    str+=""+cacheInstruccionesNucleo2[i][j]+" ";
                }
                str+="\n";
            }
        }
        return str;
    }
    
    /**
     * 
     * @param nucleo nucleo al que se le acabo el Quantum
     */
    public static synchronized void cambioDeContexto(int nucleo){
        if (nucleo == 0){ //Nucleo 1
            Contexto contexto = new Contexto(registrosNucleo1,PCN1,hiloN1);
            colaContextos.add(contexto);
            colaProcesos.add(ProcessIDN1);
            //System.out.println("PC A GUARDAR NUCLEO1 "+PCN1);
            Contexto contextoNuevo = colaContextos.poll();
            PCN1 = contextoNuevo.getPC();
            System.arraycopy(contextoNuevo.getRegistros(), 0, registrosNucleo1, 0, 32);
            //System.out.println("PC EXTRAIDO NUCLEO1 "+PCN1);
        }
        else { //Nucleo 2
            Contexto contexto = new Contexto(registrosNucleo2,PCN2,hiloN2);
            colaContextos.add(contexto);
            colaProcesos.add(ProcessIDN2);
            //System.out.println("PC A GUARDAR NUCLEO2 "+PCN2);
            Contexto contextoNuevo = colaContextos.poll();
            PCN2 = contextoNuevo.getPC();
            System.arraycopy(contextoNuevo.getRegistros(), 0, registrosNucleo2, 0, 32);
           //System.out.println("PC EXTRAIDO NUCLEO2 "+PCN2);
        }
        try {
            barrera();
        } catch (InterruptedException | BrokenBarrierException ex) {
            Logger.getLogger(ProyectoArqui.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    /**
     * Programa Principal
     * @param args
     * @throws IOException 
     */
    public void iniciar() throws IOException {
        String rutaN1 = path+"/bitacoraNucleo1.txt";
        String rutaN2 = path+"/bitacoraNucleo2.txt";
        //m_salida.setVisible(true);
        
            
        File archivo1 = new File(rutaN1);
        File archivo2 = new File(rutaN2);
               
        BufferedWriter bw1 = new BufferedWriter(new FileWriter(archivo1));
        BufferedWriter bw2 = new BufferedWriter(new FileWriter(archivo2));
        PrintWriter escribir1 = new PrintWriter(bw1);//para crear el objeto que escribe en el archivo
        PrintWriter escribir2 = new PrintWriter(bw2);//para crear el objeto que escribe en el archivo
        clock = 0;                          
       
        
        cargarMemoriaInstrucciones();
        iniciarNucleos();
        
        for (int i = 0; i < 2; i++){
            new Thread (""+i){
                private boolean estoyOcupado;
                private int miQuantum;
                private boolean finalizoHilo = false;
                
                /**
                 * Verifica si un bloque esta en el cache
                 * @param etiqueta la etiqueta del bloque 
                 * @param nucleo el numero de nucleo dueño del cache
                 * @return 
                 */
                private boolean estaEnCache( int etiqueta, int nucleo ){
                    boolean estaBloqueEnCache = false;
                    int bloque = etiqueta%8;
                    if(nucleo == 0 ){
                        //System.out.println("Buscando en cache N1 con la etiqueta "+etiqueta);
                        if(cacheInstruccionesNucleo1[bloque][16] == etiqueta ){
                            estaBloqueEnCache = true;
                         }
                    }
                    if(nucleo == 1){
                        //System.out.println("Buscando en cache N2 con la etiqueta "+etiqueta);
                        if(cacheInstruccionesNucleo2[bloque][16] == etiqueta ){
                            estaBloqueEnCache = true;
                         }
                    }
                    return estaBloqueEnCache;
                }

                /**
                 * 
                 * @param PC 
                 * @param etiqueta etiqueta para buscar en memoria
                 * @param nucleo  nucleo dueño del cache
                 */
                public synchronized void resolverFalloCache(int PC, int etiqueta, int nucleo){
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
                   // System.out.println("FilaFalloCacheN1 "+i);
                    for(int n = etiqueta*16; n < (etiqueta*16)+16; n++,j++){
                        cacheInstruccionesNucleo1[i][j] = memoriaInstrucciones[n];
                        //System.out.print(memoriaInstrucciones[n]+" ");
                    }
                    cacheInstruccionesNucleo1[i][j] = etiqueta;
                }
                else if (nucleo == 1){
                   // System.out.println("FilaFalloCacheN2 "+i);
                    for(int n = etiqueta*16; n < (etiqueta*16)+16; n++,j++){
                        cacheInstruccionesNucleo2[i][j] = memoriaInstrucciones[n];
                        //System.out.print(memoriaInstrucciones[n]+" ");
                    }
                    cacheInstruccionesNucleo2[i][j] = etiqueta;
                }
                //System.out.println();
                for(int k =0 ; k<(4*(b+m+b));++k){
                    try {
                        barrera();
                        barrera();
                    } catch (InterruptedException | BrokenBarrierException ex) {
                        Logger.getLogger(ProyectoArqui.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                bus.release();
            }
            
            /**
             * Extrae la instruccion de la cache correspondiente
             * @param PC
             * @param bloque bloque de cache donde se encuentra la instruccion
             * @param self nucleo que desea obtener la instruccion
             * @return 
             */
            private int[] obtenerInstruccion(int PC, int bloque ,int self){
                int numPalabra;
                numPalabra = (PC%16)/4;
                int fila = bloque%8;
                //System.out.println("Fila"+fila);
                int[] palabra = new int[4] ;
                int i = 0;
                if(self == 0){ //Es el Nucleo 1
                    //System.arraycopy(cacheInstruccionesNucleo1[bloque], numPalabra, palabra, 0, 4);
                    for(int columna = (numPalabra*4); columna < ((numPalabra*4)+4); ++columna,++i){
                        palabra[i] = cacheInstruccionesNucleo1[fila][columna];
                        //System.out.print(cacheInstruccionesNucleo1[fila][columna]);
                    }           
                }
                else{
                    //System.arraycopy(cacheInstruccionesNucleo2[bloque], numPalabra, palabra, 0, 4);
                    for(int columna = (numPalabra*4); columna < ((numPalabra*4)+4); ++columna,++i){
                        palabra[i] = cacheInstruccionesNucleo2[fila][columna];
                        //System.out.print(cacheInstruccionesNucleo2[fila][columna]);
                    }  
                }
                //System.out.println();
                return palabra;
            }
            
            
            /**
             * 
             * @param instruccion instruccion a ejecutar
             * @param nucleo 
             */
            public  void  ejecutarInstruccion(int [] instruccion, int nucleo) throws IOException{
                
                if(nucleo == 0){                       
                    
                    switch(instruccion[0]){
                        case 8:
                           //System.out.println("Instruccion: "+instruccion[0]+" "+instruccion[1]+" "+instruccion[2]+" "+instruccion[3]);
                            registrosNucleo1[instruccion[2]] = registrosNucleo1[instruccion[1]]+instruccion[3];
                            
                            break;
                        case 32:
                            registrosNucleo1[instruccion[3]] = registrosNucleo1[instruccion[1]]+registrosNucleo1[instruccion[2]];
                            break;
                        case 34:
                            registrosNucleo1[instruccion[3]] = registrosNucleo1[instruccion[1]]-registrosNucleo1[instruccion[2]];
                            //escribirEnArchivo(verRegistros(1));
                            break;
                        case 12:
                            registrosNucleo1[instruccion[3]] = registrosNucleo1[instruccion[1]]*registrosNucleo1[instruccion[2]];
                            break;
                        case 14:
                            registrosNucleo1[instruccion[3]] = registrosNucleo1[instruccion[1]]/registrosNucleo1[instruccion[2]];
                        case 4:
                            if(registrosNucleo1[instruccion[1]]==0){
                                PCN1 += (4*instruccion[3]);
                            }
                            break;
                        case 5:
                            if(registrosNucleo1[instruccion[1]]!=0){
                                PCN1 += (4*instruccion[3]);
                            }
                            break;
                        case 3:
                            registrosNucleo1[31] = PCN1;
                            PCN1= PCN1+instruccion[3];
                            break;
                        case 2:
                            PCN1 = registrosNucleo1[instruccion[1]];
                            break;
                        case 63:
                            System.out.println("Se termina de ejecutar el hilo");
                            this.miQuantum = QuantumIngresado+1;
                            /// imprimir registros para verificacion
                            //escribirEnArchivo("Hilo "+hiloN1);
                            escribirEnArchivo(verRegistros(0));
                            escribirEnArchivo("Tamano cola "+colaContextos.size());
                            if(hayHilosEnCola()){
                                traerSiguenteHilo(0);
                            }
                            else{
                                estaOcupadoN1 = false;
                                this.estoyOcupado = false;
                            }
                            
                            break;
                        default:
                            System.out.println("Fallo al ejecutar instruccion "+instruccion[0]);
                    }
                   // escribirEnArchivo(verRegistros(0));
                    
                }
                else{
                    switch(instruccion[0]){
                        case 8:
                            registrosNucleo2[instruccion[2]] = registrosNucleo2[instruccion[1]]+instruccion[3];
                            break;
                        case 32:
                            registrosNucleo2[instruccion[3]] = registrosNucleo2[instruccion[1]]+registrosNucleo2[instruccion[2]];
                            break;
                        case 34:
                            registrosNucleo2[instruccion[3]] = registrosNucleo2[instruccion[1]]-registrosNucleo2[instruccion[2]];
                            break;
                        case 12:
                            registrosNucleo2[instruccion[3]] = registrosNucleo2[instruccion[1]]*registrosNucleo2[instruccion[2]];
                            break;
                        case 14:
                            registrosNucleo2[instruccion[3]] = registrosNucleo2[instruccion[1]]/registrosNucleo2[instruccion[2]];
                            break;
                        case 4:
                            if(registrosNucleo2[instruccion[1]]==0){
                                PCN2 += (4*instruccion[3]);
                            }
                            break;
                        case 5:
                            if(registrosNucleo2[instruccion[1]]!=0){
                                PCN2 += (4*instruccion[3]);
                            }
                            break;
                        case 3:
                            registrosNucleo2[31] = PCN2;
                            PCN2+=instruccion[3];
                            break;
                        case 2:
                            PCN2 = registrosNucleo2[instruccion[1]];
                            break;
                        case 63:
                            System.out.println("Se termina de ejecutar el hilo");
                            this.miQuantum = QuantumIngresado+1;
                            /// imprimir registros para verificacion
                            //escribirEnArchivo("Hilo "+hiloN2);
                            escribirEnArchivo(verRegistros(1));
                            escribirEnArchivo("Tamano cola "+colaContextos.size());
                            if(hayHilosEnCola()){
                                traerSiguenteHilo(1);
                            }else{
                                estaOcupadoN2 = false;
                                this.estoyOcupado = false;
                            }
                            
                            /// llamar a siguiente proceso en el proximo ciclo de reloj
                            break;
                        default:
                            System.out.println("Fallo al ejecutar instruccion "+instruccion[0]);
                    }
                }
                this.miQuantum--;

            }
           
                
                /**
                 * Escribe en un archivo el string que recibe
                 * @param str
                 * @throws IOException 
                 */
                public void escribirEnArchivo(String str) throws IOException {
                    int nucleo = Integer.parseInt(this.getName());
                    if(nucleo == 0){
                        escribir1.append(str+"\n");//para escribir en el archivo  
                    }
                    else{
                        escribir2.append(str+"\n");//para escribir en el archivo
                    }
                }
                public String verInstruccion(int[] instruccion){
                    String instr ="";
                    for(int i=0 ;i <instruccion.length;++i){
                        instr+=""+instruccion[i]+" ";
                    }
                    instr+="\n";
                    return instr;
                }
                
                public String verRegistros(int nucleo){
                    String str ="";
                    if(nucleo==0){
                        for(int i=0;i<registrosNucleo1.length;++i){
                            str+="r"+i+" "+registrosNucleo1[i]+"\n";
                        }
                    }
                    else{
                        for(int i=0;i<registrosNucleo2.length;++i){
                            str+="r"+i+" "+registrosNucleo2[i]+"\n";
                        }
                    }
                    return str;
                }
                
                public void run(){
                    estoyOcupado = true;
                    String str ="";
                    this.miQuantum = QuantumIngresado;
                    try {
                        escribirEnArchivo("Nucleo "+getName());
                        
                        //toda la logica por aqui
                         int numNucleo = Integer.parseInt(this.getName());
                         int [] instruccion;
                        while(estaOcupadoN1 || estaOcupadoN2){
                            if(estoyOcupado){
                                if (numNucleo == 0){
                                    IRN1 = PCN1;
                                   // System.out.println("PCN1 "+PCN1);
                                    int bloque = PCN1/16;
                                    str+="PC "+PCN1+" Reloj "+clock+"\nBloque "+bloque;
                                    //escribirEnArchivo(str);str="";
                                    PCN1 +=4;
                                    if(!estaEnCache(bloque, numNucleo)){
                                        //escribirEnArchivo("Hay fallo de cache con bloque "+bloque+" PC "+IRN1);
                                        resolverFalloCache(IRN1,bloque,numNucleo); 
                                        //escribirEnArchivo(obtenerCache(0));
                                    }
                                    instruccion = obtenerInstruccion(IRN1, bloque,0);
                                    //escribirEnArchivo("Instruccion "+verInstruccion(instruccion));
                                    ejecutarInstruccion(instruccion,0);
                                    //escribirEnArchivo(str+="Quantum "+this.miQuantum);str="";


                                }else{
                                     IRN2 = PCN2;
                                    // System.out.println("PCN2 "+PCN2);
                                    int bloque = PCN2/16;
                                    str+="PC "+PCN2+" Reloj "+clock+"\nBloque "+bloque;
                                    //escribirEnArchivo(str);str="";
                                    PCN2 +=4;
                                    if(!estaEnCache(bloque, numNucleo)){
                                        //escribirEnArchivo("Hay fallo de cache con bloque "+bloque);
                                        resolverFalloCache(IRN2,bloque,numNucleo); 
                                        //escribirEnArchivo(obtenerCache(1));
                                    }
                                    instruccion = obtenerInstruccion(IRN2, bloque,1);
                                    //escribirEnArchivo("Instruccion "+verInstruccion(instruccion));
                                    ejecutarInstruccion(instruccion,1);
                                    //escribirEnArchivo(str+="Quantum "+this.miQuantum);str="";

                                }
                                if(this.miQuantum == 0){
                                    //escribirEnArchivo(str+="Cambio de contexto "+this.miQuantum);str="";
                                    cambioDeContexto(Integer.parseInt(this.getName()));
                                    this.miQuantum = QuantumIngresado;
                                }
                            }
                            try {
                                barrera();
                                barrera();
                            } catch (InterruptedException | BrokenBarrierException ex) {
                                Logger.getLogger(ProyectoArqui.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        escribir1.close();
                        bw1.close();
                        escribir2.close();
                        bw2.close();
                    } catch (IOException ex) {
                        Logger.getLogger(ProyectoArqui.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
           }.start();
        }
        

        new Thread("Interfaz"){
            Salida m_salida = new Salida();
            public void run(){

                System.out.println("Soy el hilo interfaz");
                m_salida.setMemoriaInstrucciones(memoriaInstrucciones);
                m_salida.validate();
                m_salida.repaint();
                
                while(estaOcupadoN1 || estaOcupadoN2){
                    try {
                        barrera();
                        System.out.println("Actualizando HiloN1 "+hiloN1);
                        System.out.println("Actualizando HiloN2 "+hiloN2);
                        m_salida.setCaches(cacheInstruccionesNucleo1, 0);                    
                        m_salida.setCaches(cacheInstruccionesNucleo2, 1);
                        m_salida.setRegistros(registrosNucleo1, 0);
                        m_salida.setRegistros(registrosNucleo2, 1);
                        m_salida.setReloj(clock);
                        barrera();
                    } catch (InterruptedException | BrokenBarrierException ex) {
                        Logger.getLogger(ProyectoArqui.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    m_salida.validate();
                    m_salida.repaint();
                }

                

            }

        }.start();
        
        
        while(estaOcupadoN1 || estaOcupadoN2){
            try {
                barrera();
                ++clock;
                //Thread.sleep(1000);
            
                //m_salida.refrescar();
                barrera();
            } catch (InterruptedException | BrokenBarrierException ex) {
                Logger.getLogger(ProyectoArqui.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
        }
        //System.out.println("Ya termine");
        
        //System.exit(0);
    }
    
    
}