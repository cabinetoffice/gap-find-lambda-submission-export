package gov.cabinetoffice.gap.service;

import gov.cabinetoffice.gap.model.SendLambdaExportEmailDTO;
import gov.cabinetoffice.gap.utils.HelperUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

public class NotifyServiceTest {

    @Nested
    class sendConfirmationEmail {

        ArgumentCaptor<SendLambdaExportEmailDTO> sendEmailDTOCaptor = ArgumentCaptor
                .forClass(SendLambdaExportEmailDTO.class);

        @Test
        void ShouldSuccessfullySendEmail() throws Exception {

            final String mockEmail = "testing@and.digital";
            final UUID mockBatchId = UUID.randomUUID();
            final UUID mockSubmissionId = UUID.randomUUID();
            final String mockSchemeId = "12345";
            final String mockRedirectUrl = "http://testing.com/url-to-test";

            try (MockedStatic<RestService> mockedRestService = mockStatic(RestService.class);
                    MockedStatic<HelperUtils> mockedHelperUtils = mockStatic(HelperUtils.class)) {

                mockedHelperUtils.when(() -> HelperUtils.getRedirectUrl(mockSchemeId, mockBatchId.toString()))
                        .thenReturn(mockRedirectUrl);
                mockedRestService.when(() -> RestService.sendPostRequest(any(), anyString())).thenAnswer(i -> null);

                NotifyService.sendConfirmationEmail(mockEmail, mockBatchId.toString(), mockSchemeId,
                        mockSubmissionId.toString());

                mockedHelperUtils.verify(() -> HelperUtils.getRedirectUrl(mockSchemeId, mockBatchId.toString()));
                mockedRestService.verify(() -> RestService.sendPostRequest(sendEmailDTOCaptor.capture(), anyString()));

                SendLambdaExportEmailDTO capturedDTO = sendEmailDTOCaptor.getValue();

                assertThat(capturedDTO.getEmailAddress()).isEqualTo(mockEmail);
                assertThat(capturedDTO.getExportId()).isEqualTo(mockBatchId);
                assertThat(capturedDTO.getSubmissionId()).isEqualTo(mockSubmissionId);
                assertThat(capturedDTO.getPersonalisation()).hasFieldOrPropertyWithValue("REDIRECT_URL",
                        mockRedirectUrl);

            }
        }

        @Test
        void ShouldThrowExceptionWhenRequestThrowsAnError() throws Exception {
            final String mockEmail = "testing@and.digital";
            final String mockBatchId = UUID.randomUUID().toString();
            final String mockSubmissionId = UUID.randomUUID().toString();
            final String mockSchemeId = "12345";
            final String mockRedirectUrl = "http://testing.com/url-to-test";

            try (MockedStatic<RestService> mockedRestService = mockStatic(RestService.class);
                    MockedStatic<HelperUtils> mockedHelperUtils = mockStatic(HelperUtils.class)) {

                mockedHelperUtils.when(() -> HelperUtils.getRedirectUrl(mockSchemeId, mockBatchId.toString()))
                        .thenReturn(mockRedirectUrl);
                mockedRestService
                        .when(() -> RestService.sendPostRequest(any(SendLambdaExportEmailDTO.class), anyString()))
                        .thenThrow(new RuntimeException());

                assertThrows(RuntimeException.class, () -> NotifyService.sendConfirmationEmail(mockEmail, mockBatchId,
                        mockSchemeId, mockSubmissionId));

            }
        }

    }

}
