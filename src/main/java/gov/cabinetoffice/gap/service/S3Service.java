package gov.cabinetoffice.gap.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;

import java.time.Instant;
import java.util.Date;

public class S3Service {

    private static final String ATTACHMENTS_BUCKET = System.getenv("SUBMISSION_EXPORTS_BUCKET_NAME");

    public static final int LINK_TIMEOUT_DURATION = Integer.parseInt(System.getenv("SIGNED_URL_TIMEOUT_SECONDS"));

    public static String generateExportDocSignedUrl(AmazonS3 client, String objectKey) {

        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(ATTACHMENTS_BUCKET,
                objectKey).withMethod(HttpMethod.GET)
                        .withExpiration(Date.from(Instant.now().plusSeconds(LINK_TIMEOUT_DURATION)));

        return client.generatePresignedUrl(generatePresignedUrlRequest).toExternalForm();
    }

}
