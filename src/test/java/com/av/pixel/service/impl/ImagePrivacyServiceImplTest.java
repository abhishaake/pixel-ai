package com.av.pixel.service.impl;

import com.av.pixel.cache.RLock;
import com.av.pixel.dao.Generations;
import com.av.pixel.dto.UserCreditDTO;
import com.av.pixel.dto.UserDTO;
import com.av.pixel.enums.OrderTypeEnum;
import com.av.pixel.exception.Error;
import com.av.pixel.request.UpdateImagePrivacyRequest;
import com.av.pixel.response.ImagePrivacyResponse;
import com.av.pixel.service.AdminConfigService;
import com.av.pixel.service.UserCreditService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImagePrivacyServiceImplTest {

    private static final String OWNER = "P100";
    private static final String STRANGER = "P200";

    @Mock private MongoTemplate mongoTemplate;
    @Mock private UserCreditService userCreditService;
    @Mock private AdminConfigService adminConfigService;
    @Mock private RLock locker;

    private ImagePrivacyServiceImpl service;
    private ObjectId genId;

    @BeforeEach
    void setUp() {
        service = new ImagePrivacyServiceImpl(mongoTemplate, userCreditService, adminConfigService, locker);
        genId = new ObjectId();
        when(locker.tryLock(anyString(), anyLong())).thenReturn(true);
        when(adminConfigService.getPrivacyUnlockCost()).thenReturn(50);
        when(userCreditService.getUserCredit(anyString()))
                .thenReturn(new UserCreditDTO().setAvailable(500));
        when(userCreditService.debitUserCredit(anyString(), anyInt(), any(), anyString(), anyString()))
                .thenReturn(new UserCreditDTO().setAvailable(450));
        when(mongoTemplate.save(any(Generations.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Generations stored(String userCode, Boolean privateImage, Boolean unlocked) {
        Generations g = new Generations()
                .setUserCode(userCode)
                .setPrivateImage(privateImage)
                .setPrivacyUnlocked(unlocked);
        g.setId(genId);
        when(mongoTemplate.findOne(any(Query.class), eq(Generations.class))).thenReturn(g);
        return g;
    }

    private UserDTO owner() {
        return new UserDTO().setCode(OWNER);
    }

    private UpdateImagePrivacyRequest request(boolean makePrivate) {
        return new UpdateImagePrivacyRequest()
                .setGenerationId(genId.toString())
                .setPrivateImage(makePrivate);
    }

    @Test
    void firstUnlockChargesConfiguredCostAndSetsBothFlags() {
        Generations g = stored(OWNER, false, null);

        ImagePrivacyResponse res = service.updateImagePrivacy(owner(), request(true));

        assertThat(res.getPrivateImage()).isTrue();
        assertThat(res.getPrivacyUnlocked()).isTrue();
        assertThat(res.getChargedCredits()).isEqualTo(50);
        assertThat(res.getAvailableCredits()).isEqualTo(450);
        assertThat(g.getPrivateImage()).isTrue();
        assertThat(g.getPrivacyUnlocked()).isTrue();
        verify(userCreditService).debitUserCredit(OWNER, 50, OrderTypeEnum.PRIVACY_UNLOCK, "SERVER", genId.toString());
    }

    @Test
    void togglingAnAlreadyUnlockedImageIsFree() {
        stored(OWNER, true, true);

        ImagePrivacyResponse res = service.updateImagePrivacy(owner(), request(false));

        assertThat(res.getPrivateImage()).isFalse();
        assertThat(res.getChargedCredits()).isZero();
        verify(userCreditService, never()).debitUserCredit(anyString(), anyInt(), any(), anyString(), anyString());
    }

    @Test
    void imageGeneratedPrivateIsTreatedAsAlreadyUnlocked() {
        Generations g = stored(OWNER, true, null);

        ImagePrivacyResponse res = service.updateImagePrivacy(owner(), request(false));

        assertThat(res.getChargedCredits()).isZero();
        assertThat(res.getPrivacyUnlocked()).isTrue();
        assertThat(g.getPrivacyUnlocked()).isTrue();
        verify(userCreditService, never()).debitUserCredit(anyString(), anyInt(), any(), anyString(), anyString());
    }

    @Test
    void reprivatisingAfterAFreeUnlockIsStillFree() {
        stored(OWNER, false, true);

        ImagePrivacyResponse res = service.updateImagePrivacy(owner(), request(true));

        assertThat(res.getPrivateImage()).isTrue();
        assertThat(res.getChargedCredits()).isZero();
        verify(userCreditService, never()).debitUserCredit(anyString(), anyInt(), any(), anyString(), anyString());
    }

    @Test
    void nonOwnerIsRejectedWithoutChargeOrWrite() {
        stored(STRANGER, false, null);

        assertThatThrownBy(() -> service.updateImagePrivacy(owner(), request(true)))
                .isInstanceOf(Error.class)
                .satisfies(e -> assertThat(((Error) e).getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(userCreditService, never()).debitUserCredit(anyString(), anyInt(), any(), anyString(), anyString());
        verify(mongoTemplate, never()).save(any(Generations.class));
    }

    @Test
    void insufficientCreditsReturns402AndChangesNothing() {
        stored(OWNER, false, null);
        when(userCreditService.getUserCredit(OWNER)).thenReturn(new UserCreditDTO().setAvailable(10));

        assertThatThrownBy(() -> service.updateImagePrivacy(owner(), request(true)))
                .isInstanceOf(Error.class)
                .hasMessage("Not enough credits")
                .satisfies(e -> assertThat(((Error) e).getHttpStatus()).isEqualTo(HttpStatus.PAYMENT_REQUIRED));

        verify(userCreditService, never()).debitUserCredit(anyString(), anyInt(), any(), anyString(), anyString());
        verify(mongoTemplate, never()).save(any(Generations.class));
    }

    @Test
    void makingALockedImagePublicIsANoOp() {
        stored(OWNER, false, null);

        ImagePrivacyResponse res = service.updateImagePrivacy(owner(), request(false));

        assertThat(res.getPrivateImage()).isFalse();
        assertThat(res.getPrivacyUnlocked()).isFalse();
        assertThat(res.getChargedCredits()).isZero();
        verify(userCreditService, never()).debitUserCredit(anyString(), anyInt(), any(), anyString(), anyString());
        verify(mongoTemplate, never()).save(any(Generations.class));
    }

    @Test
    void missingGenerationReturns404() {
        when(mongoTemplate.findOne(any(Query.class), eq(Generations.class))).thenReturn(null);

        assertThatThrownBy(() -> service.updateImagePrivacy(owner(), request(true)))
                .isInstanceOf(Error.class)
                .satisfies(e -> assertThat(((Error) e).getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void invalidGenerationIdReturns400() {
        UpdateImagePrivacyRequest bad = new UpdateImagePrivacyRequest()
                .setGenerationId("not-an-object-id")
                .setPrivateImage(true);

        assertThatThrownBy(() -> service.updateImagePrivacy(owner(), bad))
                .isInstanceOf(Error.class)
                .satisfies(e -> assertThat(((Error) e).getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void lockIsKeyedOnUserSoConcurrentUnlocksOfDifferentImagesCannotOverdraw() {
        stored(OWNER, false, null);

        service.updateImagePrivacy(owner(), request(true));

        // The key must not contain the generation id, otherwise two different
        // images could each pass the balance check and push the account negative.
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(locker).tryLock(key.capture(), anyLong());

        assertThat(key.getValue()).isEqualTo("privacy_unlock_" + OWNER);
        assertThat(key.getValue()).doesNotContain(genId.toString());
    }

    @Test
    void concurrentChangeReturns409() {
        stored(OWNER, false, null);
        when(locker.tryLock(anyString(), anyLong())).thenReturn(false);

        assertThatThrownBy(() -> service.updateImagePrivacy(owner(), request(true)))
                .isInstanceOf(Error.class)
                .satisfies(e -> assertThat(((Error) e).getHttpStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(userCreditService, never()).debitUserCredit(anyString(), anyInt(), any(), anyString(), anyString());
    }
}
