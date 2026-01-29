package com.evlo.support;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * WebFlux FilePart를 FileUploadService(MultipartFile)에서 사용할 수 있도록
 * byte[] 기반 MultipartFile 구현.
 */
public class InMemoryMultipartFile implements MultipartFile {

    private final byte[] content;
    private final String name;
    private final String originalFilename;
    private final String contentType;

    public InMemoryMultipartFile(byte[] content, String name, String originalFilename, String contentType) {
        this.content = content != null ? content : new byte[0];
        this.name = name;
        this.originalFilename = originalFilename != null ? originalFilename : "";
        this.contentType = contentType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() {
        return content;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(File dest) throws IOException {
        Files.write(dest.toPath(), content);
    }

    @Override
    public void transferTo(Path dest) throws IOException {
        Files.write(dest, content);
    }
}
