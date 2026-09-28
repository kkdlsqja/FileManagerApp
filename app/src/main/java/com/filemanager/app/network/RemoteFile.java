package com.filemanager.app.network;

public class RemoteFile {
    private String fileName;    // 파일 또는 폴더 이름
    private boolean isDirectory; // 폴더인지 파일인지 구분 (true면 폴더)
    private long fileSize;      // 파일 크기

    // Getter
    public String getFileName() {
        return fileName;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public long getFileSize() {
        return fileSize;
    }
}