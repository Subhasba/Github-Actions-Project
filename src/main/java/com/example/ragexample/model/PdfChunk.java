package com.example.ragexample.model;

import jakarta.persistence.*;

@Entity
@Table(name = "pdf_chunks")
public class PdfChunk {

    @Id
    private String id;

    @Column(columnDefinition = "TEXT")
    private String text;

    private String fileName;
    private int length;

    private String checkSum;

    // getters & setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public int getLength() { return length; }
    public void setLength(int length) { this.length = length; }
}