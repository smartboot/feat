package tech.smartboot.feat.common.utils;

import tech.smartboot.feat.common.logging.Logger;
import tech.smartboot.feat.common.logging.LoggerFactory;

import java.io.File;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileReleaseTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReleaseTracker.class);

    private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
    private final Set<FileTracker> trackers = new HashSet<>();
    private final Thread reaperThread;
    private volatile boolean running = true;

    public FileReleaseTracker() {
        reaperThread = new ReaperThread();
        // 设置为守护线程
        reaperThread.setDaemon(true);
        reaperThread.start();
    }

    /**
     * 跟踪文件和标识对象
     * @param file 文件
     * @param marker 处理器
     */
    public void track(File file, Object marker) {
        if (file == null || marker == null) {
            throw new IllegalArgumentException("File and marker cannot be null.");
        }
        // 创建并添加一个FileTracker到trackers集合
        FileTracker tracker = new FileTracker(file, marker, referenceQueue);
        synchronized (trackers) {
            trackers.add(tracker);
        }
    }

    /**
     * 停止追踪，退出清理线程
     */
    public void stop() {
        running = false;
        reaperThread.interrupt(); // 中断ReaperThread
    }

    /**
     * 返回 tracker 是否仍然在运行
     */
    public boolean isRunning() {
        return running;
    }


    private class ReaperThread extends Thread {
        @Override
        public void run() {
            while (running) {
                try {
                    // 从referenceQueue中移除被垃圾回收的FileTracker
                    FileTracker tracker = (FileTracker) referenceQueue.remove();
                    synchronized (trackers) {
                        trackers.remove(tracker);
                    }
                    tracker.deleteFile(); // 删除文件
                } catch (InterruptedException e) {
                    // 中断时退出循环
                    if (!running) {
                        break;
                    }
                }
            }
        }
    }

    private static class FileTracker extends PhantomReference<Object> {
        private final File file;
        private final AtomicBoolean deleteAttempted = new AtomicBoolean(false);

        FileTracker(File file, Object marker, ReferenceQueue<? super Object> queue) {
            super(marker, queue);
            this.file = file;
        }


        public void deleteFile() {
            if (deleteAttempted.compareAndSet(false, true)) {
                if (file.exists()) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        LOGGER.info("成功删除文件: " + file.getAbsolutePath());
                    } else {
                        LOGGER.error("删除文件失败: " + file.getAbsolutePath());
                    }
                }
            }
        }

    }
}
