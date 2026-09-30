package com.filemanager.app.network;

public class RemoteFile {

    private String fileName;
    private boolean isDirectory;
    private long fileSize;
    private long lastModified;
    private String path;

    public String getFileName() {
        return fileName;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public long getFileSize() {
        return fileSize;
    }

    public long getLastModified() {
        return lastModified;
    }

    public String getPath() {
        return path;
    }
}
