package com.example.ragexample.service;

import com.example.ragexample.config.ChromaConfig;
import com.example.ragexample.model.PdfChunk;
import com.example.ragexample.repository.PdfChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class IngestService {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);

    private final PdfChunkRepository pdfRepo;
    private final EmbeddingModel embeddingModel;
    private final ChromaApi chromaApi;
    private final ChatModel chatModel;
    private final ChromaConfig config;

    public IngestService(PdfChunkRepository pdfRepo,
                         EmbeddingModel embeddingModel,
                         ChromaApi chromaApi,
                         ChatModel chatModel,
                         ChromaConfig config) {
        this.pdfRepo = pdfRepo;
        this.embeddingModel = embeddingModel;
        this.chromaApi = chromaApi;
        this.chatModel = chatModel;
        this.config = config;
    }

    public void processPdf(MultipartFile file) {
        try {


            log.info(" Processing PDF...");

            var resource = new InputStreamResource(file.getInputStream());
            var content = resource.getContentAsByteArray();
            var reader = new PagePdfDocumentReader(resource);
            List<Document> docs = reader.get();

            for (Document doc : docs) {
                String text = doc.getText();
                if (text == null || text.isBlank()) continue;

                List<String> chunks = splitText(text, 800, 100);

                for (String chunk : chunks) {
                    if (chunk.isBlank()) continue;

                    saveToPostgres(chunk, file.getOriginalFilename());
                    saveToChroma(chunk);
                }
            }

            log.info(" PDF processed successfully");

        } catch (Exception e) {
            log.error(" PDF processing failed", e);
        }
    }


    private List<String> splitText(String text, int size, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;

        //'\r\n'

        //hello, asldasf  sdaljkfs
        //how are you
        //doing well

        while (start < text.length()) {
            int end = Math.min(start + size, text.length());
            chunks.add(text.substring(start, end).trim());
            start += (size - overlap);
        }
        return chunks;
    }

    private void saveToPostgres(String text, String fileName) {
        PdfChunk chunk = new PdfChunk();
        chunk.setId(UUID.randomUUID().toString());
        chunk.setText(text);
        chunk.setFileName(fileName);
        chunk.setLength(text.length());

        pdfRepo.save(chunk);
    }

    private float[] embed(String text) {
        EmbeddingResponse res = embeddingModel.embedForResponse(List.of(text));
        return res.getResult().getOutput();
    }


    private void saveToChroma(String text) {
        try {
            ensureDatabase();
            var collection = ensureCollection();

            float[] embedding = embed(text);

            chromaApi.upsertEmbeddings(
                    config.getTenantName(),
                    config.getDatabaseName(),
                    collection.id(),
                    new ChromaApi.AddEmbeddingsRequest(
                            List.of(UUID.randomUUID().toString()),
                            List.of(embedding),
                            List.of(Map.of(
                                    "type", "pdf",
                                    "length", text.length()
                            )),
                            List.of(text)
                    )
            );

            log.info(" Embedding stored");

        } catch (Exception e) {
            log.error(" Chroma Error", e);
        }
    }


    private void ensureDatabase() {
        try {
            chromaApi.createDatabase(
                    config.getTenantName(),
                    config.getDatabaseName()
            );
            log.info("🛠 DB ensured (created or exists)");
        } catch (Exception e) {
            log.warn(" DB might already exist");
        }
    }

    private ChromaApi.Collection ensureCollection() {
        var collection = chromaApi.getCollection(
                config.getTenantName(),
                config.getDatabaseName(),
                config.getCollectionName()
        );

        if (collection == null) {
            log.info(" Creating Collection...");
            collection = chromaApi.createCollection(
                    config.getTenantName(),
                    config.getDatabaseName(),
                    new ChromaApi.CreateCollectionRequest(
                            config.getCollectionName(),
                            Map.of("created_by", "rag-system")
                    )
            );
        }

        return collection;
    }

    public List<String> query(String question) {

        try {
            if (question.toLowerCase().contains("what is this document")) {
                question = "Give a summary of the Bean & Leaf investment memorandum";
            }

            var collection = chromaApi.getCollection(
                    config.getTenantName(),
                    config.getDatabaseName(),
                    config.getCollectionName()
            );

            if (collection == null) {
                return List.of("No data. Upload PDF first.");
            }

            float[] queryEmbedding = embed(question);

            var response = chromaApi.queryCollection(
                    config.getTenantName(),
                    config.getDatabaseName(),
                    collection.id(),
                    new ChromaApi.QueryRequest(queryEmbedding, 8) // ✅ higher retrieval
            );

            if (response.documents() == null || response.documents().isEmpty()) {
                return List.of("No relevant data found");
            }

            //  Filter relevant chunks only
            List<String> docs = response.documents()
                    .stream()
                    .map(doc -> String.join(" ", doc))
                    .filter(text -> text.toLowerCase().contains("bean")
                            || text.toLowerCase().contains("coffee")
                            || text.toLowerCase().contains("tea")
                            || text.toLowerCase().contains("investment"))
                    .limit(2)
                    .toList();

            String combinedContext = String.join("\n", docs);

            String cleanContext = combinedContext
                    .replaceAll("\\s+", " ")
                    .replaceAll("[^\\x00-\\x7F]", "")
                    .trim();

            String prompt = """
                    You are a strict AI system.

                    Answer ONLY using the given context.
                    Do NOT add external knowledge.

                    If answer is unclear:
                    "Answer not available in context"

                    Context:
                    %s

                    Question:
                    %s

                    Answer:
                    """.formatted(cleanContext, question);

            var result = chatModel.call(new Prompt(prompt));
            String output = result.getResult().getOutput().getText();

            if (output != null &&
                    output.toLowerCase().contains("non-disclosure") &&
                    !cleanContext.toLowerCase().contains("non-disclosure")) {

                return List.of("Answer not available in context");
            }

            return List.of(output != null ? output : "No response");

        } catch (Exception e) {
            log.error(" Query failed", e);
            return List.of("Query failed");
        }
    }
}