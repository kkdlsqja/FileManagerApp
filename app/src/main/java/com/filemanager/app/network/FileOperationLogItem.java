package com.filemanager.app.network;

public class FileOperationLogItem {

    private Long id;
    private String fileName;
    private String category;
    private String status;
    private String sourcePath;
    private String destinationPath;
    private String detail;
    private long occurredAt;

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getCategory() {
        return category;
    }

    public String getStatus() {
        return status;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public String getDetail() {
        return detail;
    }

    public long getOccurredAt() {
        return occurredAt;
    }
}
