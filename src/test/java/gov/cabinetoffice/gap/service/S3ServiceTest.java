package gov.cabinetoffice.gap.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.net.URL;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class S3ServiceTest {

    private static AmazonS3 client;

    ArgumentCaptor<GeneratePresignedUrlRequest> presignedUrlRequestCaptor = ArgumentCaptor
            .forClass(GeneratePresignedUrlRequest.class);

    @BeforeAll
    static void beforeAll() {
        client = mock(AmazonS3.class);
    }

    @BeforeEach
    void resetMocks() {
        reset(client);
    }

    @Test
    void successfullyGenerateExportSignedURL() throws Exception {

        URL mockUrl = new URL("https://mock_url.co.uk/object_path");

        when(client.generatePresignedUrl(any())).thenReturn(mockUrl);

        Instant currentInstant = Instant.now();
        Date mockExpiryDate = Date.from(currentInstant.plusSeconds(S3Service.LINK_TIMEOUT_DURATION));

        try (MockedStatic<Date> mockedInstant = mockStatic(Date.class)) {
            mockedInstant.when(() -> Date.from(any())).thenReturn(mockExpiryDate);

            String response = S3Service.generateExportDocSignedUrl(client, "object_path");

            verify(client).generatePresignedUrl(presignedUrlRequestCaptor.capture());
            GeneratePresignedUrlRequest capturedValues = presignedUrlRequestCaptor.getValue();

            assertEquals(mockUrl.toExternalForm(), response);
            assertEquals(mockExpiryDate, capturedValues.getExpiration());
            assertEquals(HttpMethod.GET, capturedValues.getMethod());

        }

    }

    @Test
    void unableToGenerateSignedURL() throws Exception {
        when(client.generatePresignedUrl(any())).thenThrow(SdkClientException.class);

        assertThrows(SdkClientException.class, () -> S3Service.generateExportDocSignedUrl(client, "object_path"));
    }

}
