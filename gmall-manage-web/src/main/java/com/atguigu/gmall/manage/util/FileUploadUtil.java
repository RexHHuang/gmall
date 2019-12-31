package com.atguigu.gmall.manage.util;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

public class FileUploadUtil {

    private static final String BASE_URL = "http://192.168.126.129/";

    public static String imageUpload(MultipartFile file, FastFileStorageClient fileStorageClient) {
        String originalFilename = file.getOriginalFilename();
        String suffix = StringUtils.substringAfterLast(originalFilename, ".");
        try {
            StorePath storePath = fileStorageClient.uploadFile(file.getInputStream(), file.getSize(), suffix, null);
            return BASE_URL + storePath.getFullPath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
