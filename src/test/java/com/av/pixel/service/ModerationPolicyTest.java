package com.av.pixel.service;

import com.av.pixel.service.ModerationPolicy.DetectedLabel;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ModerationPolicyTest {

    private static final String DEFAULT_BLOCKLIST =
            "Explicit:60,Explicit Nudity:60,Explicit Sexual Activity:60,Sex Toys:60";

    private final ModerationPolicy policy = new ModerationPolicy(DEFAULT_BLOCKLIST);

    @Test
    void blocksExplicitNudityAboveThreshold() {
        Optional<String> blocked = policy.firstBlocked(List.of(
                new DetectedLabel("Exposed Female Nipple", "Explicit Nudity", 94.2d),
                new DetectedLabel("Explicit Nudity", "Explicit", 94.2d)));

        assertThat(blocked).contains("Exposed Female Nipple");
    }

    @Test
    void allowsBlockedLabelBelowItsThreshold() {
        Optional<String> blocked = policy.firstBlocked(List.of(
                new DetectedLabel("Explicit Nudity", "Explicit", 55.0d)));

        assertThat(blocked).isEmpty();
    }

    @Test
    void allowsKissingOnTheLipsAtHighConfidence() {
        Optional<String> blocked = policy.firstBlocked(List.of(
                new DetectedLabel("Kissing on the Lips", "Non-Explicit Nudity of Intimate parts and Kissing", 99.0d)));

        assertThat(blocked).isEmpty();
    }

    @Test
    void allowsSwimwearAndUnderwear() {
        Optional<String> blocked = policy.firstBlocked(List.of(
                new DetectedLabel("Female Swimwear Or Underwear", "Swimwear or Underwear", 99.0d)));

        assertThat(blocked).isEmpty();
    }

    @Test
    void allowsNonExplicitNudityThatExposesNoPrivateParts() {
        Optional<String> blocked = policy.firstBlocked(List.of(
                new DetectedLabel("Bare Back", "Non-Explicit Nudity", 99.0d),
                new DetectedLabel("Exposed Male Nipple", "Non-Explicit Nudity", 99.0d)));

        assertThat(blocked).isEmpty();
    }

    @Test
    void allowsObstructedIntimateParts() {
        Optional<String> blocked = policy.firstBlocked(List.of(
                new DetectedLabel("Obstructed Female Nipple", "Obstructed Intimate Parts", 99.0d)));

        assertThat(blocked).isEmpty();
    }

    @Test
    void matchesOnParentNameWhenChildIsNotListed() {
        Optional<String> blocked = policy.firstBlocked(List.of(
                new DetectedLabel("Exposed Male Genitalia", "Explicit Nudity", 88.0d)));

        assertThat(blocked).contains("Exposed Male Genitalia");
    }

    @Test
    void allowsUnrelatedCategories() {
        Optional<String> blocked = policy.firstBlocked(List.of(
                new DetectedLabel("Alcoholic Beverages", "Alcohol", 99.0d),
                new DetectedLabel("Weapon Violence", "Violence", 97.0d)));

        assertThat(blocked).isEmpty();
    }

    @Test
    void reportsHighestConfidenceViolationFirst() {
        Optional<String> blocked = policy.firstBlocked(List.of(
                new DetectedLabel("Exposed Buttocks or Anus", "Explicit Nudity", 80.0d),
                new DetectedLabel("Exposed Female Genitalia", "Explicit Nudity", 96.0d)));

        assertThat(blocked).contains("Exposed Female Genitalia");
    }

    @Test
    void blocksExplicitSexualActivity() {
        Optional<String> blocked = policy.firstBlocked(List.of(
                new DetectedLabel("Explicit Sexual Activity", "Explicit", 70.0d)));
        assertThat(blocked).contains("Explicit Sexual Activity");
    }

    @Test
    void blocksSexToys() {
        Optional<String> blocked = policy.firstBlocked(List.of(
                new DetectedLabel("Sex Toys", "Explicit", 65.0d)));
        assertThat(blocked).contains("Sex Toys");
    }

    @Test
    void blocksThirdLevelNudityEvenWithoutAncestorLabels() {
        Optional<String> blocked = policy.firstBlocked(List.of(
                new DetectedLabel("Exposed Female Genitalia", "Explicit Nudity", 80.0d)));
        assertThat(blocked).contains("Exposed Female Genitalia");
    }

    @Test
    void shippedConfigurationMatchesTheGuardedBlocklist() throws Exception {
        Properties shipped = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
            shipped.load(in);
        }
        assertThat(shipped.getProperty("moderation.blocked-labels")).isEqualTo(DEFAULT_BLOCKLIST);
    }

    @Test
    void lowestThresholdIsTheSmallestConfiguredConfidence() {
        assertThat(policy.lowestThreshold()).isEqualTo(60.0d);
    }

    @Test
    void toleratesWhitespaceAndSkipsMalformedEntries() {
        ModerationPolicy messy = new ModerationPolicy("  Explicit Nudity : 60 , garbage , Violence:notanumber ");

        assertThat(messy.lowestThreshold()).isEqualTo(60.0d);
        assertThat(messy.firstBlocked(List.of(new DetectedLabel("Explicit Nudity", "Explicit", 61.0d)))).contains("Explicit Nudity");
        assertThat(messy.firstBlocked(List.of(new DetectedLabel("Weapon Violence", "Violence", 99.0d)))).isEmpty();
    }

    @Test
    void emptyBlocklistBlocksNothingAndReportsMaximumThreshold() {
        ModerationPolicy empty = new ModerationPolicy("");

        assertThat(empty.lowestThreshold()).isEqualTo(100.0d);
        assertThat(empty.firstBlocked(List.of(new DetectedLabel("Explicit Nudity", "Explicit", 99.0d)))).isEmpty();
    }
}
