# Paid Image Privacy Unlock Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let an image's owner change its visibility after generation, charging a one-time token fee the first time they make it private, free for every toggle thereafter.

**Architecture:** A new `PUT /api/v1/images/privacy` endpoint backed by a small, dedicated `ImagePrivacyService` (kept out of the 865-line `GenerationsServiceImpl`). A `privacyUnlocked` flag on the `generations` document records the entitlement, and treating a pre-existing `privateImage == true` as already-unlocked avoids any data migration. The Flutter app renders an owner-only priced button that becomes a free switch once unlocked.

**Tech Stack:** Java 17 / Spring Boot / Spring Data MongoDB / Lombok / JUnit 5 + Mockito (backend); Flutter / Dart / flutter_bloc / Retrofit + Dio / build_runner (app).

**Spec:** `docs/superpowers/specs/2026-08-31-paid-image-privacy-design.md`

## Global Constraints

- Backend repo: `/Users/mohit/Documents/GitHub/pixel-ai`. App repo: `/Users/mohit/Documents/GitHub/PixelAI`. **They are separate git repos — commit in each separately.**
- Backend branch: `feature/paid-image-privacy` (already created, spec already committed).
- Default unlock price is **50** tokens, held in `Constants.DEFAULT_PRIVACY_UNLOCK_COST`, overridable by `AdminConfig.privacyUnlockCost`.
- Never hardcode `50` in the Flutter app. The price always comes from the API payload field `privacyUnlockCost`.
- Effective unlocked test, used verbatim everywhere: `Boolean.TRUE.equals(g.getPrivacyUnlocked()) || Boolean.TRUE.equals(g.getPrivateImage())`.
- Lombok `@Data @Accessors(chain = true)` is used on all DAOs/DTOs — setters return `this`. Follow it.
- `RLock.tryLock(key, timeout)` takes **milliseconds**, not seconds. Existing callers pass `10`. Match that.
- Insufficient credits must throw exactly `new Error(HttpStatus.PAYMENT_REQUIRED, "Not enough credits")` so the app's existing 402 paywall path fires.
- **Do not commit** `src/main/resources/service-account-key.json` (untracked secret) or the `.DS_Store` files. Always `git add` explicit paths, never `git add -A`.
- **Maven must run on JDK 21.** The default `mvn` on this machine runs on Homebrew JDK 24, which this project's Lombok cannot process — every build fails with `java.lang.ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN`. This is pre-existing and unrelated to this feature. Prefix every Maven command with:
  ```
  export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
  ```

---

### Task 1: Backend test infrastructure

There is currently **no test infrastructure in the backend at all**: `pom.xml` has no `spring-boot-starter-test`, and `src/test/java/com/av/pixel/PixelApplicationTests.java` is entirely commented out. Every later backend task needs this, so it comes first.

**Files:**
- Modify: `pom.xml` (dependencies section)
- Delete: `src/test/java/com/av/pixel/PixelApplicationTests.java`
- Test: `src/test/java/com/av/pixel/SmokeTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces: a working `mvn test` command and JUnit 5 + Mockito on the test classpath for Tasks 2–5.

- [ ] **Step 1: Add the test dependency**

In `pom.xml`, inside `<dependencies>`, add:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

`spring-boot-starter-test` brings JUnit 5, Mockito, `mockito-junit-jupiter`, and AssertJ. Version is managed by the `spring-boot-starter-parent`, so do not add one.

- [ ] **Step 2: Remove the dead placeholder test**

```bash
rm src/test/java/com/av/pixel/PixelApplicationTests.java
```

It is 100% commented out and contributes nothing. A `@SpringBootTest` there would try to boot the whole app (Mongo, S3, SES) and fail — every test in this plan is a plain unit test with mocks instead.

- [ ] **Step 3: Write a smoke test proving the toolchain works**

Create `src/test/java/com/av/pixel/SmokeTest.java`:

```java
package com.av.pixel;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SmokeTest {

    @Test
    void junitAndAssertjAreOnTheClasspath() {
        assertThat(1 + 1).isEqualTo(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void mockitoIsOnTheClasspath() {
        List<String> mock = Mockito.mock(List.class);
        Mockito.when(mock.size()).thenReturn(3);
        assertThat(mock.size()).isEqualTo(3);
    }
}
```

- [ ] **Step 4: Run it**

```bash
mvn -q test -Dtest=SmokeTest
```

Expected: BUILD SUCCESS, 2 tests passing. If Maven cannot resolve dependencies offline, run `mvn -U test -Dtest=SmokeTest` once to refresh.

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/test/java/com/av/pixel/SmokeTest.java
git rm --cached src/test/java/com/av/pixel/PixelApplicationTests.java 2>/dev/null || true
git commit -m "test: add JUnit 5 + Mockito test infrastructure"
```

---

### Task 2: Data model, order type, and price resolution

**Files:**
- Modify: `src/main/java/com/av/pixel/dao/Generations.java`
- Modify: `src/main/java/com/av/pixel/dao/AdminConfig.java`
- Modify: `src/main/java/com/av/pixel/enums/OrderTypeEnum.java`
- Modify: `src/main/java/com/av/pixel/constants/Constants.java`
- Modify: `src/main/java/com/av/pixel/service/AdminConfigService.java`
- Modify: `src/main/java/com/av/pixel/service/impl/AdminConfigServiceImpl.java`
- Test: `src/test/java/com/av/pixel/service/impl/AdminConfigServiceImplPrivacyCostTest.java`

**Interfaces:**
- Consumes: Task 1's test toolchain.
- Produces:
  - `Generations.getPrivacyUnlocked() : Boolean` / `setPrivacyUnlocked(Boolean) : Generations`
  - `AdminConfig.getPrivacyUnlockCost() : Integer`
  - `OrderTypeEnum.PRIVACY_UNLOCK`
  - `Constants.DEFAULT_PRIVACY_UNLOCK_COST : Integer` (value `50`)
  - `AdminConfigService.getPrivacyUnlockCost() : Integer`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/av/pixel/service/impl/AdminConfigServiceImplPrivacyCostTest.java`:

```java
package com.av.pixel.service.impl;

import com.av.pixel.cache.Cache;
import com.av.pixel.dao.AdminConfig;
import com.av.pixel.repository.AdminConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AdminConfigServiceImplPrivacyCostTest {

    private static final String ADMIN_CONFIG_KEY = "ADMIN_CONFIG";

    @Mock
    private AdminConfigRepository adminConfigRepository;

    private AdminConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminConfigServiceImpl(adminConfigRepository);
        Cache.setAdminConfigMap(new ConcurrentHashMap<>());
    }

    private void cacheConfig(AdminConfig config) {
        ConcurrentHashMap<String, AdminConfig> map = new ConcurrentHashMap<>();
        map.put(ADMIN_CONFIG_KEY, config);
        Cache.setAdminConfigMap(map);
    }

    @Test
    void returnsConfiguredCostWhenPresent() {
        cacheConfig(new AdminConfig().setPrivacyUnlockCost(75));

        assertThat(service.getPrivacyUnlockCost()).isEqualTo(75);
    }

    @Test
    void fallsBackToFiftyWhenFieldIsNull() {
        cacheConfig(new AdminConfig());

        assertThat(service.getPrivacyUnlockCost()).isEqualTo(50);
    }

    @Test
    void fallsBackToFiftyWhenConfigIsMissingEntirely() {
        assertThat(service.getPrivacyUnlockCost()).isEqualTo(50);
    }
}
```

The last two cases are the point of this test. `getDefaultCredits()` next door returns `0` when unset; copying that here would silently make the feature free the moment the config document lacked the field.

- [ ] **Step 2: Run it to verify it fails**

```bash
mvn -q test -Dtest=AdminConfigServiceImplPrivacyCostTest
```

Expected: COMPILATION FAILURE — `cannot find symbol: method setPrivacyUnlockCost` and `method getPrivacyUnlockCost`.

- [ ] **Step 3: Add the model fields**

In `src/main/java/com/av/pixel/dao/Generations.java`, add after `Boolean videoEffect;`:

```java
    Boolean privacyUnlocked;
```

In `src/main/java/com/av/pixel/dao/AdminConfig.java`, add after `private Boolean isTestingEnabledForUsers;`:

```java
    private Integer privacyUnlockCost;
```

In `src/main/java/com/av/pixel/enums/OrderTypeEnum.java`, add `PRIVACY_UNLOCK` to the constant list:

```java
public enum OrderTypeEnum {
    IMAGE_GENERATION,
    VIDEO_EFFECT,
    AD_CREDIT,
    PURCHASE_CREDIT,
    PRIVACY_UNLOCK
}
```

In `src/main/java/com/av/pixel/constants/Constants.java`, add:

```java
    public static final Integer DEFAULT_PRIVACY_UNLOCK_COST = 50;
```

- [ ] **Step 4: Implement price resolution**

In `src/main/java/com/av/pixel/service/AdminConfigService.java`, add to the interface:

```java
    Integer getPrivacyUnlockCost ();
```

In `src/main/java/com/av/pixel/service/impl/AdminConfigServiceImpl.java`, add the method and the import `com.av.pixel.constants.Constants`:

```java
    @Override
    public Integer getPrivacyUnlockCost () {
        AdminConfig adminConfig = Cache.adminConfigMap.get(ADMIN_CONFIG_KEY);

        if (Objects.isNull(adminConfig) || Objects.isNull(adminConfig.getPrivacyUnlockCost())) {
            return Constants.DEFAULT_PRIVACY_UNLOCK_COST;
        }
        return adminConfig.getPrivacyUnlockCost();
    }
```

- [ ] **Step 5: Run the test to verify it passes**

```bash
mvn -q test -Dtest=AdminConfigServiceImplPrivacyCostTest
```

Expected: PASS, 3 tests.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/av/pixel/dao/Generations.java \
        src/main/java/com/av/pixel/dao/AdminConfig.java \
        src/main/java/com/av/pixel/enums/OrderTypeEnum.java \
        src/main/java/com/av/pixel/constants/Constants.java \
        src/main/java/com/av/pixel/service/AdminConfigService.java \
        src/main/java/com/av/pixel/service/impl/AdminConfigServiceImpl.java \
        src/test/java/com/av/pixel/service/impl/AdminConfigServiceImplPrivacyCostTest.java
git commit -m "feat: add privacyUnlocked flag and configurable privacy unlock cost"
```

---

### Task 3: Request/response contracts and DTO exposure

**Files:**
- Create: `src/main/java/com/av/pixel/request/UpdateImagePrivacyRequest.java`
- Create: `src/main/java/com/av/pixel/response/ImagePrivacyResponse.java`
- Modify: `src/main/java/com/av/pixel/dto/GenerationsDTO.java`
- Modify: `src/main/java/com/av/pixel/mapper/GenerationsMap.java`
- Modify: `src/main/java/com/av/pixel/service/impl/GenerationsServiceImpl.java:188,461,676`
- Test: `src/test/java/com/av/pixel/mapper/GenerationsMapPrivacyTest.java`

**Interfaces:**
- Consumes: `Generations.getPrivacyUnlocked()` from Task 2.
- Produces:
  - `UpdateImagePrivacyRequest` with `getGenerationId() : String`, `getPrivateImage() : Boolean`
  - `ImagePrivacyResponse` with chained setters `setGenerationId(String)`, `setPrivateImage(Boolean)`, `setPrivacyUnlocked(Boolean)`, `setChargedCredits(Integer)`, `setAvailableCredits(Integer)`
  - `GenerationsMap.toGenerationsDTO(Generations, Integer privacyUnlockCost)` and `GenerationsMap.toList(List<Generations>, TreeSet<String>, Map<String, User>, Integer privacyUnlockCost)`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/av/pixel/mapper/GenerationsMapPrivacyTest.java`:

```java
package com.av.pixel.mapper;

import com.av.pixel.dao.Generations;
import com.av.pixel.dto.GenerationsDTO;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GenerationsMapPrivacyTest {

    private Generations generation() {
        return (Generations) new Generations()
                .setUserCode("P100")
                .setModel("ideogram")
                .setRenderOption("TURBO")
                .setStyle("AUTO")
                .setId(new ObjectId());
    }

    @Test
    void mapsPrivacyUnlockedAndCost() {
        Generations g = generation().setPrivacyUnlocked(true).setPrivateImage(false);

        GenerationsDTO dto = GenerationsMap.toGenerationsDTO(g, 50);

        assertThat(dto.getPrivacyUnlocked()).isTrue();
        assertThat(dto.getPrivateImage()).isFalse();
        assertThat(dto.getPrivacyUnlockCost()).isEqualTo(50);
    }

    @Test
    void leavesPrivacyUnlockedNullWhenUnset() {
        Generations g = generation().setPrivateImage(false);

        GenerationsDTO dto = GenerationsMap.toGenerationsDTO(g, 50);

        assertThat(dto.getPrivacyUnlocked()).isNull();
    }
}
```

`setId` comes from `BaseEntity` and is not chained, hence the cast on the chained builder — set it last if the cast is awkward.

- [ ] **Step 2: Run it to verify it fails**

```bash
mvn -q test -Dtest=GenerationsMapPrivacyTest
```

Expected: COMPILATION FAILURE — no two-arg `toGenerationsDTO`, no `getPrivacyUnlocked` on the DTO.

- [ ] **Step 3: Create the request class**

`src/main/java/com/av/pixel/request/UpdateImagePrivacyRequest.java`:

```java
package com.av.pixel.request;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UpdateImagePrivacyRequest {

    String generationId;
    Boolean privateImage;
}
```

- [ ] **Step 4: Create the response class**

`src/main/java/com/av/pixel/response/ImagePrivacyResponse.java`:

```java
package com.av.pixel.response;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ImagePrivacyResponse {

    String generationId;
    Boolean privateImage;
    Boolean privacyUnlocked;
    Integer chargedCredits;
    Integer availableCredits;
}
```

Deliberately **not** annotated `@JsonInclude(NON_EMPTY)` — `chargedCredits: 0` must reach the app so it knows the toggle was free.

- [ ] **Step 5: Add the DTO fields**

In `src/main/java/com/av/pixel/dto/GenerationsDTO.java`, add after `Boolean privateImage;`:

```java
    Boolean privacyUnlocked;
    Integer privacyUnlockCost;
```

- [ ] **Step 6: Add the mapper overloads**

In `src/main/java/com/av/pixel/mapper/GenerationsMap.java`, keep the existing one-arg methods as delegates so no unrelated caller breaks, and add the cost-aware versions.

Replace the `toList` signature and its `toGenerationsDTO` call:

```java
    public static List<GenerationsDTO> toList (List<Generations> generations, TreeSet<String> likedGenerations, Map<String, User> userMap){
        return toList(generations, likedGenerations, userMap, null);
    }

    public static List<GenerationsDTO> toList (List<Generations> generations, TreeSet<String> likedGenerations,
                                               Map<String, User> userMap, Integer privacyUnlockCost){
        if (CollectionUtils.isEmpty(generations)) {
            return new ArrayList<>();
        }
        return generations.stream()
                .map(g -> {
                    GenerationsDTO genDTO = toGenerationsDTO(g, privacyUnlockCost);
                    if (Objects.nonNull(genDTO)) {
                        if (Objects.nonNull(likedGenerations) && likedGenerations.contains(g.getId().toString())){
                            genDTO.setSelfLike(true);
                        }
                        if (Objects.nonNull(userMap) && userMap.containsKey(g.getUserCode())) {
                            User user = userMap.get(g.getUserCode());
                            genDTO.setUserName(user.getFirstName());
                            genDTO.setUserImgUrl(user.getImageUrl());
                        }
                    }
                    return genDTO;
                })
                .toList();
    }
```

Then the DTO mapper:

```java
    public static GenerationsDTO toGenerationsDTO(Generations generations){
        return toGenerationsDTO(generations, null);
    }

    public static GenerationsDTO toGenerationsDTO(Generations generations, Integer privacyUnlockCost){
```

and inside its builder chain, after `.setPrivateImage(generations.getPrivateImage())`, add:

```java
                .setPrivacyUnlocked(generations.getPrivacyUnlocked())
                .setPrivacyUnlockCost(privacyUnlockCost)
```

- [ ] **Step 7: Run the test to verify it passes**

```bash
mvn -q test -Dtest=GenerationsMapPrivacyTest
```

Expected: PASS, 2 tests.

- [ ] **Step 8: Pass the real cost at the three call sites**

`GenerationsServiceImpl` already injects `adminConfigService`. Update all three:

- Line ~188 (in `generate`): `GenerationsMap.toGenerationsDTO(generations, adminConfigService.getPrivacyUnlockCost())`
- Line ~461 (in `filterImages`): `GenerationsMap.toList(generationsPage.getContent(), likedGenerations, userMap, adminConfigService.getPrivacyUnlockCost())`
- Line ~676 (in the video-effect path): `GenerationsMap.toGenerationsDTO(generations, adminConfigService.getPrivacyUnlockCost())`

Verify each line number before editing — earlier edits may have shifted them. Grep instead:

```bash
grep -n "GenerationsMap.toGenerationsDTO\|GenerationsMap.toList" src/main/java/com/av/pixel/service/impl/GenerationsServiceImpl.java
```

- [ ] **Step 9: Verify the whole module still compiles**

```bash
mvn -q test
```

Expected: BUILD SUCCESS, all tests passing.

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/av/pixel/request/UpdateImagePrivacyRequest.java \
        src/main/java/com/av/pixel/response/ImagePrivacyResponse.java \
        src/main/java/com/av/pixel/dto/GenerationsDTO.java \
        src/main/java/com/av/pixel/mapper/GenerationsMap.java \
        src/main/java/com/av/pixel/service/impl/GenerationsServiceImpl.java \
        src/test/java/com/av/pixel/mapper/GenerationsMapPrivacyTest.java
git commit -m "feat: expose privacyUnlocked and privacyUnlockCost in generation payloads"
```

---

### Task 4: ImagePrivacyService — the charge and toggle logic

This is the core of the feature. It goes in its own service rather than `GenerationsServiceImpl`, which is already 865 lines with 20 constructor dependencies — adding to it would make this logic effectively untestable.

**Files:**
- Create: `src/main/java/com/av/pixel/service/ImagePrivacyService.java`
- Create: `src/main/java/com/av/pixel/service/impl/ImagePrivacyServiceImpl.java`
- Test: `src/test/java/com/av/pixel/service/impl/ImagePrivacyServiceImplTest.java`

**Interfaces:**
- Consumes: `AdminConfigService.getPrivacyUnlockCost()`, `Generations.getPrivacyUnlocked()`, `OrderTypeEnum.PRIVACY_UNLOCK`, `UpdateImagePrivacyRequest`, `ImagePrivacyResponse` (Tasks 2–3); plus existing `UserCreditService`, `RLock`, `MongoTemplate`.
- Produces: `ImagePrivacyService.updateImagePrivacy(UserDTO, UpdateImagePrivacyRequest) : ImagePrivacyResponse` for Task 5.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/av/pixel/service/impl/ImagePrivacyServiceImplTest.java`:

```java
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
    void concurrentChangeReturns409() {
        stored(OWNER, false, null);
        when(locker.tryLock(anyString(), anyLong())).thenReturn(false);

        assertThatThrownBy(() -> service.updateImagePrivacy(owner(), request(true)))
                .isInstanceOf(Error.class)
                .satisfies(e -> assertThat(((Error) e).getHttpStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(userCreditService, never()).debitUserCredit(anyString(), anyInt(), any(), anyString(), anyString());
    }
}
```

Before writing this, confirm `UserCreditDTO` really has a chained `setAvailable(Integer)`:

```bash
cat src/main/java/com/av/pixel/dto/UserCreditDTO.java
```

If it is not `@Accessors(chain = true)`, build the DTO across two statements instead.

- [ ] **Step 2: Run it to verify it fails**

```bash
mvn -q test -Dtest=ImagePrivacyServiceImplTest
```

Expected: COMPILATION FAILURE — `ImagePrivacyServiceImpl` does not exist.

- [ ] **Step 3: Write the interface**

`src/main/java/com/av/pixel/service/ImagePrivacyService.java`:

```java
package com.av.pixel.service;

import com.av.pixel.dto.UserDTO;
import com.av.pixel.request.UpdateImagePrivacyRequest;
import com.av.pixel.response.ImagePrivacyResponse;

public interface ImagePrivacyService {

    ImagePrivacyResponse updateImagePrivacy (UserDTO userDTO, UpdateImagePrivacyRequest request);
}
```

- [ ] **Step 4: Write the implementation**

`src/main/java/com/av/pixel/service/impl/ImagePrivacyServiceImpl.java`:

```java
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
import com.av.pixel.service.ImagePrivacyService;
import com.av.pixel.service.UserCreditService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
@AllArgsConstructor
public class ImagePrivacyServiceImpl implements ImagePrivacyService {

    private final MongoTemplate mongoTemplate;
    private final UserCreditService userCreditService;
    private final AdminConfigService adminConfigService;
    private final RLock locker;

    @Override
    public ImagePrivacyResponse updateImagePrivacy (UserDTO userDTO, UpdateImagePrivacyRequest request) {

        if (Objects.isNull(request) || StringUtils.isEmpty(request.getGenerationId())
                || Objects.isNull(request.getPrivateImage())) {
            throw new Error(HttpStatus.BAD_REQUEST, "generationId and privateImage are required");
        }

        ObjectId generationObjectId;
        try {
            generationObjectId = new ObjectId(request.getGenerationId());
        } catch (IllegalArgumentException e) {
            throw new Error(HttpStatus.BAD_REQUEST, "Invalid generationId");
        }

        String userCode = Objects.nonNull(userDTO) ? userDTO.getCode() : null;
        if (StringUtils.isEmpty(userCode)) {
            throw new Error(HttpStatus.FORBIDDEN, "Not allowed");
        }

        Generations generation = findGeneration(generationObjectId);
        if (Objects.isNull(generation)) {
            throw new Error(HttpStatus.NOT_FOUND, "Image not found");
        }

        if (!userCode.equals(generation.getUserCode())) {
            throw new Error(HttpStatus.FORBIDDEN, "You can only change privacy of your own images");
        }

        boolean makePrivate = Boolean.TRUE.equals(request.getPrivateImage());
        boolean alreadyUnlocked = isUnlocked(generation);

        if (alreadyUnlocked) {
            return applyFree(generation, makePrivate, userCode);
        }

        if (!makePrivate) {
            // Locked and already public: nothing to change and nothing to charge for.
            return new ImagePrivacyResponse()
                    .setGenerationId(request.getGenerationId())
                    .setPrivateImage(false)
                    .setPrivacyUnlocked(false)
                    .setChargedCredits(0)
                    .setAvailableCredits(currentBalance(userCode));
        }

        return unlockAndApply(userCode, generation);
    }

    private ImagePrivacyResponse applyFree (Generations generation, boolean makePrivate, String userCode) {
        generation.setPrivateImage(makePrivate);
        generation.setPrivacyUnlocked(true);
        mongoTemplate.save(generation);

        return new ImagePrivacyResponse()
                .setGenerationId(generation.getId().toString())
                .setPrivateImage(makePrivate)
                .setPrivacyUnlocked(true)
                .setChargedCredits(0)
                .setAvailableCredits(currentBalance(userCode));
    }

    private ImagePrivacyResponse unlockAndApply (String userCode, Generations generation) {
        String key = "privacy_unlock_" + generation.getId();
        boolean locked = locker.tryLock(key, 10);

        if (!locked) {
            throw new Error(HttpStatus.CONFLICT, "Privacy change already in progress, please wait..");
        }

        try {
            Integer cost = adminConfigService.getPrivacyUnlockCost();

            UserCreditDTO credit = userCreditService.getUserCredit(userCode);
            Integer available = Objects.nonNull(credit) && Objects.nonNull(credit.getAvailable())
                    ? credit.getAvailable() : 0;

            if (available < cost) {
                throw new Error(HttpStatus.PAYMENT_REQUIRED, "Not enough credits");
            }

            generation.setPrivateImage(true);
            generation.setPrivacyUnlocked(true);
            mongoTemplate.save(generation);

            // Saved before debiting on purpose: if the debit fails the user gets one free
            // unlock, which is preferable to charging them for a change that never landed.
            UserCreditDTO updated;
            try {
                updated = userCreditService.debitUserCredit(userCode, cost, OrderTypeEnum.PRIVACY_UNLOCK,
                        "SERVER", generation.getId().toString());
            } catch (Exception e) {
                log.error("privacy unlock debit failed after save, userCode : {} , generationId : {} , cost : {}",
                        userCode, generation.getId(), cost, e);
                return new ImagePrivacyResponse()
                        .setGenerationId(generation.getId().toString())
                        .setPrivateImage(true)
                        .setPrivacyUnlocked(true)
                        .setChargedCredits(0)
                        .setAvailableCredits(available);
            }

            return new ImagePrivacyResponse()
                    .setGenerationId(generation.getId().toString())
                    .setPrivateImage(true)
                    .setPrivacyUnlocked(true)
                    .setChargedCredits(cost)
                    .setAvailableCredits(Objects.nonNull(updated) ? updated.getAvailable() : available - cost);
        } finally {
            locker.unlock(key);
        }
    }

    private Generations findGeneration (ObjectId generationObjectId) {
        Query query = new Query(Criteria.where("_id").is(generationObjectId)
                .and("deleted").is(false));
        return mongoTemplate.findOne(query, Generations.class);
    }

    private boolean isUnlocked (Generations generation) {
        return Boolean.TRUE.equals(generation.getPrivacyUnlocked())
                || Boolean.TRUE.equals(generation.getPrivateImage());
    }

    private Integer currentBalance (String userCode) {
        UserCreditDTO credit = userCreditService.getUserCredit(userCode);
        return Objects.nonNull(credit) ? credit.getAvailable() : 0;
    }
}
```

Note that `applyFree` takes the **requested** value rather than inverting the current one. Deriving it by inversion would break idempotency: two identical requests would flip the image back and forth instead of settling on the requested state. The `reprivatisingAfterAFreeUnlockIsStillFree` and `imageGeneratedPrivateIsTreatedAsAlreadyUnlocked` tests both fail if this is got wrong.

- [ ] **Step 5: Run the test to verify it passes**

```bash
mvn -q test -Dtest=ImagePrivacyServiceImplTest
```

Expected: PASS, 10 tests. If `org.apache.commons.lang3.StringUtils` does not resolve, check the import used elsewhere:

```bash
grep -rn "import org.apache.commons.lang3.StringUtils" src/main/java | head -1
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/av/pixel/service/ImagePrivacyService.java \
        src/main/java/com/av/pixel/service/impl/ImagePrivacyServiceImpl.java \
        src/test/java/com/av/pixel/service/impl/ImagePrivacyServiceImplTest.java
git commit -m "feat: add ImagePrivacyService with one-time paid unlock and free toggling"
```

---

### Task 5: Wire up the endpoint

**Files:**
- Modify: `src/main/java/com/av/pixel/controller/ImagesController.java`

**Interfaces:**
- Consumes: `ImagePrivacyService.updateImagePrivacy` from Task 4.
- Produces: `PUT /api/v1/images/privacy`, the endpoint the app calls in Task 7.

- [ ] **Step 1: Add the dependency and the endpoint**

`ImagesController` uses `@AllArgsConstructor` over its fields. Add a second field and the mapping.

Add these imports:

```java
import com.av.pixel.request.UpdateImagePrivacyRequest;
import com.av.pixel.response.ImagePrivacyResponse;
import com.av.pixel.service.ImagePrivacyService;
```

Add the field next to `GenerationsService imagesService;`:

```java
    ImagePrivacyService imagePrivacyService;
```

Add the endpoint after the existing `PUT /view` method:

```java
    @Authenticated
    @PutMapping("/privacy")
    public ResponseEntity<Response<ImagePrivacyResponse>> updateImagePrivacy (
            UserDTO userDTO,
            @RequestBody UpdateImagePrivacyRequest request) {
        return response(imagePrivacyService.updateImagePrivacy(userDTO, request), HttpStatus.OK);
    }
```

`@Authenticated` without `permissions` matches the other write endpoints and guarantees a non-null `UserDTO`.

- [ ] **Step 2: Verify the application context still wires**

```bash
mvn -q clean compile
```

Expected: BUILD SUCCESS. A missing bean would surface here as a compile error; a genuine wiring problem only shows at boot, which Step 3 covers.

- [ ] **Step 3: Run the full test suite**

```bash
mvn -q test
```

Expected: BUILD SUCCESS, all tests from Tasks 1–4 passing.

- [ ] **Step 4: Manually verify against a running server**

Start the app however you normally do, then, substituting a real bearer token and a generation id **owned by that token's user**:

```bash
curl -i -X PUT http://localhost:8080/api/v1/images/privacy -H "Content-Type: application/json" -H "Authorization: Bearer <TOKEN>" -d '{"generationId":"<OWNED_GENERATION_ID>","privateImage":true}'
```

Expected first call: `200`, body `data.chargedCredits: 50`, `data.privateImage: true`. Expected second identical call: `200`, `data.chargedCredits: 0`. Then check the `transactions` collection holds one `PRIVACY_UNLOCK` row, not two.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/av/pixel/controller/ImagesController.java
git commit -m "feat: add PUT /api/v1/images/privacy endpoint"
```

---

### Task 6: App — parse the new fields

Everything from here happens in `/Users/mohit/Documents/GitHub/PixelAI`, a **separate git repo**. Create a branch there first:

```bash
cd /Users/mohit/Documents/GitHub/PixelAI && git checkout -b feature/paid-image-privacy
```

The app currently has no real tests — `test/widget_test.dart` is the stale Flutter counter template and **will fail if run**. Do not run a bare `flutter test`; always scope to a specific file.

**Files:**
- Modify: `lib/domain/model/response/image_filter_response_model.dart`
- Test: `test/domain/model/generation_privacy_test.dart`

**Interfaces:**
- Consumes: the `privateImage` / `privacyUnlocked` / `privacyUnlockCost` fields added to `GenerationsDTO` in Task 3.
- Produces: `Generation.privateImage` (mutable `bool?`), `Generation.privacyUnlocked` (mutable `bool?`), `Generation.privacyUnlockCost` (`final int?`).

- [ ] **Step 1: Write the failing test**

Create `test/domain/model/generation_privacy_test.dart`:

```dart
import 'package:flutter_test/flutter_test.dart';
import 'package:pixel_ai/domain/model/response/image_filter_response_model.dart';

void main() {
  group('Generation privacy fields', () {
    test('parses privateImage, privacyUnlocked and privacyUnlockCost', () {
      final g = Generation.fromJson({
        'generationId': 'abc',
        'userCode': 'P100',
        'privateImage': true,
        'privacyUnlocked': true,
        'privacyUnlockCost': 50,
      });

      expect(g.privateImage, isTrue);
      expect(g.privacyUnlocked, isTrue);
      expect(g.privacyUnlockCost, 50);
    });

    test('leaves privacy fields null when the server omits them', () {
      final g = Generation.fromJson({'generationId': 'abc', 'userCode': 'P100'});

      expect(g.privateImage, isNull);
      expect(g.privacyUnlocked, isNull);
      expect(g.privacyUnlockCost, isNull);
    });

    test('privateImage and privacyUnlocked are mutable', () {
      final g = Generation.fromJson({
        'generationId': 'abc',
        'privateImage': false,
        'privacyUnlocked': false,
      });

      g.privateImage = true;
      g.privacyUnlocked = true;

      expect(g.privateImage, isTrue);
      expect(g.privacyUnlocked, isTrue);
    });
  });
}
```

The mutability test matters: the details screen and `ImageListingPaginationBloc` share the same `Generation` instance, so an in-place update is what keeps the profile grid in sync without a refetch.

- [ ] **Step 2: Run it to verify it fails**

```bash
cd /Users/mohit/Documents/GitHub/PixelAI && flutter test test/domain/model/generation_privacy_test.dart
```

Expected: compile error — `Generation` has no member `privateImage`.

- [ ] **Step 3: Add the fields**

In `lib/domain/model/response/image_filter_response_model.dart`, in `class Generation`, add alongside the other mutable fields (`likeCount`, `isLikedByMe`):

```dart
  bool? privateImage;
  bool? privacyUnlocked;
  final int? privacyUnlockCost;
```

Add them to the constructor:

```dart
    this.privateImage,
    this.privacyUnlocked,
    this.privacyUnlockCost,
```

Add them to `fromJson`:

```dart
        privateImage: json["privateImage"],
        privacyUnlocked: json["privacyUnlocked"],
        privacyUnlockCost: json["privacyUnlockCost"],
```

And to `toJson`:

```dart
        "privateImage": privateImage,
        "privacyUnlocked": privacyUnlocked,
        "privacyUnlockCost": privacyUnlockCost,
```

- [ ] **Step 4: Run the test to verify it passes**

```bash
flutter test test/domain/model/generation_privacy_test.dart
```

Expected: PASS, 3 tests.

- [ ] **Step 5: Commit**

```bash
git add lib/domain/model/response/image_filter_response_model.dart test/domain/model/generation_privacy_test.dart
git commit -m "feat: parse privacy fields on Generation"
```

---

### Task 7: App — request model, API method, repository method

**Files:**
- Create: `lib/domain/model/request/update_image_privacy_request_model.dart`
- Create: `lib/domain/model/response/image_privacy_response_model.dart`
- Modify: `lib/data/remote/pixel_ai_service.dart`
- Modify: `lib/domain/model/repository.dart`
- Regenerate: `lib/data/remote/pixel_ai_service.g.dart`

**Interfaces:**
- Consumes: `PUT /api/v1/images/privacy` from Task 5.
- Produces: `PixelRepository.updateImagePrivacy(UpdateImagePrivacyRequestModel) : Future<ResponseState<BaseAPIResponse<ImagePrivacyData>>>` for Task 8.

- [ ] **Step 1: Create the request model**

`lib/domain/model/request/update_image_privacy_request_model.dart`:

```dart
class UpdateImagePrivacyRequestModel {
  final String? generationId;
  final bool? privateImage;

  UpdateImagePrivacyRequestModel({
    this.generationId,
    this.privateImage,
  });

  Map<String, dynamic> toJson() => {
        'generationId': generationId,
        'privateImage': privateImage,
      };
}
```

This matches the hand-written `toJson` style of `video_effect_request_model.dart` rather than the codegen style, so no build_runner pass is needed for it.

- [ ] **Step 2: Create the response model**

`lib/domain/model/response/image_privacy_response_model.dart`:

```dart
class ImagePrivacyData {
  final String? generationId;
  final bool? privateImage;
  final bool? privacyUnlocked;
  final int? chargedCredits;
  final int? availableCredits;

  ImagePrivacyData({
    this.generationId,
    this.privateImage,
    this.privacyUnlocked,
    this.chargedCredits,
    this.availableCredits,
  });

  factory ImagePrivacyData.fromJson(Map<String, dynamic> json) =>
      ImagePrivacyData(
        generationId: json['generationId'],
        privateImage: json['privateImage'],
        privacyUnlocked: json['privacyUnlocked'],
        chargedCredits: json['chargedCredits'],
        availableCredits: json['availableCredits'],
      );

  Map<String, dynamic> toJson() => {
        'generationId': generationId,
        'privateImage': privateImage,
        'privacyUnlocked': privacyUnlocked,
        'chargedCredits': chargedCredits,
        'availableCredits': availableCredits,
      };
}
```

- [ ] **Step 3: Add the Retrofit method**

In `lib/data/remote/pixel_ai_service.dart`, add the imports:

```dart
import '../../domain/model/request/update_image_privacy_request_model.dart';
import '../../domain/model/response/image_privacy_response_model.dart';
```

and the method next to the existing `imageAction`:

```dart
  @PUT("/api/v1/images/privacy")
  Future<HttpResponse<BaseAPIResponse<ImagePrivacyData>>> updateImagePrivacy(
      @Body() UpdateImagePrivacyRequestModel model);
```

- [ ] **Step 4: Regenerate the Retrofit client**

```bash
dart run build_runner build
```

**Do not pass `--delete-conflicting-outputs` in this repo.** `json_serializable` is not a dev dependency, so `lib/core/base_response.g.dart` is checked in but not regenerable — that flag deletes it and never brings it back, breaking every build. If it does get deleted, restore it with `git checkout -- lib/core/base_response.g.dart`.

Expected: `pixel_ai_service.g.dart` regenerates with a `updateImagePrivacy` implementation. If the generator complains it cannot deserialize `ImagePrivacyData`, confirm the `fromJson` factory signature matches exactly `Map<String, dynamic>` — `Parser.JsonSerializable` requires it.

- [ ] **Step 5: Add the repository method**

In `lib/domain/model/repository.dart`, add the same two imports, then, next to `reportImage`:

```dart
  Future<ResponseState<BaseAPIResponse<ImagePrivacyData>>> updateImagePrivacy(
      UpdateImagePrivacyRequestModel model) async {
    return handleResponse<BaseAPIResponse<ImagePrivacyData>>(
      () => _pixelAiService.updateImagePrivacy(model),
    );
  }
```

- [ ] **Step 6: Verify it analyses clean**

```bash
flutter analyze lib/data/remote/pixel_ai_service.dart lib/domain/model/repository.dart lib/domain/model/request/update_image_privacy_request_model.dart lib/domain/model/response/image_privacy_response_model.dart
```

Expected: No issues found.

- [ ] **Step 7: Commit**

```bash
git add lib/domain/model/request/update_image_privacy_request_model.dart \
        lib/domain/model/response/image_privacy_response_model.dart \
        lib/data/remote/pixel_ai_service.dart \
        lib/data/remote/pixel_ai_service.g.dart \
        lib/domain/model/repository.dart
git commit -m "feat: add updateImagePrivacy API and repository method"
```

---

### Task 8: App — ImagePrivacyCubit

**Files:**
- Modify: `pubspec.yaml` (dev_dependencies)
- Create: `lib/presentation/bloc/image_privacy_cubit/image_privacy_state.dart`
- Create: `lib/presentation/bloc/image_privacy_cubit/image_privacy_cubit.dart`
- Test: `test/presentation/bloc/image_privacy_cubit_test.dart`

**Interfaces:**
- Consumes: `PixelRepository.updateImagePrivacy` from Task 7.
- Produces: `ImagePrivacyCubit(PixelRepository)` with `setPrivacy({required String generationId, required bool privateImage})`, emitting `ImagePrivacyLoading`, `ImagePrivacySuccess(privateImage, chargedCredits, availableCredits)`, `ImagePrivacyFailure(message, showBuyFlow)` — consumed by the UI in Task 9.

- [ ] **Step 1: Add test dependencies**

The app has no mocking or bloc-testing packages. In `pubspec.yaml` under `dev_dependencies`, add:

```yaml
  bloc_test: ^9.1.7
  mocktail: ^1.0.4
```

Then:

```bash
flutter pub get
```

- [ ] **Step 2: Write the failing test**

Create `test/presentation/bloc/image_privacy_cubit_test.dart`:

```dart
import 'package:bloc_test/bloc_test.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:pixel_ai/core/base_response.dart';
import 'package:pixel_ai/core/data_state.dart';
import 'package:pixel_ai/core/exceptions.dart';
import 'package:pixel_ai/domain/model/repository.dart';
import 'package:pixel_ai/domain/model/request/update_image_privacy_request_model.dart';
import 'package:pixel_ai/domain/model/response/image_privacy_response_model.dart';
import 'package:pixel_ai/presentation/bloc/image_privacy_cubit/image_privacy_cubit.dart';
import 'package:pixel_ai/presentation/bloc/image_privacy_cubit/image_privacy_state.dart';

class MockPixelRepository extends Mock implements PixelRepository {}

class FakeRequest extends Fake implements UpdateImagePrivacyRequestModel {}

void main() {
  late MockPixelRepository repository;

  setUpAll(() => registerFallbackValue(FakeRequest()));

  setUp(() => repository = MockPixelRepository());

  SuccessResponse<BaseAPIResponse<ImagePrivacyData>> success({
    required bool privateImage,
    required int charged,
  }) =>
      SuccessResponse(
        BaseAPIResponse<ImagePrivacyData>(
          data: ImagePrivacyData(
            generationId: 'abc',
            privateImage: privateImage,
            privacyUnlocked: true,
            chargedCredits: charged,
            availableCredits: 450,
          ),
        ),
        statusCode: 200,
      );

  FailedResponse<BaseAPIResponse<ImagePrivacyData>> failure(int status, String display) =>
      FailedResponse(
        ApiException.from(
          DioException(
            requestOptions: RequestOptions(path: '/api/v1/images/privacy'),
            response: Response(
              requestOptions: RequestOptions(path: '/api/v1/images/privacy'),
              statusCode: status,
              data: {'displayMessage': display},
            ),
          ),
        ),
      );

  blocTest<ImagePrivacyCubit, ImagePrivacyState>(
    'emits loading then success with the charged amount',
    build: () {
      when(() => repository.updateImagePrivacy(any()))
          .thenAnswer((_) async => success(privateImage: true, charged: 50));
      return ImagePrivacyCubit(repository);
    },
    act: (c) => c.setPrivacy(generationId: 'abc', privateImage: true),
    expect: () => [
      isA<ImagePrivacyLoading>(),
      isA<ImagePrivacySuccess>()
          .having((s) => s.privateImage, 'privateImage', true)
          .having((s) => s.chargedCredits, 'chargedCredits', 50)
          .having((s) => s.availableCredits, 'availableCredits', 450),
    ],
  );

  blocTest<ImagePrivacyCubit, ImagePrivacyState>(
    'sets showBuyFlow on a 402',
    build: () {
      when(() => repository.updateImagePrivacy(any()))
          .thenAnswer((_) async => failure(402, 'Not enough credits'));
      return ImagePrivacyCubit(repository);
    },
    act: (c) => c.setPrivacy(generationId: 'abc', privateImage: true),
    expect: () => [
      isA<ImagePrivacyLoading>(),
      isA<ImagePrivacyFailure>()
          .having((s) => s.showBuyFlow, 'showBuyFlow', true)
          .having((s) => s.message, 'message', 'Not enough credits'),
    ],
  );

  blocTest<ImagePrivacyCubit, ImagePrivacyState>(
    'does not set showBuyFlow on a non-402 failure',
    build: () {
      when(() => repository.updateImagePrivacy(any()))
          .thenAnswer((_) async => failure(403, 'Not allowed'));
      return ImagePrivacyCubit(repository);
    },
    act: (c) => c.setPrivacy(generationId: 'abc', privateImage: true),
    expect: () => [
      isA<ImagePrivacyLoading>(),
      isA<ImagePrivacyFailure>().having((s) => s.showBuyFlow, 'showBuyFlow', false),
    ],
  );
}
```

Before running, confirm the exact constructor shapes of `SuccessResponse`, `FailedResponse` and `ApiException.from`:

```bash
cat lib/core/data_state.dart lib/core/exceptions.dart
```

Adjust the two helper builders to match — the rest of the test is unaffected.

- [ ] **Step 3: Run it to verify it fails**

```bash
flutter test test/presentation/bloc/image_privacy_cubit_test.dart
```

Expected: compile error — `image_privacy_cubit.dart` does not exist.

- [ ] **Step 4: Write the state**

`lib/presentation/bloc/image_privacy_cubit/image_privacy_state.dart`:

```dart
import 'package:equatable/equatable.dart';

abstract class ImagePrivacyState extends Equatable {
  const ImagePrivacyState();

  @override
  List<Object?> get props => [];
}

class ImagePrivacyInitial extends ImagePrivacyState {
  const ImagePrivacyInitial();
}

class ImagePrivacyLoading extends ImagePrivacyState {
  const ImagePrivacyLoading();
}

class ImagePrivacySuccess extends ImagePrivacyState {
  final bool privateImage;
  final int chargedCredits;
  final int? availableCredits;

  const ImagePrivacySuccess({
    required this.privateImage,
    required this.chargedCredits,
    this.availableCredits,
  });

  @override
  List<Object?> get props => [privateImage, chargedCredits, availableCredits];
}

class ImagePrivacyFailure extends ImagePrivacyState {
  final String message;
  final bool showBuyFlow;

  const ImagePrivacyFailure({required this.message, this.showBuyFlow = false});

  @override
  List<Object?> get props => [message, showBuyFlow];
}
```

Confirm `equatable` is already a dependency (`grep -n "equatable" pubspec.yaml`); if it is not, drop the `Equatable` base class and the `props` overrides — `blocTest`'s `isA<>().having()` matchers do not need value equality.

- [ ] **Step 5: Write the cubit**

`lib/presentation/bloc/image_privacy_cubit/image_privacy_cubit.dart`:

```dart
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:pixel_ai/core/data_state.dart';
import 'package:pixel_ai/domain/model/repository.dart';
import 'package:pixel_ai/domain/model/request/update_image_privacy_request_model.dart';

import 'image_privacy_state.dart';

class ImagePrivacyCubit extends Cubit<ImagePrivacyState> {
  final PixelRepository _repository;

  ImagePrivacyCubit(this._repository) : super(const ImagePrivacyInitial());

  Future<void> setPrivacy({
    required String generationId,
    required bool privateImage,
  }) async {
    emit(const ImagePrivacyLoading());

    final response = await _repository.updateImagePrivacy(
      UpdateImagePrivacyRequestModel(
        generationId: generationId,
        privateImage: privateImage,
      ),
    );

    response.when(
      onSuccess: (success) {
        final data = success.response?.data;
        if (data == null) {
          emit(const ImagePrivacyFailure(message: 'Could not update visibility.'));
          return;
        }
        emit(ImagePrivacySuccess(
          privateImage: data.privateImage ?? privateImage,
          chargedCredits: data.chargedCredits ?? 0,
          availableCredits: data.availableCredits,
        ));
      },
      onFailed: (failed) {
        final statusCode = failed.exception.response?.statusCode;
        String message = 'Could not update visibility.';
        try {
          message = ((failed.exception.response?.data as Map)['displayMessage']).toString();
        } catch (_) {}
        emit(ImagePrivacyFailure(message: message, showBuyFlow: statusCode == 402));
      },
    );
  }
}
```

The `response.when(onSuccess:, onFailed:)` shape and the `displayMessage` extraction are copied from `GenerateImageBloc`. Verify the exact `when` signature in `lib/core/data_state.dart` and match it.

- [ ] **Step 6: Run the test to verify it passes**

```bash
flutter test test/presentation/bloc/image_privacy_cubit_test.dart
```

Expected: PASS, 3 tests.

- [ ] **Step 7: Commit**

```bash
git add pubspec.yaml pubspec.lock \
        lib/presentation/bloc/image_privacy_cubit/ \
        test/presentation/bloc/image_privacy_cubit_test.dart
git commit -m "feat: add ImagePrivacyCubit"
```

---

### Task 9: App — the owner-only control on the image details screen

**Files:**
- Modify: `lib/presentation/view/image_detail_view_screen.dart` (`ImageDetailUserInfoWidget`, around line 1254)

**Interfaces:**
- Consumes: `ImagePrivacyCubit` and its states (Task 8), `Generation.privateImage` / `privacyUnlocked` / `privacyUnlockCost` (Task 6).
- Produces: the user-facing feature. Nothing depends on it.

- [ ] **Step 1: Provide the cubit at the screen root**

`ImageDetailUserInfoWidget` is a separate widget from `_ImageDetailViewScreenState`, so the cubit must be provided above both. In `_ImageDetailViewScreenState.build`, wrap the returned widget:

```dart
return BlocProvider(
  create: (_) => ImagePrivacyCubit(locator<PixelRepository>()),
  child: <the existing returned widget>,
);
```

Add the imports:

```dart
import '../bloc/image_privacy_cubit/image_privacy_cubit.dart';
import '../bloc/image_privacy_cubit/image_privacy_state.dart';
```

`locator` and `PixelRepository` are already imported in this file (used by `reportImage`).

- [ ] **Step 2: Convert `ImageDetailUserInfoWidget` to a StatefulWidget**

It is currently a `StatelessWidget`. It needs local state to show the pending switch position while a request is in flight. Change the declaration to:

```dart
class ImageDetailUserInfoWidget extends StatefulWidget {
  final Generation imageDetail;
  final ValueNotifier<int> selectedImage;

  const ImageDetailUserInfoWidget({
    super.key,
    required this.imageDetail,
    required this.selectedImage,
  });

  @override
  State<ImageDetailUserInfoWidget> createState() => _ImageDetailUserInfoWidgetState();
}

class _ImageDetailUserInfoWidgetState extends State<ImageDetailUserInfoWidget> {
  bool _updating = false;
```

Then prefix every existing `imageDetail` / `selectedImage` reference in the moved `build` and helper methods with `widget.`.

- [ ] **Step 3: Add the control to the Row**

Inside the `Row`'s `children`, after the Download button and before the Block-user button, add:

```dart
          if (BlocProvider.of<LoginBloc>(context).loginResponseModel?.user?.code ==
              widget.imageDetail.userCode)
            _buildPrivacyControl(context),
```

- [ ] **Step 4: Build the control**

Add to `_ImageDetailUserInfoWidgetState`:

```dart
  bool get _isUnlocked =>
      widget.imageDetail.privacyUnlocked == true ||
      widget.imageDetail.privateImage == true;

  Widget _buildPrivacyControl(BuildContext context) {
    if (_updating) {
      return const SizedBox(
        width: 32,
        height: 32,
        child: Center(
          child: SizedBox(
            width: 16,
            height: 16,
            child: CircularProgressIndicator(strokeWidth: 2),
          ),
        ),
      );
    }

    if (_isUnlocked) {
      return Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text("Private",
              style: MyTheme.textStyleRegular12
                  .copyWith(color: MyColors.color444444)),
          Switch(
            value: widget.imageDetail.privateImage == true,
            onChanged: (value) => _submit(context, value),
          ),
        ],
      );
    }

    final cost = widget.imageDetail.privacyUnlockCost;
    return MyFlatButton(
      onTap: () => _confirmUnlock(context, cost),
      backgroundColor: MyColors.colorEBEDF1,
      height: null,
      padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 8),
      title: cost == null ? "Make private" : "Make private · $cost ✦",
      style: MyTheme.textStyleBold8.copyWith(color: MyColors.color444444),
      suffixIcon: const Icon(Icons.lock_outline,
          color: MyColors.color444444, size: 14),
      borderRadius: 16,
    );
  }
```

The `MyFlatButton` parameters mirror the Block-user button directly below it, so the two match visually.

- [ ] **Step 5: Add the confirmation dialog**

```dart
  void _confirmUnlock(BuildContext context, int? cost) {
    showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text("Make this image private?", style: MyTheme.textStyleMedium14),
        content: Text(
          cost == null
              ? "You'll be able to switch it between public and private anytime after, free."
              : "This costs $cost ✦ once. After that you can switch it between "
                  "public and private anytime, free.",
          style: MyTheme.textStyleRegular12,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: Text("Cancel", style: MyTheme.textStyleMedium14),
          ),
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            child: Text("Confirm", style: MyTheme.textStyleMedium14),
          ),
        ],
      ),
    ).then((confirmed) {
      if (confirmed == true && context.mounted) {
        _submit(context, true);
      }
    });
  }
```

- [ ] **Step 6: Submit and handle the result**

```dart
  void _submit(BuildContext context, bool privateImage) {
    final generationId = widget.imageDetail.id;
    if (generationId == null) return;

    setState(() => _updating = true);
    BlocProvider.of<ImagePrivacyCubit>(context)
        .setPrivacy(generationId: generationId, privateImage: privateImage);
  }
```

Wrap the widget returned by `build` in a listener so the result lands back here:

```dart
    return BlocListener<ImagePrivacyCubit, ImagePrivacyState>(
      listener: (context, state) {
        if (state is ImagePrivacySuccess) {
          setState(() {
            _updating = false;
            widget.imageDetail.privateImage = state.privateImage;
            widget.imageDetail.privacyUnlocked = true;
          });
          MyFlushbarHelper.showSuccessSnackbar(
            context,
            state.privateImage ? "Image is now private" : "Image is now public",
          );
        } else if (state is ImagePrivacyFailure) {
          setState(() => _updating = false);
          MyFlushbarHelper.showFailureSnackbar(context, state.message);
          if (state.showBuyFlow) {
            Navigator.pushNamed(context, RouteName.inAppPurchase);
          }
        }
      },
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16.0),
        child: Row(
          // ...existing children...
        ),
      ),
    );
```

Mutating `widget.imageDetail` in place is intentional: `ImageListingPaginationBloc` holds the same `Generation` object, so the profile grid picks up the change with no refetch.

- [ ] **Step 7: Verify it analyses clean**

```bash
flutter analyze lib/presentation/view/image_detail_view_screen.dart
```

Expected: No issues found. Anything about `imageDetail` being undefined means a `widget.` prefix was missed in Step 2.

- [ ] **Step 8: Run the app and verify by hand**

```bash
flutter run
```

Against the Task 5 backend, check all of:
1. On someone else's image: no privacy control at all.
2. On your own public, never-private image: a `Make private · 50 ✦` button.
3. Tap it → dialog appears. Cancel → nothing charged, button unchanged.
4. Tap again → Confirm → spinner, then a `Private` switch in the on position, success snackbar, 50 tokens gone.
5. Flip the switch off → image goes public, **no charge**.
6. Flip it back on → still no charge.
7. Reopen the image from the feed → it still shows the switch, not the button.
8. On an account with under 50 tokens: Confirm → failure snackbar reading "Not enough credits", then the purchase screen opens.
9. On a video-effect generation you own: the same control appears and works.

- [ ] **Step 9: Commit**

```bash
git add lib/presentation/view/image_detail_view_screen.dart
git commit -m "feat: add owner-only privacy control to image details screen"
```

---

## Self-review notes

Spec coverage checked section by section:

| Spec item | Task |
|---|---|
| `Generations.privacyUnlocked` | 2 |
| `AdminConfig.privacyUnlockCost` + `DEFAULT_PRIVACY_UNLOCK_COST` fallback | 2 |
| `OrderTypeEnum.PRIVACY_UNLOCK` | 2 |
| `UpdateImagePrivacyRequest` / `ImagePrivacyResponse` | 3 |
| `GenerationsDTO` exposure + mapper | 3 |
| Service logic steps 1–6, write-order tradeoff, lock | 4 |
| `PUT /api/v1/images/privacy` | 5 |
| App model fields | 6 |
| App request/API/repository | 7 |
| `ImagePrivacyCubit` incl. 402 handling | 8 |
| Owner-only button/switch, confirmation dialog | 9 |
| All 8 backend test cases from the spec | 4 (plus 404/400/409, added) |
| All 3 app test cases from the spec | 6 and 8 |

Two things the spec assumed that this plan had to add: the backend has no test framework at all (Task 1), and the app has no mocking/bloc-test packages (Task 8, Step 1). Both are prerequisites rather than scope creep.
