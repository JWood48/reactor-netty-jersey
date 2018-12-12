package com.stony.reactor.jersey;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>reactor-netty-ext
 * <p>com.stony.reactor.jersey
 *
 * @author stony
 * @version 上午10:49
 * @since 2018/3/1
 */
public class MimeTypeUtil {

    static Map<String, MimeType> SUFFIX_MAP;
    static List<MimeType> MIME_TYPES;
    private MimeTypeUtil() {
        BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        this.getClass().getResourceAsStream("/mime.types"), StandardCharsets.UTF_8));
        List<MimeType> types = null;
        try {
            types = br.lines()
                    .filter(s -> !s.startsWith("#"))
                    .map(s -> s.replace(";", ""))
                    .map(s -> {
                        String[] a = s.split(" ");
                        return a;
                    })
                    .filter(a -> a.length >= 2)
                    .collect(
                            () -> new ArrayList<MimeType>(128),
                            (mimeTypes, strings) -> {
                                if(strings.length == 2) {
                                    mimeTypes.add(new MimeType(strings[0],strings[1]));
                                } else {
                                    String content = strings[0];
                                    for (int i = 1, len = strings.length; i < len; i++) {
                                        mimeTypes.add(new MimeType(content, strings[i]));
                                    }
                                }
                            }, ArrayList::addAll);
            if(types == null || types.isEmpty()) {
                return;
            }
            MIME_TYPES = types;
            SUFFIX_MAP = types.stream().collect(Collectors.toMap(MimeType::getSuffix, mimeType -> mimeType));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public static String getFilePath(String path) {
        if (isEmpty(path)) {
            return null;
        }
        if (path.charAt(0) == '/') {
            path = path.substring(1);
        }
        if (path.charAt(path.length() - 1) == '/') {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
    public static String getFileName(String path) {
        if (isEmpty(path)) {
            return null;
        }
        if (path.charAt(path.length() - 1) == '/') {
            path = path.substring(0, path.length() - 1);
        }
        int index = path.lastIndexOf("/");
        if (index == -1) {
            return path;
        }
        String name = path.substring(index+1);
        if (isEmpty(name)) {
            return null;
        }
        return name;
    }
    public static String getFileSuffix(String fileName) {
        int index = fileName.lastIndexOf(".");
        if (index == -1) {
            return null;
        }
        return fileName.substring(index + 1);
    }
    public static MimeTypeUtil getInstance() {
        return MimeTypeUtilHolder.INSTANCE;
    }
    public static List<MimeType> getMimeTypes() {
        return MIME_TYPES;
    }
    public MimeType getMimeTypeByUri(String uri) {
        try{
            return getMimeTypeByPath(URI.create(uri).getPath());
        } catch (Exception e){
            System.out.println("getMimeTypeByUri [" + uri + "] : " + e.getMessage());
            return null;
        }
    }

    public MimeType getMimeTypeByPath(String path) {
        try{
            return doGetMimeTypeByPath(path);
        } catch (Exception e){
            System.out.println("getMimeTypeByPath [" + path + "] : " + e.getMessage());
            return null;
        }
    }

    private MimeType doGetMimeTypeByPath(String path) {
        String filePath = getFilePath(path);
        String fileName = getFileName(filePath);
        if (isEmpty(fileName)) {
            return null;
        }
        String suffix = getFileSuffix(fileName);
        if(suffix == null) {
            return null;
        }
        MimeType type = getMimeTypeBySuffix(suffix);
        if(type == null) {
            return null;
        }
        type.setFileName(fileName);
        type.setFilePath(filePath);
        return type;
    }


    public MimeType getMimeTypeBySuffix(String suffix) {
        if(SUFFIX_MAP == null) {
            return null;
        }
        return SUFFIX_MAP.get(suffix);
    }
    static abstract class MimeTypeUtilHolder {
        static MimeTypeUtil INSTANCE = new MimeTypeUtil();
    }
    static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
}