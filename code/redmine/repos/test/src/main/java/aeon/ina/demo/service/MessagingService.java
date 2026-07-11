package aeon.ina.demo.service;

public class MessagingService {

    public String buildPingMessage(String name) {
        if (name == null || name.isBlank()) {
            return "Pong from Demo Plugin!";
        }
        return "Pong, " + name + "!";
    }
}
