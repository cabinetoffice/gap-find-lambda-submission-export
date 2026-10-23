package gov.cabinetoffice.gap.utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelperUtilsTest {

    @Nested
    class getRedirectUrl {

        @Test
        void successfullyGenerateURLUsingDomain() {

            String url = HelperUtils.getRedirectUrl("12345", "12345-6789-12345");

            assertEquals("test.co.uk/apply/testing/scheme/12345/12345-6789-12345", url);

        }

    }

    @Nested
    class generateFilename {

        @Test
        void successfullyGenerateFilename() {

            String generatedFilename = HelperUtils.generateFilename("Totally Legit Org", "GAP-GAP-20221019");

            assertThat(generatedFilename).isEqualTo("Totally_Legit_Org_GAP_GAP_20221019");

        }

        @Test
        void specialCharactersAreReplaced() {
            String generatedFilename = HelperUtils.generateFilename("Org<with>lots/of\\special", "GAP-GAP-20221019");

            assertThat(generatedFilename).isEqualTo("Org_with_lots_of_special_GAP_GAP_20221019");
        }

        @Test
        void legalNamesLongerThan25CharsAreTrimmed() {
            final String longLegalName = "a legal name with more than fifty characters that needs to be trimmed by the document export lambda so that file name length limits in windows don't cause problems";
            final String trimmedLegalName = longLegalName.substring(0, 25)
                    .trim()
                    .replace(" ", "_");

            String generatedFilename = HelperUtils.generateFilename(longLegalName, "GAP-GAP-20221019");

            assertThat(generatedFilename).isEqualTo(trimmedLegalName + "_GAP_GAP_20221019");
        }

        @Test
        void emojisAreRemoved() {
            String generatedFilename = HelperUtils.generateFilename("Rainbow \uD83C\uDF08 Charity \u2705", "GAP-GAP-20221019");

            assertThat(generatedFilename).isEqualTo("Rainbow_Charity_GAP_GAP_20221019");
        }

        @Test
        void emojisAreRemovedBeforeTrimming() {
            String generatedFilename = HelperUtils.generateFilename("\uD83D\uDE80\uD83D\uDE80 Twenty five characters long name", "GAP-GAP-20221019");

            assertThat(generatedFilename).isEqualTo("Twenty_five_characters_lo_GAP_GAP_20221019");
        }

        @Test
        void legalNameOfOnlyEmojisFallsBackToDefault() {
            String generatedFilename = HelperUtils.generateFilename("\uD83C\uDF08\uD83D\uDE80", "GAP-GAP-20221019");

            assertThat(generatedFilename).isEqualTo("submission_GAP_GAP_20221019");
        }

        @Test
        void accentedCharactersAreKept() {
            String generatedFilename = HelperUtils.generateFilename("Café Société", "");

            assertThat(generatedFilename).isEqualTo("Café_Société");
        }

        @Test
        void nullLegalNameGenerateFilename() {

            assertThatThrownBy(() -> HelperUtils.generateFilename(null, "GAP-GAP-20221019"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("legalName and gapId cannot be null");

        }

        @Test
        void nullGapIdGenerateFilename() {

            assertThatThrownBy(() -> HelperUtils.generateFilename("Totally Legit Org", null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("legalName and gapId cannot be null");

        }

    }

}
