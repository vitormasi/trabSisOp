package trabsisop;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExecutaMemoria {

    //prioridade de vitima armazena a página
    private List<Integer> prioridadeVitimaFifo = new ArrayList<>();
    private List<Integer> prioridadeVitimaLru = new ArrayList<>();
    private List<List<Integer>> referenciaPorPagina = new ArrayList<>(); //composto por uma lista com 0 pagina, 1 bit referencia
    
    private List<List<String>> memoriaFisica = new ArrayList<>();
    private List<Integer> tabelaPaginas = new ArrayList<>();
    
    private int metodoSubstituicao;
    private int contFalhas = 0;
    private int posicaoSegundaChance = 0;
    private int tamanhoPagina, tamanhoMemoriaFisica;
    private Boolean memoriaFisicaCheia = false;
    private String comandoEmExecucao;
    
    //realizar toda execução do metodo solicitado
    public void exec(String method, String request, String newValue) throws IOException {
        comandoEmExecucao = "[" + method + " " + request + (newValue != null ? ", " + newValue : "") + "] - ";
        int pagina = Integer.parseInt(request) / tamanhoPagina;
        int posicaoNaPagina = Integer.parseInt(request) % tamanhoPagina;
        
        //metodo de gravação
        if("s".equals(method)) {
            storeDisco(pagina, posicaoNaPagina, newValue);
        }
        
        //metodo load que também executa quando é gravação
        if(tabelaPaginas.get(pagina) != null) {
            //caso exista na memória virtual retorna (informa no log) e atualiza a próxima vítima
            addLog(comandoEmExecucao + "Registro na memória | retorno: " + memoriaFisica.get(tabelaPaginas.get(pagina)).get(posicaoNaPagina));
            atualizaMetodoSubstituicao(true, pagina);
        } else {
            //caso nao exista na memória virtual, realiza a substituição e atualiza a próxima vítima
            int vitima = 0;
            contFalhas++;
            if(memoriaFisicaCheia) {
                //se estiver cheia terá que alterar
                vitima = getProximaVitima(); //index da memória física que será substituido
                memoriaFisica.set(vitima, acessaPaginaDisco(pagina));
                tabelaPaginas.set(tabelaPaginas.indexOf(vitima), null);
                tabelaPaginas.set(pagina, vitima);
            } else {
                //se memória ainda não está cheia, deve preencher a próxima entrada vazia
                for(int i=0; i < tamanhoMemoriaFisica ; i++) {
                    if(memoriaFisica.get(i) == null) {
                        vitima = i;
                        memoriaFisica.set(vitima, acessaPaginaDisco(pagina));
                        tabelaPaginas.set(pagina, vitima);
                        if(i+1 == tamanhoMemoriaFisica){
                            memoriaFisicaCheia = true;
                        }
                        break;
                    }
                }
            }
            //informa log que ouve altração na memória e informa o retorno solicitado
            addLog(comandoEmExecucao + "FALHA substituição de registro na memória: " + vitima + " -> página " + pagina + " | retorno: " + memoriaFisica.get(tabelaPaginas.get(pagina)).get(posicaoNaPagina));
            atualizaMetodoSubstituicao(false, pagina);
        }
    }
    
    //metodo para iniciar disco, tabelaPaginas e arrays de substituição com valores default
    public void init() throws IOException {
        for(int i=0; i < tamanhoMemoriaFisica; i++){
            memoriaFisica.add(null);
            prioridadeVitimaFifo.add(null);
            prioridadeVitimaLru.add(null);
            referenciaPorPagina.add(null);
        }
        
        String posicaoPaginaInit = "";
        for(int i=0; i < tamanhoPagina; i++){
            tabelaPaginas.add(null);
            posicaoPaginaInit = posicaoPaginaInit.concat(i + "|");
        }
        posicaoPaginaInit = posicaoPaginaInit.substring(0, posicaoPaginaInit.length()-1);
        String discoInit = "";
        for(int i=0; i< tamanhoPagina; i++){
            discoInit = discoInit.concat(posicaoPaginaInit + "\n");
        }
        discoInit = discoInit.substring(0, discoInit.length()-1);
        
        Path path = Paths.get("disco.txt");
        Files.write(path, discoInit.getBytes());
        
        //zera a variável log caso esteja utilizada de outra execução
        path = Paths.get("log.txt");
        Files.write(path, "".getBytes());
        
    }
    
    //apenas para setar o método de substituição escolhido
    public void setMetodoSubstituicao(int metodo) {
        metodoSubstituicao = metodo;
    }
    
    public void setTamanhoPagina(int tamanhoPagina) {
        this.tamanhoPagina = tamanhoPagina;
    }
    
    public void setTamanhoMemoriaFisica(int tamanhoMemoriaFisica) {
        this.tamanhoMemoriaFisica = tamanhoMemoriaFisica;
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
    private int getProximaVitima() {
        switch (metodoSubstituicao) {
            case 1: //FIFO
		return tabelaPaginas.get(prioridadeVitimaFifo.get(0));
            case 2: //LRU
		return tabelaPaginas.get(prioridadeVitimaLru.get(0));
            case 3: //SEGUNDA CHANCE
		while(true) {
                    //caso a posição da segunda chance esteja com bit referencia 1, zera e passa ao próximo
                    if(1 == referenciaPorPagina.get(posicaoSegundaChance).get(1)) {
                        referenciaPorPagina.set(posicaoSegundaChance, Arrays.asList(referenciaPorPagina.get(posicaoSegundaChance).get(0), 0));
                        addPosicaoSegundaChance();
                    } else {
                        return tabelaPaginas.get(referenciaPorPagina.get(posicaoSegundaChance).get(0));
                    }
                }
	}
        return 0;
    }
    
    private void atualizaMetodoSubstituicao(Boolean estaNaMemoriaVirtual, int pagina) {
        if(estaNaMemoriaVirtual){
            //LRU: como foi utilizado, é removido da posição que estava e passa ao final da fila
            prioridadeVitimaLru.remove(prioridadeVitimaLru.indexOf(pagina));
            prioridadeVitimaLru.add(pagina);
            
            //SEGUNDA CHANCE: rearmazena o próprio valor com bit de referencia 1
            referenciaPorPagina.set(getIndexPaginaArrayReferenciaPorPagina(pagina), Arrays.asList(pagina, 1));
            
        } else {
            //remove a primeira posição e add a nova página na última posição
            prioridadeVitimaFifo.remove(0);
            prioridadeVitimaFifo.add(pagina);
            
            //remove a primeira posição e add o nova página na última posição
            prioridadeVitimaLru.remove(0);
            prioridadeVitimaLru.add(pagina);
           
            //remove a página que estava na posição da segunda chance e adiciona o novo registro da página com bit referencia 1
            referenciaPorPagina.set(posicaoSegundaChance, Arrays.asList(pagina, 1));
            addPosicaoSegundaChance();
        }
    }
    
    private int getIndexPaginaArrayReferenciaPorPagina(int pagina) {
        for(List<Integer> posicaoSegChance : referenciaPorPagina) {
            if(posicaoSegChance.get(0) == pagina) {
                return referenciaPorPagina.indexOf(posicaoSegChance);
            }
        }
        return 0;
    }
    
    //metodo auxiliar para que a próxima posição retorne a zero quando +1 no final
    private void addPosicaoSegundaChance() {
        posicaoSegundaChance++;
        if (posicaoSegundaChance >= memoriaFisica.size()) {
            posicaoSegundaChance = 0;
        }
    }
    
    //get para a classe main buscar
    public int getContFalhas(){
        return contFalhas;
    }
    
    private List<List<String>> acessaDiscoInteiro() throws IOException{
        List<List<String>> retorno = new ArrayList<>();
        Path path = Paths.get("disco.txt");
        
        List<String> linhas = Files.readAllLines(path);
        for (String linha : linhas) {
            String[] split = linha.split("\\|");
            retorno.add(Arrays.asList(split));
        }
        
        return retorno;
    }
    
    private List<String> acessaPaginaDisco(int pagina) throws IOException {
        return acessaDiscoInteiro().get(pagina);
    }
    
    private void storeDisco(int pagina, int posicaoPagina, String newValue) throws IOException {
        addLog(comandoEmExecucao + "Alterando o disco na página " + pagina + " posição " + posicaoPagina + " para o valor -> " + newValue);
        
        List<List<String>> disco = acessaDiscoInteiro();
        disco.get(pagina).set(posicaoPagina, newValue);
        
        String discoString = "";
        for (List<String> linha : disco) {
            String linhaString = "";
            for (String posicao : linha) {
                linhaString = linhaString.concat(posicao + "|");
            }
            linhaString = linhaString.substring(0, linhaString.length()-1);
            discoString = discoString + linhaString + "\n";
        }
        discoString = discoString.substring(0, discoString.length()-1);
        
        Path path = Paths.get("disco.txt");
        Files.write(path, discoString.getBytes());
    }
    
}
