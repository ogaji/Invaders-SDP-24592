package screen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class ScreenRunTest {
    @ParameterizedTest
    @EnumSource(Destination.class)
    void returnsTheDestinationChosenByTheScreen(Destination destination) {
        Screen screen = new Screen(448, 520, 60) {
            @Override
            protected void update() {
                nextDestination = destination;
                isRunning = false;
            }
        };

        assertSame(destination, screen.run());
    }

    @Test
    void interruptionPreservesTheConfiguredMenuDestination() {
        Screen screen = new Screen(448, 520, 60) {
            @Override
            protected void update() {
                nextDestination = Destination.SETTINGS;
                Thread.currentThread().interrupt();
            }
        };

        try {
            assertSame(Destination.SETTINGS, screen.run());
            assertFalse(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
