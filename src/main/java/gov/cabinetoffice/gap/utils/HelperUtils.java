package gov.cabinetoffice.gap.utils;

public class HelperUtils {

    private HelperUtils() {
        throw new IllegalStateException("Utility class");
    }

    private static final String DOMAIN = System.getenv("REDIRECT_DOMAIN");

    public static String getRedirectUrl(String schemeId, String batchId) {
        return DOMAIN + "/scheme/" + schemeId + "/" + batchId;
    }

    public static String generateFilename(final String legalName, final String gapId) {

        if (legalName == null || gapId == null) {
            throw new RuntimeException("legalName and gapId cannot be null");
        }

        final String truncatedLegalName = legalName.length() > 50 ? legalName.substring(0, 50).trim() : legalName;
        final String cleanLegalName = truncatedLegalName
                .replace(" ", "_")
                .replaceAll("[<>:\"/\\\\?*]", "_");

        final String cleanGapId = gapId.replace("-", "_");
        return gapId.isEmpty() ? cleanLegalName : String.format("%s_%s", cleanLegalName, cleanGapId);
    }

}
