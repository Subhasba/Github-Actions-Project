//package com.example.ragexample.service;
//
//import org.springframework.ai.chat.model.ChatModel;
//import org.springframework.ai.chat.prompt.Prompt;
//import org.springframework.ai.document.Document;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//public class QueryService {
//
//    private final MongoDBVectorStore vectorStore;
//    private final ChatModel chatModel;
//
//    public QueryService(MongoDBVectorStore vectorStore, ChatModel chatModel) {
//        this.vectorStore = vectorStore;
//        this.chatModel = chatModel;
//    }
//
//    public String ask(String question) {
//
//        List<Document> docs = vectorStore.similaritySearch(question);
//
//        String context = docs.stream()
//                .map(Document::getContent)
//                .reduce("", (a, b) -> a + "\n" + b);
//
//        String prompt = """
//                Answer based only on the context below:
//                %s
//
//                Question: %s
//                """.formatted(context, question);
//
//        return chatModel.call(new Prompt(prompt))
//                .getResult()
//                .getOutput()
//                .getText();
//    }
//}