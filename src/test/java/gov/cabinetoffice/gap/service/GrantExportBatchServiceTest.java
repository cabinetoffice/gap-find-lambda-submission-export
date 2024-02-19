package gov.cabinetoffice.gap.service;

import gov.cabinetoffice.gap.enums.GrantExportStatus;
import gov.cabinetoffice.gap.model.AddingS3ObjectKeyDTO;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class GrantExportBatchServiceTest {

    private static final OkHttpClient mockedHttpClient = mock(OkHttpClient.class);
    private static final UUID mockExportId = UUID.randomUUID();

    @BeforeEach
    void beforeEach() {
        reset(mockedHttpClient);
    }

    @Nested
    class updateGrantExportBatchRecordStatus {

        ArgumentCaptor<GrantExportStatus> grantExportStatusArgumentCaptor = ArgumentCaptor
                .forClass(GrantExportStatus.class);

        @Test
        void successfullyUpdateStatus() throws Exception {
            try (MockedStatic<RestService> mockedRestService = mockStatic(RestService.class)) {
                mockedRestService.when(() -> RestService.sendPatchRequest(any(), any(), anyString())).thenAnswer(i -> null);
                GrantExportBatchService.updateGrantExportBatchRecordStatus(mockedHttpClient, mockExportId.toString(), GrantExportStatus.COMPLETE);
                mockedRestService.verify(() -> RestService.sendPatchRequest(any(), grantExportStatusArgumentCaptor.capture(), anyString()));

                final GrantExportStatus capturedStatus = grantExportStatusArgumentCaptor.getValue();

                assertThat(capturedStatus.toString()).isEqualTo("COMPLETE");
            }
        }

        @Test
        void shouldThrowExceptionWhenRequestThrowsAnError() {
            try (MockedStatic<RestService> mockedRestService = mockStatic(RestService.class)) {
                mockedRestService
                        .when(() -> RestService.sendPatchRequest(any(), any(GrantExportStatus.class), anyString()))
                        .thenThrow(new RuntimeException());

                assertThrows(RuntimeException.class, () -> GrantExportBatchService.
                        updateGrantExportBatchRecordStatus(mockedHttpClient, mockExportId.toString(), GrantExportStatus.COMPLETE));
            }
        }
    }

    @Nested
    class addS3ObjectKeyToGrantExportBatchRecord {

        ArgumentCaptor<AddingS3ObjectKeyDTO>  addingS3ObjectKeyDTOCaptor = ArgumentCaptor
                .forClass(AddingS3ObjectKeyDTO.class);
        final String s3ObjectKey =  "s3ObjectKey";

        @Test
        void successfullyAddsS3ObjectKey() throws Exception {

            try (MockedStatic<RestService> mockedRestService = mockStatic(RestService.class)) {
                mockedRestService.when(() -> RestService.sendPatchRequest(any(), any(), anyString())).thenAnswer(i -> null);

                GrantExportBatchService.addS3ObjectKeyToGrantExportBatchRecord(mockedHttpClient, mockExportId.toString(), s3ObjectKey);

                mockedRestService.verify(() -> RestService.sendPatchRequest(any(), addingS3ObjectKeyDTOCaptor.capture(), anyString()));

                final AddingS3ObjectKeyDTO capturedDTO = addingS3ObjectKeyDTOCaptor.getValue();

                assertThat(capturedDTO.getS3ObjectKey()).isEqualTo(s3ObjectKey);

            }
        }

        @Test
        void ShouldThrowExceptionWhenRequestThrowsAnError() {
            try (MockedStatic<RestService> mockedRestService = mockStatic(RestService.class)) {
                mockedRestService
                        .when(() -> RestService.sendPatchRequest(any(), any(AddingS3ObjectKeyDTO.class), anyString()))
                        .thenThrow(new RuntimeException());

                assertThrows(RuntimeException.class, () -> GrantExportBatchService.
                        addS3ObjectKeyToGrantExportBatchRecord(mockedHttpClient, mockExportId.toString(), s3ObjectKey));
            }
        }
    }
}
