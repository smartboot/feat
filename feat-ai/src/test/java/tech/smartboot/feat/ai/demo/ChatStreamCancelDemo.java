package tech.smartboot.feat.ai.demo;

import tech.smartboot.feat.ai.chat.ChatModel;
import tech.smartboot.feat.ai.chat.ChatOptions;
import tech.smartboot.feat.ai.chat.ChatStreamListener;
import tech.smartboot.feat.ai.chat.entity.ChatResponse;
import tech.smartboot.feat.core.common.exception.FeatException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 验证 chatStream 返回的 future 的终态语义：
 * <ol>
 *     <li>happy：服务端正常发 [DONE] 后断连，future 以完整响应完成，onCompletion 触发</li>
 *     <li>abrupt：服务端不发 [DONE] 直接断连，future 以已累积内容补齐完成，onCompletion 触发（不悬挂）</li>
 *     <li>unauthorized：服务端返回 401，future 异常完成，onError 恰好触发一次</li>
 * </ol>
 * 需配合 mock SSE 服务运行（见验证脚本）。
 */
public class ChatStreamCancelDemo {
    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !args[0].startsWith("http")) {
            System.err.println("usage: ChatStreamCancelDemo <baseUrl> <happy|abrupt|unauthorized>");
            System.exit(1);
        }
        String mode = args[1];
        CountDownLatch firstChunk = new CountDownLatch(1);
        CountDownLatch completion = new CountDownLatch(1);
        CountDownLatch error = new CountDownLatch(1);
        AtomicInteger errorCount = new AtomicInteger();
        StringBuilder errorMessages = new StringBuilder();
        StringBuilder received = new StringBuilder();

        ChatModel chatModel = new ChatModel(new ChatOptions().baseUrl(args[0]).model("test-model"));
        CompletableFuture<ChatResponse> future = chatModel.chatStream("hello", new ChatStreamListener() {
            @Override
            public void onStreamResponse(String content) {
                received.append(content);
                firstChunk.countDown();
            }

            @Override
            public void onCompletion(ChatResponse chatResponse) {
                completion.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                errorCount.incrementAndGet();
                errorMessages.append("[").append(throwable.getMessage()).append("] ");
                error.countDown();
            }
        });

        if (!mode.equals("unauthorized") && !firstChunk.await(5, TimeUnit.SECONDS)) {
            System.err.println("FAIL [" + mode + "]: no stream chunk received");
            System.exit(1);
        }

        switch (mode) {
            case "happy":
            case "abrupt": {
                ChatResponse response = future.get(5, TimeUnit.SECONDS);
                boolean ok = response.getContent().contentEquals(received) && response.isSuccess()
                        && completion.await(1, TimeUnit.SECONDS) && errorCount.get() == 0;
                System.out.println((ok ? "PASS" : "FAIL") + " [" + mode + "]: content=" + response.getContent()
                        + ", onCompletion=" + (completion.getCount() == 0) + ", onErrorCount=" + errorCount.get());
                System.exit(ok ? 0 : 1);
            }
            case "unauthorized": {
                boolean errorFired = error.await(5, TimeUnit.SECONDS);
                Throwable cause = null;
                boolean exceptional = false;
                try {
                    future.get(2, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    exceptional = true;
                    cause = e.getCause();
                }
                // onError 恰好一次、future 异常完成且异常为 FeatException
                boolean ok = errorFired && errorCount.get() == 1 && exceptional && cause instanceof FeatException;
                System.out.println((ok ? "PASS" : "FAIL") + " [unauthorized]: onErrorCount=" + errorCount.get()
                        + ", exceptional=" + exceptional + ", cause=" + (cause == null ? "null" : cause.getClass().getSimpleName())
                        + ", messages=" + errorMessages);
                System.exit(ok ? 0 : 1);
            }
            default:
                System.err.println("unknown mode: " + mode);
                System.exit(1);
        }
    }
}
