package org.oreo.smore.global.common;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class CloudStorageManager {

    private final BlobContainerClient blobContainerClient;

    /**
     * 프로필 이미지 업로드 (파일명: user/{userId}.{확장자}, 덮어쓰기 가능)
     */
    public String uploadProfileImage(MultipartFile file, Long userId) throws Exception {
        String extension = getExtension(file.getOriginalFilename());
        String blobName = "user/" + userId + extension;

        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);

        try (InputStream inputStream = file.getInputStream()) {
            blobClient.upload(inputStream, file.getSize(), true);

            BlobHttpHeaders headers = new BlobHttpHeaders()
                    .setContentType(file.getContentType())
                    .setContentDisposition("inline");
            blobClient.setHttpHeaders(headers);
        }

        // 캐시 무효화를 위해 타임스탬프 쿼리 파라미터 추가
        return blobClient.getBlobUrl() + "?t=" + System.currentTimeMillis();
    }

    /**
     * 프로필 이미지 삭제 (user/{userId}.* 정확히 매칭되는 파일만 삭제)
     */
    public void deleteProfileImage(Long userId) {
        String prefix = "user/" + userId + ".";
        blobContainerClient.listBlobs()
                .stream()
                .filter(blobItem -> blobItem.getName().startsWith(prefix) &&
                        blobItem.getName().substring(prefix.length()).contains(".") == false) // 확장자 뒤에 다른 문자가 없게
                .findFirst()
                .ifPresent(blobItem -> blobContainerClient.getBlobClient(blobItem.getName()).delete());
    }

    public String uploadRoomImage(MultipartFile file, Long roomId) throws Exception {
        String extension = getExtension(file.getOriginalFilename());
        String blobName = "room/" + roomId + extension;

        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);

        try (InputStream inputStream = file.getInputStream()) {
            blobClient.upload(inputStream, file.getSize(), true);

            BlobHttpHeaders headers = new BlobHttpHeaders()
                    .setContentType(file.getContentType())
                    .setContentDisposition("inline");
            blobClient.setHttpHeaders(headers);
        }

        // 캐시 무효화를 위해 타임스탬프 쿼리 파라미터 추가
        return blobClient.getBlobUrl() + "?t=" + System.currentTimeMillis();
    }

    public void deleteRoomImage(Long roomId) {
        String prefix = "room/" + roomId + ".";
        blobContainerClient.listBlobs()
                .stream()
                .filter(blobItem -> blobItem.getName().startsWith(prefix) &&
                        !blobItem.getName().substring(prefix.length()).contains(".")) // 확장자 뒤에 다른 문자가 없게
                .findFirst()
                .ifPresent(blobItem -> blobContainerClient.getBlobClient(blobItem.getName()).delete());
    }

    /**
     * 파일 확장자 추출 (.jpg, .png 등)
     */
    private String getExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return "";
    }
}
