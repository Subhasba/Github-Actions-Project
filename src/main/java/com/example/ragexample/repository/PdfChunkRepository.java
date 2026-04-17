package com.example.ragexample.repository;

import com.example.ragexample.model.PdfChunk;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PdfChunkRepository extends JpaRepository<PdfChunk, String> {
}