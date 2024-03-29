package gov.cabinetoffice.gap.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.tests.EventLoader;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import gov.cabinetoffice.gap.enums.GrantExportStatus;
import gov.cabinetoffice.gap.model.GrantExportDTO;
import gov.cabinetoffice.gap.model.GrantExportListDTO;
import gov.cabinetoffice.gap.service.*;
import gov.cabinetoffice.gap.testData.TestContext;
import gov.cabinetoffice.gap.utils.HelperUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static gov.cabinetoffice.gap.testData.SubmissionTestData.SCHEME_ID;
import static gov.cabinetoffice.gap.testData.SubmissionTestData.V1_SUBMISSION_WITH_ESSENTIAL_SECTION;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.*;

public class HandlerTest {

    private static AmazonS3 s3client;
    private static MockedStatic<AmazonS3ClientBuilder> mockedS3Builder;
    private static MockedStatic<HelperUtils> mockedHelperUtils;
    private static MockedStatic<ExportRecordService> mockedExportService;
    private static MockedStatic<SubmissionService> mockedSubmissionService;
    private static MockedStatic<AmazonSNSClientBuilder> mockedSnsBuilder;
    private static AmazonSNSClient mockedSnsClient;
    private static MockedStatic<NotifyService> mockedNotifyService;

    @BeforeAll
    static void beforeAll() {
        s3client = mock(AmazonS3.class);
        mockedS3Builder = mockStatic(AmazonS3ClientBuilder.class);
        mockedHelperUtils = mockStatic(HelperUtils.class);
        mockedExportService = mockStatic(ExportRecordService.class);
        mockedSubmissionService = mockStatic(SubmissionService.class);
        mockedSnsClient = mock(AmazonSNSClient.class);
        mockedSnsBuilder = mockStatic(AmazonSNSClientBuilder.class);
        mockedNotifyService = mockStatic(NotifyService.class);
    }

    @BeforeEach
    void beforeEach() {
        mockedS3Builder.reset();
        mockedS3Builder.when(AmazonS3ClientBuilder::defaultClient).thenReturn(s3client);
        mockedHelperUtils.reset();
        mockedExportService.reset();
        mockedSubmissionService.reset();
        mockedSnsBuilder.reset();
        mockedNotifyService.reset();

        mockedS3Builder.clearInvocations();
        mockedHelperUtils.clearInvocations();
        mockedExportService.clearInvocations();
        mockedSubmissionService.clearInvocations();
        mockedSnsBuilder.clearInvocations();
        mockedNotifyService.clearInvocations();
    }

    @AfterAll
    static void afterAll() {
        mockedS3Builder.close();
        mockedHelperUtils.close();
        mockedExportService.close();
        mockedSubmissionService.close();
        mockedSnsBuilder.close();
        mockedNotifyService.close();
    }

    private Context createContext() {
        return new TestContext();
    }

    @Test
    void noRecordsInSqsEvent() {
        final Context contextMock = createContext();
        final SQSEvent event = EventLoader.loadSQSEvent("emptyTestEvent.json");

        final Handler handler = new Handler();

        assertThatThrownBy(
                () -> handler.handleRequest(event, contextMock))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void updatesStatusWhenExportFails() throws Exception {
        final SQSEvent event = EventLoader.loadSQSEvent("testEvent.json");
        final Context contextMock = createContext();
        final String submissionId = event.getRecords().get(0).getMessageAttributes().get("submissionId")
                .getStringValue();
        final String exportBatchId = event.getRecords().get(0).getMessageAttributes().get("exportBatchId")
                .getStringValue();

        when(SubmissionService.getSubmissionData(any(), anyString(), anyString())).thenThrow(new RuntimeException());

        final Handler handler = new Handler();
        handler.handleRequest(event, contextMock);

        mockedExportService.verify(() -> ExportRecordService.updateExportRecordStatus(any(), eq(exportBatchId), eq(submissionId),
                eq(GrantExportStatus.PROCESSING)));
        mockedSubmissionService.verify(() -> SubmissionService.getSubmissionData(any(), eq(exportBatchId), eq(submissionId)));
        mockedExportService.verify(() -> ExportRecordService.updateExportRecordStatus(any(), eq(exportBatchId), eq(submissionId),
                eq(GrantExportStatus.FAILED)));
    }

    @Test
    void updatesStatusAndCreatesAttachmentZipWhenExportFails() {
        final SQSEvent event = EventLoader.loadSQSEvent("testEvent.json");
        final Context contextMock = createContext();
        final String submissionId = event.getRecords().get(0).getMessageAttributes().get("submissionId")
                .getStringValue();
        final String exportBatchId = event.getRecords().get(0).getMessageAttributes().get("exportBatchId")
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
             final MockedStatic<ZipService> mockedZipService = mockStatic(ZipService.class)) {

            mockedZipService.when(() -> ZipService.createZip(any(), anyString(), anyString(), anyString(), eq(true)))
                    .thenThrow(new RuntimeException());

            mockedZipService.when(() -> ZipService.createZip(any(), anyString(), anyString(), anyString(), eq(false)))
                    .thenAnswer((Answer<Void>) invocation -> null);

            final String mockS3Key = V1_SUBMISSION_WITH_ESSENTIAL_SECTION.getGapId() + "/attachments.zip";

            mockedZipService.when(() -> ZipService.uploadZip(eq(V1_SUBMISSION_WITH_ESSENTIAL_SECTION.getGapId()), any()))
                    .thenReturn(mockS3Key);

            Handler handler = new Handler();
            SQSBatchResponse response = handler.handleRequest(event, contextMock);

            assertEquals(new SQSBatchResponse(), response);

            // STEP 0
            mockedExportService.verify(() -> ExportRecordService.updateExportRecordStatus(any(), eq(exportBatchId), eq(submissionId),
                    eq(GrantExportStatus.PROCESSING)), atLeastOnce());

            // STEP 1
            mockedSubmissionService.verify(() -> SubmissionService.getSubmissionData(any(), eq(exportBatchId), eq(submissionId)), atLeastOnce());

            // STEP 2
            mockedOdtService
                    .verify(() -> OdtService.generateSingleOdt(V1_SUBMISSION_WITH_ESSENTIAL_SECTION, expectedFilename));

            // STEP 3
            mockedZipService.verify(() -> ZipService.createZip(s3client, expectedFilename, applicationId, submissionId, true));

            // Inside catch block
            mockedZipService.verify(() -> ZipService.createZip(s3client, expectedFilename, applicationId, submissionId, false));

            mockedZipService.verify(() -> ZipService.uploadZip(V1_SUBMISSION_WITH_ESSENTIAL_SECTION.getGapId(), "attachments"));

            mockedExportService.verify(() -> ExportRecordService.addS3ObjectKeyToExportRecord(any(), eq(exportBatchId), eq(submissionId),
                    eq(mockS3Key)));

            mockedExportService.verify(() -> ExportRecordService.updateExportRecordStatus(any(), eq(exportBatchId), eq(submissionId),
                    eq(GrantExportStatus.FAILED)), atLeastOnce());
        }
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

        final UUID exportId = UUID.randomUUID();
        final List<GrantExportDTO> grantExports = Collections.singletonList(new GrantExportDTO(
                exportId,
                UUID.randomUUID(),
                1,
                GrantExportStatus.COMPLETE,
                "test-email@gamil.com",
                Instant.now(),
                1,
                null,
                "location.zip"));
        final GrantExportListDTO completedGrantExportsMock = new GrantExportListDTO(exportId, grantExports);

        mockedExportService.when(() -> ExportRecordService.getCompletedExportRecordsByBatchId(any(), anyString()))
                .thenReturn(completedGrantExportsMock);

        mockedSubmissionService.when(() -> SubmissionService.getSubmissionData(any(), anyString(), anyString()))
                .thenReturn(V1_SUBMISSION_WITH_ESSENTIAL_SECTION);

        mockedHelperUtils
                .when(() -> HelperUtils.generateFilename("test org name", V1_SUBMISSION_WITH_ESSENTIAL_SECTION.getGapId()))
                .thenCallRealMethod();

        when(s3client.putObject(anyString(), anyString(), any(File.class))).thenReturn(new PutObjectResult());

        mockedHelperUtils.when(() -> HelperUtils.getRedirectUrl(SCHEME_ID, exportBatchId))
                .thenReturn("test.co.uk/testing");

        mockedExportService.when(() -> ExportRecordService.getRemainingExportsCount(any(), eq(exportBatchId))).thenReturn(0L);

        try (final MockedStatic<OdtService> mockedOdtService = mockStatic(OdtService.class);
             final MockedStatic<ZipService> mockedZipService = mockStatic(ZipService.class)) {

            mockedZipService.when(() -> ZipService.createZip(any(), anyString(), anyString(), anyString(), anyBoolean()))
                    .thenAnswer((Answer<Void>) invocation -> null);

            mockedZipService.when(() -> ZipService.createSuperZip(completedGrantExportsMock.getGrantExports()))
                    .thenAnswer((Answer<Void>) invocation -> null);

            final String mockS3Key = V1_SUBMISSION_WITH_ESSENTIAL_SECTION.getGapId() + "/mock_filename.zip";
            final String mockBatchS3Key = V1_SUBMISSION_WITH_ESSENTIAL_SECTION.getSchemeName() + "/location.zip";

            mockedZipService.when(() -> ZipService.uploadZip(eq(V1_SUBMISSION_WITH_ESSENTIAL_SECTION.getGapId()), any()))
                    .thenReturn(mockS3Key);

            mockedZipService.when(() -> ZipService.uploadZip(eq(V1_SUBMISSION_WITH_ESSENTIAL_SECTION.getSchemeId()), any()))
                    .thenReturn(mockBatchS3Key);

            Handler handler = new Handler();
            SQSBatchResponse response = handler.handleRequest(event, contextMock);

            assertEquals(new SQSBatchResponse(), response);

            // STEP 0
            mockedExportService.verify(() -> ExportRecordService.updateExportRecordStatus(any(), eq(exportBatchId), eq(submissionId),
                    eq(GrantExportStatus.PROCESSING)), atLeastOnce());

            // STEP 1
            mockedSubmissionService.verify(() -> SubmissionService.getSubmissionData(any(), eq(exportBatchId), eq(submissionId)), atLeastOnce());

            // STEP 2
            mockedOdtService
                    .verify(() -> OdtService.generateSingleOdt(V1_SUBMISSION_WITH_ESSENTIAL_SECTION, expectedFilename));

            // STEP 3
            mockedZipService.verify(() -> ZipService.createZip(s3client, expectedFilename, applicationId, submissionId, true));

            // STEP 4
            mockedZipService.verify(() -> ZipService.uploadZip(V1_SUBMISSION_WITH_ESSENTIAL_SECTION.getGapId(), expectedFilename));

            // STEP 5
            mockedExportService.verify(() -> ExportRecordService.addS3ObjectKeyToExportRecord(any(), eq(exportBatchId), eq(submissionId),
                    eq(mockS3Key)));

            // STEP 6
            mockedExportService.verify(() -> ExportRecordService.updateExportRecordStatus(any(), eq(exportBatchId), eq(submissionId),
                    eq(GrantExportStatus.COMPLETE)));

            // STEP 7
            mockedExportService.verify(() -> ExportRecordService.getRemainingExportsCount(any(), eq(exportBatchId)), atLeastOnce());
            mockedNotifyService.verify(() -> NotifyService.sendConfirmationEmail(any(), eq(emailAddress), eq(exportBatchId),
                    eq(V1_SUBMISSION_WITH_ESSENTIAL_SECTION.getSchemeId()), eq(submissionId)));
        }
    }

    @Test
    void ShouldNotSendConfirmationEmailIfThereIsStillOutstandingExports() {
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

        mockedExportService.when(() -> ExportRecordService.getRemainingExportsCount(any(), eq(exportBatchId))).thenReturn(10L);

        try (final MockedStatic<OdtService> ignored = mockStatic(OdtService.class);
             final MockedStatic<ZipService> mockedZipService = mockStatic(ZipService.class)) {

            mockedZipService.when(() -> ZipService.createZip(any(), anyString(), anyString(), anyString(), anyBoolean()))
                    .thenAnswer((Answer<Void>) invocation -> null);

            mockedZipService.when(() -> ZipService.uploadZip(eq(V1_SUBMISSION_WITH_ESSENTIAL_SECTION.getGapId()), any()))
                    .thenReturn(V1_SUBMISSION_WITH_ESSENTIAL_SECTION.getGapId() + "/mock_filename.zip");

            Handler handler = new Handler();
            handler.handleRequest(event, contextMock);

            mockedNotifyService.verify(
                    () -> NotifyService.sendConfirmationEmail(any(), anyString(), anyString(), anyString(), anyString()),
                    never());

        }
    }

    @Test
    void updatesStatusAndSendEmailWhenSuperZipFails() throws Exception {
        final SQSEvent event = EventLoader.loadSQSEvent("testEvent.json");
        final Context contextMock = createContext();
        final String submissionId = event.getRecords().get(0).getMessageAttributes().get("submissionId")
                .getStringValue();
        final String exportBatchId = event.getRecords().get(0).getMessageAttributes().get("exportBatchId")
                .getStringValue();
        final String emailAddress = event.getRecords().get(0).getMessageAttributes().get("emailAddress")
                .getStringValue();

        mockedSubmissionService.when(() -> SubmissionService.getSubmissionData(any(), anyString(), anyString()))
                .thenReturn(V1_SUBMISSION_WITH_ESSENTIAL_SECTION);

        mockedHelperUtils
                .when(() -> HelperUtils.generateFilename("test org name", V1_SUBMISSION_WITH_ESSENTIAL_SECTION.getGapId()))
                .thenCallRealMethod();

        when(s3client.putObject(anyString(), anyString(), any(File.class))).thenReturn(new PutObjectResult());

        mockedHelperUtils.when(() -> HelperUtils.getRedirectUrl(SCHEME_ID, exportBatchId))
                .thenReturn("test.co.uk/testing");

        mockedExportService.when(() -> ExportRecordService.getOutstandingExportsCount(any(), eq(exportBatchId))).thenReturn(0L);
        when(ExportRecordService.getCompletedExportRecordsByBatchId(any(), any())).thenThrow(new RuntimeException());

        try (final MockedStatic<OdtService> mockedOdtService = mockStatic(OdtService.class);
             final MockedStatic<ZipService> mockedZipService = mockStatic(ZipService.class)) {

            mockedZipService.when(() -> ZipService.createZip(any(), anyString(), anyString(), anyString(), anyBoolean()))
                    .thenAnswer((Answer<Void>) invocation -> null);

            final String mockS3Key = V1_SUBMISSION_WITH_ESSENTIAL_SECTION.getGapId() + "/mock_filename.zip";

            mockedZipService.when(() -> ZipService.uploadZip(eq(V1_SUBMISSION_WITH_ESSENTIAL_SECTION.getGapId()), any()))
                    .thenReturn(mockS3Key);

            final Handler handler = new Handler();
            handler.handleRequest(event, contextMock);

            mockedExportService.verify(() -> ExportRecordService.updateExportRecordStatus(any(), eq(exportBatchId), eq(submissionId),
                    eq(GrantExportStatus.PROCESSING)), atLeastOnce());

            mockedSubmissionService.verify(() -> SubmissionService.getSubmissionData(any(), eq(exportBatchId), eq(submissionId)), atLeastOnce());

            mockedExportService.verify(() -> ExportRecordService.addS3ObjectKeyToExportRecord(any(), eq(exportBatchId), eq(submissionId),
                    eq(mockS3Key)), atLeastOnce());

            mockedExportService.verify(() -> ExportRecordService.updateExportRecordStatus(any(), eq(exportBatchId), eq(submissionId),
                    eq(GrantExportStatus.COMPLETE)), atLeastOnce());

            mockedExportService.verify(() -> ExportRecordService.updateGrantExportBatchRecordStatus(any(), eq(exportBatchId),
                    eq(GrantExportStatus.FAILED)), atLeastOnce());

            mockedNotifyService.verify(() -> NotifyService.sendConfirmationEmail(any(), eq(emailAddress), eq(exportBatchId),
                    eq(V1_SUBMISSION_WITH_ESSENTIAL_SECTION.getSchemeId()), eq(submissionId)));

        }

    }

    @Test
    void ShouldSendOutstandingErrorsEmailIfThereAreStillOutstandingExportErrors() throws Exception {
        final SQSEvent event = EventLoader.loadSQSEvent("testEvent.json");
        final Context contextMock = createContext();
        final String exportBatchId = event.getRecords().get(0).getMessageAttributes().get("exportBatchId")
                .getStringValue();
        final PublishResult mockResult = new PublishResult().withMessageId("MESSAGE_ID");

        when(SubmissionService.getSubmissionData(any(), anyString(), anyString())).thenThrow(new RuntimeException());
        mockedExportService.when(() -> ExportRecordService.getRemainingExportsCount(any(), eq(exportBatchId))).thenReturn(0L);
        mockedExportService.when(() -> ExportRecordService.getFailedExportsCount(any(), eq(exportBatchId))).thenReturn(2L);

        mockedSnsBuilder.when(AmazonSNSClientBuilder::defaultClient).thenReturn(mockedSnsClient);
        when(mockedSnsClient.publish(any(PublishRequest.class))).thenReturn(mockResult);

        Handler handler = new Handler();
        handler.handleRequest(event, contextMock);

        verify(mockedSnsClient).publish(any());
        mockedExportService.verify(() -> ExportRecordService.getRemainingExportsCount(any(), eq(exportBatchId)), atLeastOnce());
        mockedExportService.verify(() -> ExportRecordService.getFailedExportsCount(any(), eq(exportBatchId)), atLeastOnce()
        );
    }

}
