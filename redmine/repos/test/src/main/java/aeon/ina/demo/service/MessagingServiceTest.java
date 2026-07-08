package aeon.ina.demo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessagingServiceTest {

    @Test
    void returnsNamedMessage() {
        MessagingService service = new MessagingService();
        assertEquals("Pong, Ina!", service.buildPingMessage("Ina"));
    }

    @Test
    void returnsDefaultMessageForBlankName() {
        MessagingService service = new MessagingService();
        assertEquals("Pong from DemoPlugin!", service.buildPingMessage("   "));
    }

    @Test
    void returnsDefaultMessageForNullName() {
        MessagingService service = new MessagingService();
        assertEquals("Pong from DemoPlugin!", service.buildPingMessage(null));
    }
}
