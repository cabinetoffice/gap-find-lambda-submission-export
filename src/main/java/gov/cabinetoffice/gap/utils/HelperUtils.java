package gov.cabinetoffice.gap.utils;

public class HelperUtils {

    private static final String DOMAIN = System.getenv("REDIRECT_DOMAIN");

    public static String getRedirectUrl(String schemeId, String batchId) {
        return DOMAIN + "/scheme/" + schemeId + "/" + batchId;
    }

    public static String generateFilename(String legalName, String gapId) {
        if(legalName == null || gapId == null) {
            throw new RuntimeException("legalName and gapId cannot be null");
        }

        String cleanLegalName = legalName.replace(" ", "_")
                .replace("/", "_");
        String cleanGapId = gapId.replace("-", "_");
        return String.format("%s_%s", cleanLegalName, cleanGapId);
    }

}
