package br.com.alura.screenmatch.service;

import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.service.OpenAiService;

import java.io.FileInputStream;
import java.io.IOException;

public class ConsultaChatGPT {
    public static String obterTraducao(String texto) {
        String chaveApi = lerChaveApiDoArquivo("apikey.txt");
        OpenAiService servico = new OpenAiService(chaveApi);

        CompletionRequest requisicao = CompletionRequest.builder()
                .model("gpt-3.5-turbo-instruct")
                .prompt("traduza para o português o texto: " + texto)
                .maxTokens(1000)
                .temperature(0.7)
                .build();

        var resposta = servico.createCompletion(requisicao);
        return resposta.getChoices().get(0).getText();
    }


    private static String lerChaveApiDoArquivo(String caminhoArquivo) {
        try (FileInputStream fis = new FileInputStream(caminhoArquivo)) {
            byte[] bytes = new byte[fis.available()];
            fis.read(bytes);
            return new String(bytes).trim();
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler a chave API do arquivo", e);
        }
    }
}