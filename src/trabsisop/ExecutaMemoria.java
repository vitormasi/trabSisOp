/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trabsisop;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author vitor
 */

public class ExecutaMemoria {

    private List<String> prioridadeVitimaFifo = new ArrayList<>();
    private List<String> prioridadeVitimaLru = new ArrayList<>();
    private List<List<String>> referenciaPorPagina = new ArrayList<>(); //composto por uma lista com 0 pagina, 1 bit referencia
    private List<String> memoriaVirtual = new ArrayList<>();
    private List<String> memoriaPrincipal = new ArrayList<>();
    private int metodoSubstituicao;
    private int contFalhas = 0;
    private int posicaoSegundaChance = 0;
    
    //realizar toda execução do metodo solicitado
    public void exec(String method, String request, String newValue) throws IOException {
        String comandoEmExecucao = "[" + method + " " + request + (newValue != null ? ", " + newValue : "") + "] - ";
        
        //metodo de gravação
        if("s".equals(method)) {
            addLog(comandoEmExecucao + "Registro gravado na memória pricipal: " + memoriaPrincipal.get(Integer.parseInt(request)) + " -> " + newValue);
            memoriaPrincipal.set(Integer.parseInt(request), newValue);
            atualizaArquivoMemoriaPrincipal();
        }
        
        //metodo load que também executa quando é gravação
        if(memoriaVirtual.contains(request)) {
            //caso exista na memória virtual retorna e atualiza a próxima vítima
            addLog(comandoEmExecucao + "Registro na memória virtual | retorno: " + memoriaPrincipal.get(Integer.parseInt(request)));
            atualizaMetodoSubstituicao(true, request);
        } else {
            //caso nao exista na memória virtual, realiza a substituição e atualiza a próxima vítima
            contFalhas++;
            memoriaVirtual.set(memoriaVirtual.indexOf(getProximaVitima()), request);
            atualizaArquivoMemoriaVirtual();
            addLog(comandoEmExecucao + "FALHA substituição de registro na memória virtual: " + getProximaVitima() + " -> " + request + " | retorno: " + request);
            atualizaMetodoSubstituicao(false, request);
        }
    }
    
    //metodo para iniciar as memórias com valores default
    public void init(int tamanhoMemoriaPrincipal, int tamanhoMemoriaVirtual) throws IOException {
        for(int i = 0; i < tamanhoMemoriaPrincipal; i++) {
            memoriaPrincipal.add(String.valueOf(i));
            if(i < tamanhoMemoriaVirtual) {
                memoriaVirtual.add(String.valueOf(i));
                prioridadeVitimaFifo.add(String.valueOf(i));
                prioridadeVitimaLru.add(String.valueOf(i));
                referenciaPorPagina.add(Arrays.asList(String.valueOf(i), "1"));
            }
        }
        atualizaArquivoMemoriaPrincipal();
        atualizaArquivoMemoriaVirtual();
    }
    
    //apenas para setar o método de substituição escolhido
    public void setMetodoSubstituicao(int metodo) {
        metodoSubstituicao = metodo;
    }
    
    //grava o log no arquivo de log
    public void addLog(String logText) throws IOException {
        Path path = Paths.get("log.txt");
        byte[] bytesArquivo = Files.readAllBytes(path);
        String textoArquivo = new String(bytesArquivo);
        textoArquivo = textoArquivo.concat(logText + "\n");
        Files.write(path, textoArquivo.getBytes());
    }
    
    //seleciona quem deve ser alterado na memória virtual de acordo com o método de substituição
    private String getProximaVitima() {
        switch (metodoSubstituicao) {
            case 1: //FIFO
		return prioridadeVitimaFifo.get(0);
            case 2: //LRU
		return prioridadeVitimaLru.get(0);
            case 3: //SEGUNDA CHANCE
		while(true) {
                    //caso a posição da segunda chance esteja com bit referencia 1, zera e passa ao próximo
                    if("1".equals(referenciaPorPagina.get(posicaoSegundaChance).get(1))) {
                        referenciaPorPagina.set(posicaoSegundaChance, Arrays.asList(referenciaPorPagina.get(posicaoSegundaChance).get(0), "0"));
                        addPosicaoSegundaChance();
                    } else {
                        return referenciaPorPagina.get(posicaoSegundaChance).get(0);
                    }
                }
	}
        return "";
    }
    
    private void atualizaMetodoSubstituicao(Boolean estaNaMemoriaVirtual, String request) {
        if(estaNaMemoriaVirtual){
            //LRU: como foi utilizado, é removido da posição que estava e passa ao final da fila
            prioridadeVitimaLru.remove(prioridadeVitimaLru.indexOf(request));
            prioridadeVitimaLru.add(request);
            
            //SEGUNDA CHANCE: rearmazena o próprio valor com bit de referencia 1
            referenciaPorPagina.set(posicaoSegundaChance, Arrays.asList(referenciaPorPagina.get(posicaoSegundaChance).get(0), "1"));
            addPosicaoSegundaChance();
            
        } else {
            //remove a primeira posição e add o novo registro da memória principal na última posição
            prioridadeVitimaFifo.remove(0);
            prioridadeVitimaFifo.add(request);
            
            //remove a primeira posição e add o novo registro da memória principal na última posição
            prioridadeVitimaLru.remove(0);
            prioridadeVitimaLru.add(request);
           
            //remove a página que estava na posição da segunda chance e adiciona o novo registro da memória principal com bit referencia 1
            referenciaPorPagina.set(posicaoSegundaChance, Arrays.asList(request, "1"));
            addPosicaoSegundaChance();
        }
    }
    
    //metodo auxiliar para que a próxima posição retorne a zero quando +1 no final
    private void addPosicaoSegundaChance() {
        posicaoSegundaChance++;
        if (posicaoSegundaChance >= memoriaVirtual.size()) {
            posicaoSegundaChance = 0;
        }
    }
    
    //get para a classe main buscar
    public int getContFalhas(){
        return contFalhas;
    }
    
    //chamado sempre que há alteração na memória virtual, armazena no arquivo
    private void atualizaArquivoMemoriaVirtual() throws IOException {
        String texto = "";
        for(String registro : memoriaVirtual) {
            texto = texto.concat(registro + "\n");
        }
        Path path = Paths.get("memoriaVirtual.txt");
        Files.write(path, texto.getBytes());
    }
      
    //chamado sempre que há alteração na memória principal, armazena no arquivo
    private void atualizaArquivoMemoriaPrincipal() throws IOException {
        String texto = "";
        for(String registro : memoriaPrincipal) {
            texto = texto.concat(registro + "\n");
        }
        Path path = Paths.get("memoriaPrincipal.txt");
        Files.write(path, texto.getBytes());
    }
    
}
