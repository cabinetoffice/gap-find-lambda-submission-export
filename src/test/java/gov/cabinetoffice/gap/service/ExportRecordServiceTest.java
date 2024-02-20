package gov.cabinetoffice.gap.service;

import gov.cabinetoffice.gap.enums.GrantExportStatus;
import gov.cabinetoffice.gap.model.AddingS3ObjectKeyDTO;
import gov.cabinetoffice.gap.model.GrantExportDTO;
import gov.cabinetoffice.gap.model.GrantExportListDTO;
import gov.cabinetoffice.gap.model.OutstandingExportCountDTO;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ExportRecordServiceTest {

    private static final OkHttpClient mockedHttpClient = mock(OkHttpClient.class);
    private static final UUID mockExportId = UUID.randomUUID();
    private static final UUID mockSubmissionId = UUID.randomUUID();

    @BeforeEach
    void beforeEach() {
        reset(mockedHttpClient);
    }

    @Nested
    class updateExportRecordStatus {

        ArgumentCaptor<String> grantExportStatusArgumentCaptor = ArgumentCaptor
                .forClass(String.class);

        @Test
        void successfullyUpdateStatus() throws Exception {
            try (MockedStatic<RestService> mockedRestService = mockStatic(RestService.class)) {
                mockedRestService.when(() -> RestService.sendPostRequest(any(), any(), anyString())).thenAnswer(i -> null);
                ExportRecordService.updateExportRecordStatus(mockedHttpClient, mockExportId.toString(), mockSubmissionId.toString(), GrantExportStatus.COMPLETE);

                mockedRestService.verify(() -> RestService.sendPostRequest(any(), grantExportStatusArgumentCaptor.capture(), anyString()));

                final String capturedStatus = grantExportStatusArgumentCaptor.getValue();

                assertThat(capturedStatus).isEqualTo("\"COMPLETE\"");
            }
        }

        @Test
        void shouldThrowExceptionWhenRequestThrowsAnError() {
            try (MockedStatic<RestService> mockedRestService = mockStatic(RestService.class)) {
                mockedRestService
                        .when(() -> RestService.sendPostRequest(any(), any(String.class), anyString()))
                        .thenThrow(new RuntimeException());

                assertThrows(RuntimeException.class, () -> ExportRecordService
                        .updateExportRecordStatus(mockedHttpClient, mockExportId.toString(), mockSubmissionId.toString(),
                                GrantExportStatus.COMPLETE));
            }
        }
    }

    @Nested
    class getOutstandingExportsCount {
        @Test
        void successfullyGetOutstandingExportsCount() throws Exception {
            final OutstandingExportCountDTO expectedResponse = new OutstandingExportCountDTO(1L);
            try (MockedStatic<RestService> mockedRestService = mockStatic(RestService.class)) {
                when(RestService.sendGetRequest(any(), any(), anyString(), eq(OutstandingExportCountDTO.class))).thenReturn(expectedResponse);
                final long response = ExportRecordService.getOutstandingExportsCount(mockedHttpClient, mockExportId.toString());

                mockedRestService.verify(() -> RestService.sendGetRequest(any(), any(), anyString(), eq(OutstandingExportCountDTO.class)));

                assertThat(response).isEqualTo(expectedResponse.getOutstandingCount());
            }
        }

        @Test
        void ShouldThrowExceptionWhenRequestThrowsAnError() {
            try (MockedStatic<RestService> mockedRestService = mockStatic(RestService.class)) {
                mockedRestService
                        .when(() -> RestService.sendGetRequest(any(), any(), anyString(), eq(OutstandingExportCountDTO.class)))
                        .thenThrow(new RuntimeException());

                assertThrows(RuntimeException.class, () -> ExportRecordService
                        .getOutstandingExportsCount(mockedHttpClient, mockExportId.toString()));
            }
        }
    }

    @Nested
    class addS3ObjectKeyToExportRecord {

        ArgumentCaptor<AddingS3ObjectKeyDTO> addingS3ObjectKeyDTOCaptor = ArgumentCaptor
                .forClass(AddingS3ObjectKeyDTO.class);
        final String s3ObjectKey =  "s3ObjectKey";

        @Test
        void successfullyAddsS3ObjectKey() throws Exception {

            try (MockedStatic<RestService> mockedRestService = mockStatic(RestService.class)) {
                mockedRestService.when(() -> RestService.sendPatchRequest(any(), any(), anyString())).thenAnswer(i -> null);

                ExportRecordService.addS3ObjectKeyToExportRecord(mockedHttpClient, mockExportId.toString(),
                        mockSubmissionId.toString(), s3ObjectKey);

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

                assertThrows(RuntimeException.class, () -> ExportRecordService.addS3ObjectKeyToExportRecord(mockedHttpClient,
                        mockExportId.toString(), mockSubmissionId.toString(), s3ObjectKey));
            }
        }
    }

    @Nested
    class getCompletedExportRecordsByBatchId {
        @Test
        void successfullyGetsCompletedExportRecords() throws Exception {
            final List<GrantExportDTO> grantExports = Collections.singletonList(new GrantExportDTO(
                mockExportId,
                UUID.randomUUID(),
                1,
                GrantExportStatus.COMPLETE,
                "test-email@gamil.com",
                Instant.now(),
                1,
                null,
                "location.zip"));
            final GrantExportListDTO expectedResponse = new GrantExportListDTO(mockExportId, grantExports);

            try (MockedStatic<RestService> mockedRestService = mockStatic(RestService.class)) {
                when(RestService.sendGetRequest(any(), any(), anyString(), eq(GrantExportListDTO.class))).thenReturn(expectedResponse);

                final GrantExportListDTO response = ExportRecordService.getCompletedExportRecordsByBatchId(mockedHttpClient, mockExportId.toString());

                mockedRestService.verify(() -> RestService.sendGetRequest(any(), any(), anyString(), eq(GrantExportListDTO.class)));

                assertThat(response).isEqualTo(expectedResponse);
                assertThat(response.getExportBatchId()).isEqualTo(expectedResponse.getExportBatchId());
                assertThat(response.getGrantExports().size()).isEqualTo(expectedResponse.getGrantExports().size());
            }
        }

        @Test
        void ShouldThrowExceptionWhenRequestThrowsAnError() {
            try (MockedStatic<RestService> mockedRestService = mockStatic(RestService.class)) {
                mockedRestService
                        .when(() -> RestService.sendGetRequest(any(), any(), anyString(), eq(GrantExportListDTO.class)))
                        .thenThrow(new RuntimeException());

                assertThrows(RuntimeException.class, () -> ExportRecordService
                        .getCompletedExportRecordsByBatchId(mockedHttpClient, mockExportId.toString()));
            }
        }
    }
}
