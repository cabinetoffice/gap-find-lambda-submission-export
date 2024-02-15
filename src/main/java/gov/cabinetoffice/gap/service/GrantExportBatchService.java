package gov.cabinetoffice.gap.service;

import gov.cabinetoffice.gap.enums.GrantExportStatus;
import gov.cabinetoffice.gap.lambda.Handler;
import gov.cabinetoffice.gap.model.AddingS3ObjectKeyDTO;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrantExportBatchService {

    private static final Logger logger = LoggerFactory.getLogger(Handler.class);

    public static void updateGrantExportBatchRecordStatus(OkHttpClient restClient, String exportId, GrantExportStatus newStatus)
            throws Exception {

        final String patchEndpoint = "/grant-export-batch/" + exportId + "/status";
        try{
            logger.info("Sending patch request to {} to update status to: {}", patchEndpoint, newStatus.toString());
            RestService.sendPatchRequest(restClient, "\"" + newStatus.toString() + "\"", patchEndpoint);
        }
        catch (Exception e) {
            logger.error("Unable to send patch request to update status to {} with error message {}", newStatus.toString(), e);
        }
    }

    public static void addS3ObjectKeyToGrantExportBatchRecord(OkHttpClient restClient, String exportId, String s3ObjectKey)
            throws Exception {

        final String patchEndpoint = "/grant-export-batch/" + exportId + "/s3-object-key";
        RestService.sendPatchRequest(restClient, new AddingS3ObjectKeyDTO(s3ObjectKey), patchEndpoint);
    }

}
