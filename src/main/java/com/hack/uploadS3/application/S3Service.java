package com.hack.uploadS3.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class S3Service {

    private final S3AsyncClient s3AsyncClient;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3Service(S3AsyncClient s3AsyncClient) {
        this.s3AsyncClient = s3AsyncClient;
    }

    public String uploadFile(MultipartFile file, String id, String email) throws IOException, ExecutionException, InterruptedException {
        // Diretório é o id recebido 
        String fileName =  id + "/" + file.getOriginalFilename();

        // Usar UUID para garantir nome único no arquivo temporário
        String tempFileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
        Path tempFile = Files.createTempFile(null, tempFileName);

        try {
            // Copiar o conteúdo do MultipartFile para o arquivo temporário
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Arquivo temporário criado: " + tempFile);

            // Criando um Map para armazenar os metadados
            Map<String, String> metadata = new HashMap<>();
            metadata.put("id", id); // Adiciona o id como metadado
            metadata.put("email", email); // Adiciona o email como metadado

            // Cria o PutObjectRequest com a chave do arquivo, tipo de conteúdo e metadados
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .metadata(metadata) // Passando o Map de metadados
                    .build();

            // Usando AsyncRequestBody para criar o corpo assíncrono a partir do arquivo temporário
            AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromFile(tempFile);
            System.out.println("Iniciando upload para S3...");

            // Realiza o upload do arquivo
            s3AsyncClient.putObject(putObjectRequest, asyncRequestBody).get();
            System.out.println("Upload concluído com sucesso!");

            return "https://" + bucketName + ".s3.amazonaws.com/" + fileName;
        } catch (IOException e) {
            System.err.println("Erro durante o envio do arquivo (IOException): " + e.getMessage());
            e.printStackTrace();  // Exibe a pilha de chamadas
            throw e;
        } catch (InterruptedException e) {
            System.err.println("Erro durante o envio do arquivo (InterruptedException): " + e.getMessage());
            e.printStackTrace();  // Exibe a pilha de chamadas
            Thread.currentThread().interrupt();  // Restaura o status de interrupção
            throw e;
        } catch (ExecutionException e) {
            System.err.println("Erro durante o envio do arquivo (ExecutionException): " + e.getMessage());
            e.printStackTrace();  // Exibe a pilha de chamadas
            throw e;
        } finally {
            // Exclui o arquivo temporário após o upload
            try {
                Files.deleteIfExists(tempFile);
                System.out.println("Arquivo temporário excluído: " + tempFile);
            } catch (IOException e) {
                System.err.println("Erro ao excluir arquivo temporário: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // Método para criar um S3AsyncClient com o cliente HTTP padrão
    public static S3AsyncClient createS3AsyncClient() {
        return S3AsyncClient.builder()
                .region(Region.US_EAST_1) // Defina a região conforme necessário
                .build();
    }
}
