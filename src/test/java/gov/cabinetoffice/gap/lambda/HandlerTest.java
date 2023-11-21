package gov.cabinetoffice.gap.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.tests.EventLoader;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectResult;
import gov.cabinetoffice.gap.enums.GrantExportStatus;
import gov.cabinetoffice.gap.service.ExportRecordService;
import gov.cabinetoffice.gap.service.NotifyService;
import gov.cabinetoffice.gap.service.OdtService;
import gov.cabinetoffice.gap.service.SubmissionService;
import gov.cabinetoffice.gap.service.ZipService;
import gov.cabinetoffice.gap.testData.TestContext;
import gov.cabinetoffice.gap.utils.HelperUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;

import java.io.File;

import static gov.cabinetoffice.gap.testData.SubmissionTestData.SCHEME_ID;
import static gov.cabinetoffice.gap.testData.SubmissionTestData.V1_SUBMISSION_WITH_ESSENTIAL_SECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

public class HandlerTest {

    private static AmazonS3 s3client;
    private static MockedStatic<AmazonS3ClientBuilder> mockedS3Builder;
    private static MockedStatic<HelperUtils> mockedHelperUtils;
    private static MockedStatic<ExportRecordService> mockedExportService;
    private static MockedStatic<SubmissionService> mockedSubmissionService;

    @BeforeAll
    static void beforeAll() {
        s3client = mock(AmazonS3.class);
        mockedS3Builder = mockStatic(AmazonS3ClientBuilder.class);
        mockedS3Builder.when(AmazonS3ClientBuilder::defaultClient).thenReturn(s3client);
        mockedHelperUtils = mockStatic(HelperUtils.class);
        mockedExportService = mockStatic(ExportRecordService.class);
        mockedSubmissionService = mockStatic(SubmissionService.class);
    }

    @AfterAll
    static void afterAll() {
        mockedS3Builder.close();
        mockedHelperUtils.close();
        mockedExportService.close();
        mockedSubmissionService.close();
    }

    private Context createContext() {
        return new TestContext();
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

        mockedSubmissionService.when(() -> SubmissionService.getSubmissionData(any(), anyString(), anyString()))
                .thenReturn(V1_SUBMISSION_WITH_ESSENTIAL_SECTION);

        mockedHelperUtils
                .when(() -> HelperUtils.generateFilename("test org name", V1_SUBMISSION_WITH_ESSENTIAL_SECTION.getGapId()))
                .thenCallRealMethod();

        when(s3client.putObject(anyString(), anyString(), any(File.class))).thenReturn(new PutObjectResult());

        mockedHelperUtils.when(() -> HelperUtils.getRedirectUrl(SCHEME_ID, exportBatchId))
                .thenReturn("test.co.uk/testing");

        mockedExportService.when(() -> ExportRecordService.getOutstandingExportsCount(any(), eq(exportBatchId))).thenReturn(0L);

        try (final MockedStatic<OdtService> mockedOdtService = mockStatic(OdtService.class);
             final MockedStatic<ZipService> mockedZipService = mockStatic(ZipService.class);
             final MockedStatic<NotifyService> mockedNotifyService = mockStatic(NotifyService.class)) {

            mockedZipService.when(() -> ZipService.createZip(any(), anyString(), anyString(), anyString()))
                    .thenAnswer((Answer<Void>) invocation -> null);

            final String mockS3Key = V1_SUBMISSION_WITH_ESSENTIAL_SECTION.getGapId() + '/mock_filename.zip';

            mockedZipService.when(() -> ZipService.uploadZip(eq(V1_SUBMISSION_WITH_ESSENTIAL_SECTION), any()))
                    .thenReturn(mockS3Key);

            Handler handler = new Handler();
            SQSBatchResponse response = handler.handleRequest(event, contextMock);

            assertEquals(new SQSBatchResponse(), response);

            // STEP 0
            mockedExportService.verify(() -> ExportRecordService.updateExportRecordStatus(any(), eq(exportBatchId), eq(submissionId),
                    eq(GrantExportStatus.PROCESSING)));

            // STEP 1
            mockedSubmissionService.verify(() -> SubmissionService.getSubmissionData(any(), eq(exportBatchId), eq(submissionId)));

            // STEP 2
            mockedOdtService
                    .verify(() -> OdtService.generateSingleOdt(V1_SUBMISSION_WITH_ESSENTIAL_SECTION, expectedFilename));

            // STEP 3
            mockedZipService.verify(() -> ZipService.createZip(s3client, expectedFilename, applicationId, submissionId));

            // STEP 4
            mockedZipService.verify(() -> ZipService.uploadZip(V1_SUBMISSION_WITH_ESSENTIAL_SECTION, expectedFilename));

            // STEP 5
            mockedExportService.verify(() -> ExportRecordService.addS3ObjectKeyToExportRecord(any(), eq(exportBatchId), eq(submissionId),
                    eq(mockS3Key)));

            // STEP 6
            mockedExportService.verify(() -> ExportRecordService.updateExportRecordStatus(any(), eq(exportBatchId), eq(submissionId),
                    eq(GrantExportStatus.COMPLETE)));

            // STEP 7
            mockedExportService.verify(() -> ExportRecordService.getOutstandingExportsCount(any(), eq(exportBatchId)));
            mockedNotifyService.verify(() -> NotifyService.sendConfirmationEmail(any(), eq(emailAddress), eq(exportBatchId),
                    eq(V1_SUBMISSION_WITH_ESSENTIAL_SECTION.getSchemeId()), eq(submissionId)));
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

        when(s3client.putObject(anyString(), anyString(), any(File.class))).thenReturn(new PutObjectResult());

        mockedSubmissionService.when(() -> SubmissionService.getSubmissionData(any(), eq(exportBatchId), eq(submissionId)))
                .thenReturn(V1_SUBMISSION_WITH_ESSENTIAL_SECTION);

        mockedHelperUtils
                .when(() -> HelperUtils.generateFilename("test org name", V1_SUBMISSION_WITH_ESSENTIAL_SECTION.getGapId()))
                .thenCallRealMethod();

        when(s3client.putObject(anyString(), anyString(), any(File.class))).thenReturn(new PutObjectResult());

        mockedHelperUtils.when(() -> HelperUtils.getRedirectUrl(SCHEME_ID, exportBatchId))
                .thenReturn("test.co.uk/testing");

        mockedExportService.when(() -> ExportRecordService.getOutstandingExportsCount(any(), eq(exportBatchId))).thenReturn(10L);

        try (final MockedStatic<OdtService> ignored = mockStatic(OdtService.class);
             final MockedStatic<ZipService> mockedZipService = mockStatic(ZipService.class);
             final MockedStatic<NotifyService> mockedNotifyService = mockStatic(NotifyService.class)) {

            mockedZipService.when(() -> ZipService.createZip(any(), anyString(), anyString(), anyString()))
                    .thenAnswer((Answer<Void>) invocation -> null);

            mockedZipService.when(() -> ZipService.uploadZip(eq(V1_SUBMISSION_WITH_ESSENTIAL_SECTION), any()))
                    .thenReturn(V1_SUBMISSION_WITH_ESSENTIAL_SECTION.getGapId() + "/mock_filename.zip");

            Handler handler = new Handler();
            handler.handleRequest(event, contextMock);

            mockedNotifyService.verify(
                    () -> NotifyService.sendConfirmationEmail(any(), anyString(), anyString(), anyString(), anyString()),
                    never());

        }
    }

}
