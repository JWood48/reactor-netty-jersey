package com.stony.reactor.jersey;

/**
 * <p>reactor-netty-ext
 * <p>com.stony.reactor.jersey
 *
 * @author stony
 * @version 上午10:06
 * @since 2018/3/1
 */
public class MimeType {
    String contentType;
    String suffix;
    private String fileName;
    private String filePath;

    public MimeType(String contentType, String suffix) {
        this.contentType = contentType;
        this.suffix = suffix;
    }

    public String getContentType() {
        return contentType;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public String toString() {
        return "MimeType{" +
                "contentType='" + contentType + '\'' +
                ", suffix='" + suffix + '\'' +
                ", fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                '}';
    }



}
