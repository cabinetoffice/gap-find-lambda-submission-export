package gov.cabinetoffice.gap.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import gov.cabinetoffice.gap.enums.GrantExportStatus;
import gov.cabinetoffice.gap.model.Submission;
import gov.cabinetoffice.gap.service.*;
import gov.cabinetoffice.gap.utils.HelperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

public class Handler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private static final Logger logger = LoggerFactory.getLogger(Handler.class);

    @Override
    public SQSBatchResponse handleRequest(final SQSEvent event, final Context context) {
        try {
            final AmazonS3 client = AmazonS3ClientBuilder.defaultClient();
            final Map<String, SQSEvent.MessageAttribute> messageAttributes = event.getRecords().get(0)
                    .getMessageAttributes();
            final String submissionId = messageAttributes.get("submissionId").getStringValue();
            final String emailAddress = messageAttributes.get("emailAddress").getStringValue();
            final String exportBatchId = messageAttributes.get("exportBatchId").getStringValue();
            final String applicationId = messageAttributes.get("applicationId").getStringValue();

            logger.info("Received message with submissionId: {} and exportBatchId: {}", submissionId, exportBatchId);

            // STEP 0 - update export record to PROCESSING
            ExportRecordService.updateExportRecordStatus(exportBatchId, submissionId, GrantExportStatus.PROCESSING);

            // STEP 1 - get submission from database
            // legal name is assigned from the response they give in the essential questions section
            final Submission submission = SubmissionService.getSubmissionData(exportBatchId, submissionId);
            String legalName = submission.getSectionById("ESSENTIAL").getQuestionById("APPLICANT_ORG_NAME").getResponse();
            submission.setLegalName(legalName);

            // STEP 2 - generate .odt from submission
            final String filename = HelperUtils.generateFilename(submission.getLegalName(), submission.getGapId());
            OdtService.generateSingleOdt(submission, filename);

            // STEP 3 - download all relevant attachments and zip along with .odt
            ZipService.createZip(client, filename, applicationId, submissionId);

            // STEP 4 - upload zip to S3
            String zipObjectKey = ZipService.uploadZip(submission, filename);

            // STEP 5 - generate signed url for zip file
            String signedUrl = S3Service.generateExportDocSignedUrl(client, zipObjectKey);
            logger.info("Signed URL created");

            // Step 6 - Add signedURL to export
            ExportRecordService.addSignedUrlToExportRecord(exportBatchId, submissionId, signedUrl);

            // STEP 7 - update export record to COMPLETE
            ExportRecordService.updateExportRecordStatus(exportBatchId, submissionId, GrantExportStatus.COMPLETE);

            // STEP 8 - if final submission, email admin
            final Long outstandingCount = ExportRecordService.getOutstandingExportsCount(exportBatchId);

            if (Objects.equals(outstandingCount, 0L)) {
                NotifyService.sendConfirmationEmail(emailAddress, exportBatchId, submission.getSchemeId(),
                        submissionId);
            }
            else {
                logger.info(
                        String.format("Outstanding exports for export batch %s: %s", exportBatchId, outstandingCount));
            }

            // STEP 9 - actually be a stateless AWS lambda
            client.shutdown();
            ZipService.deleteTmpDirContents();
            logger.info("Connections & tmp dir cleared");
        }
        catch (Exception e) {
            logger.error("Could not process message", e);
            throw new RuntimeException(e);
        }

        logger.info("Message processed successfully");
        return new SQSBatchResponse();
    }

}