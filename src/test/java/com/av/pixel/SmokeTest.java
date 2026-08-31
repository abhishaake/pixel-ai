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
