package org.example.learn.spring.boot.sse.controller;

import org.example.learn.spring.boot.sse.constant.MessageConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequestMapping("/message")
@RestController
public class MessageController {

    public static final Logger logger = LoggerFactory.getLogger(MessageController.class);

    private static final Map<String, SseEmitter> sseCache = new ConcurrentHashMap<>();

    @GetMapping(path = "/subscribe", produces = {MediaType.TEXT_EVENT_STREAM_VALUE})
    public SseEmitter push(String id) throws IOException {
        // 超时时间设置为3s，用于演示客户端自动重连
        SseEmitter sseEmitter = new SseEmitter(MessageConstants.TIMEOUT);
        // 设置前端的重试时间为1s
        sseEmitter.send(SseEmitter.event().reconnectTime(MessageConstants.TIMEOUT).data("连接成功"));
        sseCache.put(id, sseEmitter);
        logger.info("sse subscribe. id={}", id);

        sseEmitter.onTimeout(() -> {
            sseCache.remove(id);
            logger.info("sse timeout. id={}", id);
        });

        sseEmitter.onCompletion(() -> {
            logger.info("sse complete. id={}", id);
        });

        return sseEmitter;
    }

    @GetMapping(path = "/publish")
    public String publish(String id, String content) throws IOException {
        logger.info("publish id={} content={}", id, content);

        SseEmitter sseEmitter = sseCache.get(id);
        if (sseEmitter != null) {
            sseEmitter.send(SseEmitter.event().name("msg").data("后端发送消息：" + content));
        }

        return "over";
    }

    @GetMapping(path = "/over")
    public String over(String id) {
        logger.info("over id={}", id);

        SseEmitter sseEmitter = sseCache.get(id);
        if (sseEmitter != null) {
            sseEmitter.complete();
            sseCache.remove(id);
        }

        return "over";
    }
}
