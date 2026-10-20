package gov.cabinetoffice.gap.utils;

import java.util.List;
import java.util.regex.Pattern;

public class UnsupportedCharacterHelper {

    // Keep in step with ValidationConstants.UNSUPPORTED_CHARACTER_PATTERNS in gap-find-applicant-backend.
    // The first three patterns corrupt the submission ODT: odfdom writes characters above U+FFFF as two
    // surrogate character references (e.g. &#55357;&#56832;), which are not valid XML.
    // The last removes emoji below U+FFFF (Unicode Emoji_Presentation=Yes, plus the U+FE0F emoji variation
    // selector and U+20E3 keycap) so they are treated the same as other emoji. Text symbols such as ✓ ★ © are kept.
    public static final List<Pattern> UNSUPPORTED_CHARACTER_PATTERNS = List.of(
            Pattern.compile("[\\x{10000}-\\x{10FFFF}]"),
            Pattern.compile("[\\uD800-\\uDFFF]"),
            Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\uFFFE\\uFFFF]"),
            Pattern.compile("[\\uFE0F\\u20E3\\u231A\\u231B\\u23E9-\\u23EC\\u23F0\\u23F3\\u25FD\\u25FE\\u2614\\u2615"
                    + "\\u2648-\\u2653\\u267F\\u2693\\u26A1\\u26AA\\u26AB\\u26BD\\u26BE\\u26C4\\u26C5\\u26CE\\u26D4"
                    + "\\u26EA\\u26F2\\u26F3\\u26F5\\u26FA\\u26FD\\u2705\\u270A\\u270B\\u2728\\u274C\\u274E"
                    + "\\u2753-\\u2755\\u2757\\u2795-\\u2797\\u27B0\\u27BF\\u2B1B\\u2B1C\\u2B50\\u2B55]")
    );

    private UnsupportedCharacterHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean containsUnsupportedCharacters(final String value) {
        return value != null && UNSUPPORTED_CHARACTER_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(value).find());
    }

    public static String stripUnsupportedCharacters(final String value) {
        if (value == null) {
            return null;
        }

        String stripped = value;
        for (final Pattern pattern : UNSUPPORTED_CHARACTER_PATTERNS) {
            stripped = pattern.matcher(stripped).replaceAll("");
        }
        return stripped;
    }
}
