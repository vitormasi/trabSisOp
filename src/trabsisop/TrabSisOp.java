package trabsisop;
import java.util.List;
import java.util.Scanner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class TrabSisOp {
	
	static final String CAMINHO_ARQUIVO = "PA.txt";

	public static void main(String[] args) throws IOException {         
            
            Scanner sc = new Scanner(System.in);
	    int Tam_memoria; // variavel para o tamanho da memoria física
	    int Tam_paginas;// variavel para o tamanho das páginas
	    int escolha;
	    //usuário digita qual será o tamanho da memória física e o tamanho das páginas
            System.out.println("Digite o tamanho da memória física: ");
            Tam_memoria = sc.nextInt();
            System.out.println("Digite o tamanho das páginas: ");
            Tam_paginas = sc.nextInt();
    
            ExecutaMemoria executaMemoria = new ExecutaMemoria();
            executaMemoria.setTamanhoPagina(Tam_paginas);
            executaMemoria.setTamanhoMemoriaFisica(Tam_memoria);
            executaMemoria.init();
                
            //mantem o programa em execução até que encerre
            Boolean encerra = false;
            while(!encerra) {
                System.out.println("Escolha: 0 - Encerrar | 1 - Importar arquivo");
                escolha = sc.nextInt();
                switch(escolha){
                    case 0:
                        //informa no log que foi encerrado e o número de falhas que ocorreram durante a execução
                        encerra = true;
                        executaMemoria.addLog("Programa encerrado com " + executaMemoria.getContFalhas() + " falhas.");
                        break;
                    case 1:
                        //usuario vai escolher qual algoritmo de substituição, por exemplo FIFO, LRU, Segunda Chance
                        System.out.println("Escolha a opção que deseja implementar: ");
                        System.out.println("1-FIFO | 2-LRU | 3-Segunda Chance");
                        int escolhaMetodo = sc.nextInt();
                        String escolhaMetodoText;
                        if(escolhaMetodo == 1){
                            escolhaMetodoText = "FIFO";
                        } else if (escolhaMetodo == 2){
                            escolhaMetodoText = "LRU";
                        } else {
                            escolhaMetodoText = "Segunda Chance";
                        }
                        executaMemoria.setMetodoSubstituicao(escolhaMetodo);
                        
                        System.out.println("Informe o caminho do arquivo:");
                        String caminho = sc.next();
                        Path path = Paths.get(caminho);
                        List<String> linhasArquivo = Files.readAllLines(path);
                        String id = "";
                        for (String l : linhasArquivo) {
                            
                            //le o id do arquivo
                            if (linhasArquivo.indexOf(l) == 0){
                                String[] split = l.split(";");
                                id = split[0].trim();
                                
                            //le o tamanho do processo e informa o log
                            } else if (linhasArquivo.indexOf(l) == 1) {
                                String[] split = l.split(";");
                                String tamanhoProcesso = split[0].trim();
                                executaMemoria.addLog("Executando arquivo de id " + id + " com tamanho de processo " + tamanhoProcesso + " com método de substituição " + escolhaMetodoText);
                            } else {
                                
                                //faz a chamada dos métodos de execução
                                String[] split = l.split(";");
                                if('s' == split[0].charAt(0)){
                                    String store = split[0].substring(2);
                                    String[] splitStore = store.split(",");
                                    executaMemoria.exec("s", splitStore[1].trim(), splitStore[0].trim());
                                } else {
                                    String[] splitStore = split[0].split(" ");
                                    executaMemoria.exec("l", splitStore[1].trim(), null);
                                }
                            }
                        }  
                        break;
                    default:
                        System.out.println("Escolha inválida!");
                        break;
                }
            }

	}
}