package ai.soc.controller;

import ai.soc.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @PostMapping("/query")
    public Map<String, Object> getChatbotResponse(@RequestBody Map<String, String> request) {
        String userQuery = request.get("query");
        return chatbotService.processQuery(userQuery);
    }
}