package gov.cabinetoffice.gap.service;

import gov.cabinetoffice.gap.model.SendLambdaExportEmailDTO;
import gov.cabinetoffice.gap.utils.HelperUtils;
import okhttp3.OkHttpClient;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class NotifyService {

    public static void sendConfirmationEmail(final OkHttpClient restClient, final String emailAddress, final String exportBatchId,
                                             final String schemeId, final String submissionId) throws Exception {
        final String redirectURL = HelperUtils.getRedirectUrl(schemeId, exportBatchId);
        final Map<String, String> personalisation = Collections.singletonMap("REDIRECT_URL", redirectURL);
        final SendLambdaExportEmailDTO sendLambdaExportEmailDTO = SendLambdaExportEmailDTO.builder()
                .exportId(UUID.fromString(exportBatchId)).emailAddress(emailAddress)
                .submissionId(UUID.fromString(submissionId)).personalisation(personalisation).build();

        RestService.sendPostRequest(restClient, sendLambdaExportEmailDTO, "/emails/sendLambdaConfirmationEmail");

    }

}
