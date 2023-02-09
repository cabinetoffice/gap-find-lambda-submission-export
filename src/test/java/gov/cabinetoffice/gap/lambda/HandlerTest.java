package gov.cabinetoffice.gap.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.tests.EventLoader;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectResult;
import gov.cabinetoffice.gap.enums.GrantExportStatus;
import gov.cabinetoffice.gap.service.*;
import gov.cabinetoffice.gap.testData.TestContext;
import gov.cabinetoffice.gap.utils.HelperUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;

import java.io.File;

import static gov.cabinetoffice.gap.testData.SubmissionTestData.SCHEME_ID;
import static gov.cabinetoffice.gap.testData.SubmissionTestData.SUBMISSION_WITH_ESSENTIAL_SECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class HandlerTest {

    private static AmazonS3 client;
    private static MockedStatic<AmazonS3ClientBuilder> mockedS3Builder;
    private static MockedStatic<HelperUtils> mockedHelperUtils;
    private static MockedStatic<S3Service> mockedS3Service;
    private static MockedStatic<ExportRecordService> mockedExportService;
    private static MockedStatic<SubmissionService> mockedSubmissionService;

    private Context createContext() {
        return new TestContext();
    }

    @BeforeAll
    static void beforeAll() {
        client = mock(AmazonS3.class);
        mockedS3Builder = mockStatic(AmazonS3ClientBuilder.class);
        mockedS3Builder.when(AmazonS3ClientBuilder::defaultClient).thenReturn(client);
        mockedHelperUtils = mockStatic(HelperUtils.class);
        mockedS3Service = mockStatic(S3Service.class);
        mockedExportService = mockStatic(ExportRecordService.class);
        mockedSubmissionService = mockStatic(SubmissionService.class);
    }

    @AfterAll
    static void afterAll() {
        mockedS3Builder.close();
        mockedHelperUtils.close();
        mockedS3Service.close();
        mockedExportService.close();
        mockedSubmissionService.close();
    }

    @Test
    void SuccessfullyRunningThroughAllActions() {
        final SQSEvent event = EventLoader.loadSQSEvent("testEvent.json");
        final Context contextMock = createContext();
        final String submissionId = event.getRecords().get(0).getMessageAttributes().get("submissionId")
                .getStringValue();
        final String exportBatchId = event.getRecords().get(0).getMessageAttributes().get("exportBatchId")
                .getStringValue();
        final String emailAddress = event.getRecords().get(0).getMessageAttributes().get("emailAddress")
                .getStringValue();
        final String applicationId = event.getRecords().get(0).getMessageAttributes().get("applicationId")
                .getStringValue();

        final String expectedFilename = "test_org_name_GAP_LL_20220927_00001";

        mockedSubmissionService.when(() -> SubmissionService.getSubmissionData(exportBatchId, submissionId))
                .thenReturn(SUBMISSION_WITH_ESSENTIAL_SECTION);

        mockedHelperUtils
                .when(() -> HelperUtils.generateFilename("test org name", SUBMISSION_WITH_ESSENTIAL_SECTION.getGapId()))
                .thenCallRealMethod();

        when(client.putObject(anyString(), anyString(), any(File.class))).thenReturn(new PutObjectResult());

        mockedS3Service.when(() -> S3Service.generateExportDocSignedUrl(any(), anyString()))
                .thenReturn("mock-signed-url/mock-file");

        mockedHelperUtils.when(() -> HelperUtils.getRedirectUrl(SCHEME_ID, exportBatchId))
                .thenReturn("test.co.uk/testing");

        mockedExportService.when(() -> ExportRecordService.getOutstandingExportsCount(exportBatchId)).thenReturn(0L);

        try (final MockedStatic<OdtService> mockedOdtService = mockStatic(OdtService.class);
                final MockedStatic<ZipService> mockedZipService = mockStatic(ZipService.class);
                final MockedStatic<NotifyService> mockedNotifyService = mockStatic(NotifyService.class)) {

            mockedZipService.when(() -> ZipService.createZip(any(), anyString(), anyString(), anyString()))
                    .thenAnswer((Answer<Void>) invocation -> null);

            mockedZipService.when(() -> ZipService.uploadZip(eq(SUBMISSION_WITH_ESSENTIAL_SECTION), any()))
                    .thenReturn(SUBMISSION_WITH_ESSENTIAL_SECTION.getGapId() + "/mock_filename.zip");

            Handler handler = new Handler();
            SQSBatchResponse response = handler.handleRequest(event, contextMock);

            assertEquals(new SQSBatchResponse(), response);

            // STEP 0
            mockedExportService.verify(() -> ExportRecordService.updateExportRecordStatus(exportBatchId, submissionId,
                    GrantExportStatus.PROCESSING));

            // STEP 1
            mockedSubmissionService.verify(() -> SubmissionService.getSubmissionData(exportBatchId, submissionId));

            // STEP 2
            mockedOdtService
                    .verify(() -> OdtService.generateSingleOdt(SUBMISSION_WITH_ESSENTIAL_SECTION, expectedFilename));

            // STEP 3
            mockedZipService.verify(() -> ZipService.createZip(client, expectedFilename, applicationId, submissionId));

            // STEP 4
            mockedZipService.verify(() -> ZipService.uploadZip(SUBMISSION_WITH_ESSENTIAL_SECTION, expectedFilename));

            // STEP 5
            mockedS3Service.verify(() -> S3Service.generateExportDocSignedUrl(client,
                    SUBMISSION_WITH_ESSENTIAL_SECTION.getGapId() + "/mock_filename.zip"));

            // STEP 6
            mockedExportService.verify(() -> ExportRecordService.addSignedUrlToExportRecord(exportBatchId, submissionId,
                    "mock-signed-url/mock-file"));

            // STEP 7
            mockedExportService.verify(() -> ExportRecordService.updateExportRecordStatus(exportBatchId, submissionId,
                    GrantExportStatus.COMPLETE));

            // STEP 8
            mockedExportService.verify(() -> ExportRecordService.getOutstandingExportsCount(exportBatchId));
            mockedNotifyService.verify(() -> NotifyService.sendConfirmationEmail(emailAddress, exportBatchId,
                    SUBMISSION_WITH_ESSENTIAL_SECTION.getSchemeId(), submissionId));
        }
    }

    @Test
    void ShouldNotSendEmailIfThereIsStillOutstandingExports() {
        final SQSEvent event = EventLoader.loadSQSEvent("testEvent.json");
        final Context contextMock = createContext();
        final String submissionId = event.getRecords().get(0).getMessageAttributes().get("submissionId")
                .getStringValue();
        final String exportBatchId = event.getRecords().get(0).getMessageAttributes().get("exportBatchId")
                .getStringValue();

        when(client.putObject(anyString(), anyString(), any(File.class))).thenReturn(new PutObjectResult());

        mockedSubmissionService.when(() -> SubmissionService.getSubmissionData(exportBatchId, submissionId))
                .thenReturn(SUBMISSION_WITH_ESSENTIAL_SECTION);

        mockedHelperUtils
                .when(() -> HelperUtils.generateFilename("test org name", SUBMISSION_WITH_ESSENTIAL_SECTION.getGapId()))
                .thenCallRealMethod();

        when(client.putObject(anyString(), anyString(), any(File.class))).thenReturn(new PutObjectResult());

        mockedS3Service.when(() -> S3Service.generateExportDocSignedUrl(any(), anyString()))
                .thenReturn("mock-signed-url/mock-file");

        mockedHelperUtils.when(() -> HelperUtils.getRedirectUrl(SCHEME_ID, exportBatchId))
                .thenReturn("test.co.uk/testing");

        mockedExportService.when(() -> ExportRecordService.getOutstandingExportsCount(exportBatchId)).thenReturn(10L);

        try (final MockedStatic<OdtService> ignored = mockStatic(OdtService.class);
             final MockedStatic<ZipService> mockedZipService = mockStatic(ZipService.class);
             final MockedStatic<NotifyService> mockedNotifyService = mockStatic(NotifyService.class)) {

            mockedZipService.when(() -> ZipService.createZip(any(), anyString(), anyString(), anyString()))
                    .thenAnswer((Answer<Void>) invocation -> null);

            mockedZipService.when(() -> ZipService.uploadZip(eq(SUBMISSION_WITH_ESSENTIAL_SECTION), any()))
                    .thenReturn(SUBMISSION_WITH_ESSENTIAL_SECTION.getGapId() + "/mock_filename.zip");

            Handler handler = new Handler();
            handler.handleRequest(event, contextMock);

            mockedNotifyService.verify(
                    () -> NotifyService.sendConfirmationEmail(anyString(), anyString(), anyString(), anyString()),
                    never());

        }
    }

}
