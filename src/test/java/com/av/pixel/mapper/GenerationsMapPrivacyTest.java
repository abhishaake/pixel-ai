package com.av.pixel.mapper;

import com.av.pixel.dao.Generations;
import com.av.pixel.dto.GenerationsDTO;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GenerationsMapPrivacyTest {

    private Generations generation() {
        Generations g = new Generations()
                .setUserCode("P100")
                .setModel("ideogram")
                .setRenderOption("TURBO")
                .setStyle("AUTO");
        g.setId(new ObjectId());
        return g;
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
