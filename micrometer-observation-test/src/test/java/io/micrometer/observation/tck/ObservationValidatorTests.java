/*
 * Copyright 2024 VMware, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.observation.tck;

import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Event;
import io.micrometer.observation.Observation.Scope;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ObservationValidator}.
 *
 * @author Jonatan Ivanov
 */
class ObservationValidatorTests {

    private final ObservationRegistry registry = TestObservationRegistry.create();

    @Test
    void doubleStartShouldBeInvalid() {
        assertThatThrownBy(() -> Observation.start("test", registry).start())
            .isExactlyInstanceOf(InvalidObservationException.class)
            .hasNoCause()
            .hasMessage("Invalid start: Observation has already been started");
    }

    @Test
    void stopBeforeStartShouldBeInvalid() {
        assertThatThrownBy(() -> Observation.createNotStarted("test", registry).stop())
            .isExactlyInstanceOf(InvalidObservationException.class)
            .hasNoCause()
            .hasMessage("Invalid stop: Observation has not been started yet");
    }

    @Test
    void errorBeforeStartShouldBeInvalid() {
        assertThatThrownBy(() -> Observation.createNotStarted("test", registry).error(new RuntimeException()))
            .isExactlyInstanceOf(InvalidObservationException.class)
            .hasNoCause()
            .hasMessage("Invalid error signal: Observation has not been started yet");
    }

    @Test
    void eventBeforeStartShouldBeInvalid() {
        assertThatThrownBy(() -> Observation.createNotStarted("test", registry).event(Event.of("test")))
            .isExactlyInstanceOf(InvalidObservationException.class)
            .hasNoCause()
            .hasMessage("Invalid event signal: Observation has not been started yet");
    }

    @Test
    @SuppressWarnings("resource")
    void scopeBeforeStartShouldBeInvalid() {
        // Since openScope throws an exception, reset and close can't happen
        assertThatThrownBy(() -> Observation.createNotStarted("test", registry).openScope())
            .isExactlyInstanceOf(InvalidObservationException.class)
            .hasNoCause()
            .hasMessage("Invalid scope opening: Observation has not been started yet");
    }

    @Test
    void observeAfterStartShouldBeInvalid() {
        assertThatThrownBy(() -> Observation.start("test", registry).observe(() -> ""))
            .isExactlyInstanceOf(InvalidObservationException.class)
            .hasNoCause()
            .hasMessage("Invalid start: Observation has already been started");
    }

    @Test
    void doubleStopShouldBeInvalid() {
        assertThatThrownBy(() -> {
            Observation observation = Observation.start("test", registry);
            observation.stop();
            observation.stop();
        }).isExactlyInstanceOf(InvalidObservationException.class)
            .hasNoCause()
            .hasMessage("Invalid stop: Observation has already been stopped");
    }

    @Test
    void errorAfterStopShouldBeInvalid() {
        assertThatThrownBy(() -> {
            Observation observation = Observation.start("test", registry);
            observation.stop();
            observation.error(new RuntimeException());
        }).isExactlyInstanceOf(InvalidObservationException.class)
            .hasNoCause()
            .hasMessage("Invalid error signal: Observation has already been stopped");
    }

    @Test
    void eventAfterStopShouldBeInvalid() {
        assertThatThrownBy(() -> {
            Observation observation = Observation.start("test", registry);
            observation.stop();
            observation.event(Event.of("test"));
        }).isExactlyInstanceOf(InvalidObservationException.class)
            .hasNoCause()
            .hasMessage("Invalid event signal: Observation has already been stopped");
    }

    @Test
    @SuppressWarnings("resource")
    void scopeOpenAfterStopShouldBeInvalid() {
        assertThatThrownBy(() -> {
            Observation observation = Observation.start("test", registry);
            observation.stop();
            observation.openScope();
        }).isExactlyInstanceOf(InvalidObservationException.class)
            .hasNoCause()
            .hasMessage("Invalid scope opening: Observation has already been stopped");
    }

    @Test
    @SuppressWarnings("resource")
    void scopeResetAfterStopShouldBeInvalid() {
        assertThatThrownBy(() -> {
            Observation observation = Observation.start("test", registry);
            Scope scope = observation.openScope();
            observation.stop();
            scope.reset();
        }).isExactlyInstanceOf(InvalidObservationException.class)
            .hasNoCause()
            .hasMessage("Invalid scope resetting: Observation has already been stopped");
    }

    @Test
    void scopeCloseAfterStopShouldBeInvalid() {
        assertThatThrownBy(() -> {
            Observation observation = Observation.start("test", registry);
            Scope scope = observation.openScope();
            observation.stop();
            scope.close();
        }).isExactlyInstanceOf(InvalidObservationException.class)
            .hasNoCause()
            .hasMessage("Invalid scope closing: Observation has already been stopped");
    }

    @Test
    void startEventStopShouldBeValid() {
        Observation.start("test", registry).event(Event.of("test")).stop();
    }

    @Test
    void startEventErrorStopShouldBeValid() {
        Observation.start("test", registry).event(Event.of("test")).error(new RuntimeException()).stop();
    }

    @Test
    void startErrorEventStopShouldBeValid() {
        Observation.start("test", registry).error(new RuntimeException()).event(Event.of("test")).stop();
    }

    @Test
    void startScopeEventStopShouldBeValid() {
        Observation observation = Observation.start("test", registry);
        observation.openScope().close();
        observation.event(Event.of("test"));
        observation.stop();
    }

    @Test
    void startScopeEventErrorStopShouldBeValid() {
        Observation observation = Observation.start("test", registry);
        Scope scope = observation.openScope();
        observation.event(Event.of("test"));
        observation.error(new RuntimeException());
        scope.close();
        observation.stop();
    }

    @Test
    void startScopeErrorEventStopShouldBeValid() {
        Observation observation = Observation.start("test", registry);
        Scope scope = observation.openScope();
        observation.error(new RuntimeException());
        observation.event(Event.of("test"));
        scope.close();
        observation.stop();
    }

}
