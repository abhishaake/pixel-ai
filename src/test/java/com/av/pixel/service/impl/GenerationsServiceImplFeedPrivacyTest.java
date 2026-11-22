package com.av.pixel.service.impl;

import com.av.pixel.enums.ImagePrivacyEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GenerationsServiceImplFeedPrivacyTest {

    private static final String ME = "P100";
    private static final String OTHER = "P200";

    private static Boolean privacyFilter(ImagePrivacyEnum requested, boolean self, String userCode) {
        return GenerationsServiceImpl.resolveFeedPrivacy(requested, self, userCode).getPrivateImage();
    }

    @Test
    void strangerAskingForPrivateReceivesOnlyPublic() {
        assertThat(privacyFilter(ImagePrivacyEnum.PRIVATE, false, ME)).isFalse();
    }

    @Test
    void strangerAskingForBothReceivesOnlyPublic() {
        assertThat(privacyFilter(ImagePrivacyEnum.BOTH, false, ME)).isFalse();
    }

    @Test
    void anonymousCallerAskingForPrivateReceivesOnlyPublic() {
        assertThat(privacyFilter(ImagePrivacyEnum.PRIVATE, false, null)).isFalse();
    }

    @Test
    void defaultFeedIsPublic() {
        assertThat(privacyFilter(ImagePrivacyEnum.DEFAULT, false, ME)).isFalse();
    }

    @Test
    void ownProfileByDefaultShowsPublicAndPrivate() {
        assertThat(privacyFilter(ImagePrivacyEnum.DEFAULT, true, ME)).isNull();
    }

    @Test
    void ownProfileMayAskForPrivateOnly() {
        assertThat(privacyFilter(ImagePrivacyEnum.PRIVATE, true, ME)).isTrue();
    }

    @Test
    void preservedAllPrivacyCodesAreUnchanged() {
        assertThat(privacyFilter(ImagePrivacyEnum.DEFAULT, false, "P108")).isNull();
        assertThat(privacyFilter(ImagePrivacyEnum.PRIVATE, false, "P125")).isNull();
        assertThat(privacyFilter(ImagePrivacyEnum.PUBLIC, false, "P109")).isNull();
    }

    @Test
    void ownCodeAloneIsSelfProfile() {
        assertThat(GenerationsServiceImpl.isSelfProfile(ME, List.of(ME))).isTrue();
    }

    @Test
    void ownCodeMixedWithAnotherIsNotSelfProfile() {
        assertThat(GenerationsServiceImpl.isSelfProfile(ME, List.of(ME, OTHER))).isFalse();
    }

    @Test
    void emptyRequestOrAnonymousCallerIsNotSelfProfile() {
        assertThat(GenerationsServiceImpl.isSelfProfile(ME, List.of())).isFalse();
        assertThat(GenerationsServiceImpl.isSelfProfile(ME, null)).isFalse();
        assertThat(GenerationsServiceImpl.isSelfProfile(null, List.of(ME))).isFalse();
    }
}
