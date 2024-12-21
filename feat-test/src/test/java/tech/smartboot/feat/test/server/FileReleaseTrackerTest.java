package tech.smartboot.feat.test.server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tech.smartboot.feat.core.common.utils.FileReleaseTracker;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class FileReleaseTrackerTest {

    private FileReleaseTracker tracker;
    private File testFile;

    @Before
    public void init() throws IOException {
        // 初始化 FileReleaseTracker 实例
        tracker = new FileReleaseTracker();
        
        // 创建一个临时文件用于测试
        testFile = File.createTempFile("testFile", ".txt");
    }

    @After
    public void tearDown() {
        // 停止 FileReleaseTracker 的运行
        tracker.stop();
        
        // 如果测试文件仍然存在，进行删除清理
        if (testFile.exists()) {
            testFile.delete();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTrackWithNullFile() {
        // 测试当文件参数为 null 时是否抛出 IllegalArgumentException
        tracker.track(null, new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTrackWithNullMarker() {
        // 测试当标识对象参数为 null 时是否抛出 IllegalArgumentException
        tracker.track(testFile, null);
    }

    @Test
    public void testTrackAndDeleteFileSuccess() throws InterruptedException {
        // 跟踪文件并触发垃圾回收以模拟删除
        Object marker = new Object();
        tracker.track(testFile, marker);

        // 释放 marker 引用
        marker = null;
        // 强制触发垃圾回收
        System.gc();

        // 等待清理线程处理
        Thread.sleep(100);

        // 验证文件是否被成功删除
        assertFalse(testFile.exists());
    }

    @Test
    public void testTrackAndDeleteFileAhead() throws InterruptedException {
        // 跟踪文件并触发垃圾回收以模拟删除
        Object marker = new Object();
        tracker.track(testFile, marker);
        // 提前删除文件
        testFile.delete();

        // 释放 marker 引用
        marker = null;
        // 强制触发垃圾回收
        System.gc();

        // 等待清理线程处理
        Thread.sleep(100);

        // 验证文件是否被成功删除
        assertFalse(testFile.exists());
    }

    @Test
    public void testStopTracker() throws InterruptedException {
        // 停止 FileReleaseTracker 的运行
        tracker.stop();

        // 等待清理线程停止
        Thread.sleep(100);

        // 验证清理线程是否已经停止
        assertFalse(isReaperThreadRunning());
    }

    @Test
    public void testMultipleDeletionsPrevented() throws InterruptedException {
        // 跟踪文件并触发垃圾回收
        Object marker = new Object();
        tracker.track(testFile, marker);

        // 释放 marker 引用
        marker = null;
        // 强制触发垃圾回收
        System.gc();

        // 等待清理线程处理
        Thread.sleep(100);

        // 验证文件是否被成功删除
        assertFalse(testFile.exists());

        // 尝试再次删除文件，应该不会有任何效果
        marker = new Object();
        tracker.track(testFile, marker);
        tracker.stop();

        // 再次验证文件是否已经被删除
        assertFalse(testFile.exists());
    }

    // 辅助方法，用于检查清理线程是否仍在运行
    private boolean isReaperThreadRunning() throws InterruptedException {
        // 允许线程有时间可能停止
        Thread.sleep(100);
        
        // 返回 tracker 是否仍然在运行
        return tracker.isRunning();
    }
}
