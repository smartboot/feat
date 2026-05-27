/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.alg.cnn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 图像数据加载器
 * <p>
 * 提供图像数据的批量加载、预处理和增强功能。
 * 支持数据打乱、分批、数据增强等操作。
 * </p>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ImageDataLoader {

    /**
     * 图像数据
     */
    private double[][][][] images;

    /**
     * 标签（one-hot编码）
     */
    private double[][] labels;

    /**
     * 标签（整数类别）
     */
    private int[] labelIndices;

    /**
     * 批量大小
     */
    private int batchSize;

    /**
     * 是否打乱数据
     */
    private boolean shuffle;

    /**
     * 当前批次索引
     */
    private int currentBatch;

    /**
     * 数据索引
     */
    private int[] indices;

    /**
     * 是否启用数据增强
     */
    private boolean augmentation;

    /**
     * 数据增强参数
     */
    private double flipProbability = 0.5;
    private double brightnessRange = 0.2;
    private double contrastRange = 0.2;

    /**
     * 归一化方法
     */
    private ImagePreprocessing.Normalization normalization = ImagePreprocessing.Normalization.MIN_MAX;

    /**
     * 构造函数
     *
     * @param images    图像数据 [N][C][H][W]
     * @param labels    标签（one-hot编码） [N][numClasses]
     * @param batchSize 批量大小
     * @param shuffle   是否打乱数据
     */
    public ImageDataLoader(double[][][][] images, double[][] labels, int batchSize, boolean shuffle) {
        this.images = images;
        this.labels = labels;
        this.batchSize = batchSize;
        this.shuffle = shuffle;
        this.currentBatch = 0;
        this.augmentation = false;

        // 初始化索引
        this.indices = new int[images.length];
        for (int i = 0; i < images.length; i++) {
            indices[i] = i;
        }

        if (shuffle) {
            shuffleIndices();
        }
    }

    /**
     * 构造函数（使用整数标签）
     *
     * @param images      图像数据
     * @param labelIndices 标签索引 [N]
     * @param numClasses  类别数量
     * @param batchSize   批量大小
     * @param shuffle     是否打乱数据
     */
    public ImageDataLoader(double[][][][] images, int[] labelIndices, int numClasses,
                          int batchSize, boolean shuffle) {
        this.images = images;
        this.labelIndices = labelIndices;
        this.batchSize = batchSize;
        this.shuffle = shuffle;
        this.currentBatch = 0;
        this.augmentation = false;

        // 转换为one-hot编码
        this.labels = new double[images.length][numClasses];
        for (int i = 0; i < images.length; i++) {
            labels[i][labelIndices[i]] = 1.0;
        }

        // 初始化索引
        this.indices = new int[images.length];
        for (int i = 0; i < images.length; i++) {
            indices[i] = i;
        }

        if (shuffle) {
            shuffleIndices();
        }
    }

    /**
     * 设置数据增强
     *
     * @param enabled 是否启用数据增强
     * @return 当前实例
     */
    public ImageDataLoader setAugmentation(boolean enabled) {
        this.augmentation = enabled;
        return this;
    }

    /**
     * 设置数据增强参数
     *
     * @param flipProbability 水平翻转概率
     * @param brightnessRange 亮度调整范围
     * @param contrastRange   对比度调整范围
     * @return 当前实例
     */
    public ImageDataLoader setAugmentationParams(double flipProbability,
                                                 double brightnessRange,
                                                 double contrastRange) {
        this.flipProbability = flipProbability;
        this.brightnessRange = brightnessRange;
        this.contrastRange = contrastRange;
        return this;
    }

    /**
     * 设置归一化方法
     *
     * @param normalization 归一化方法
     * @return 当前实例
     */
    public ImageDataLoader setNormalization(ImagePreprocessing.Normalization normalization) {
        this.normalization = normalization;
        return this;
    }

    /**
     * 打乱索引
     */
    private void shuffleIndices() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < indices.length; i++) {
            list.add(i);
        }
        Collections.shuffle(list);
        for (int i = 0; i < indices.length; i++) {
            indices[i] = list.get(i);
        }
    }

    /**
     * 获取批次数量
     *
     * @return 批次数量
     */
    public int getNumBatches() {
        return (images.length + batchSize - 1) / batchSize;
    }

    /**
     * 获取数据集大小
     *
     * @return 数据集大小
     */
    public int getDatasetSize() {
        return images.length;
    }

    /**
     * 是否有下一个批次
     *
     * @return 是否有下一个批次
     */
    public boolean hasNext() {
        return currentBatch < getNumBatches();
    }

    /**
     * 获取下一个批次
     *
     * @return 批次数据
     */
    public Batch next() {
        if (!hasNext()) {
            throw new IllegalStateException("没有更多批次");
        }

        int start = currentBatch * batchSize;
        int end = Math.min(start + batchSize, images.length);
        int currentBatchSize = end - start;

        double[][][][] batchImages = new double[currentBatchSize][][][];
        double[][] batchLabels = new double[currentBatchSize][];

        for (int i = 0; i < currentBatchSize; i++) {
            int idx = indices[start + i];
            double[][][] image = images[idx];

            // 应用数据增强
            if (augmentation) {
                image = applyAugmentation(image);
            }

            // 应用归一化
            image = ImagePreprocessing.normalize(image, normalization);

            batchImages[i] = image;
            batchLabels[i] = labels[idx];
        }

        currentBatch++;
        return new Batch(batchImages, batchLabels, currentBatchSize);
    }

    /**
     * 重置数据加载器
     */
    public void reset() {
        currentBatch = 0;
        if (shuffle) {
            shuffleIndices();
        }
    }

    /**
     * 应用数据增强
     *
     * @param image 输入图像
     * @return 增强后的图像
     */
    private double[][][] applyAugmentation(double[][][] image) {
        // 随机水平翻转
        image = ImagePreprocessing.randomHorizontalFlip(image, flipProbability);

        // 颜色抖动
        image = ImagePreprocessing.colorJitter(image, brightnessRange, contrastRange);

        return image;
    }

    /**
     * 获取训练集和验证集分割
     *
     * @param trainRatio 训练集比例
     * @return 训练集和验证集的数据加载器
     */
    public ImageDataLoader[] split(double trainRatio) {
        int trainSize = (int) (images.length * trainRatio);
        int valSize = images.length - trainSize;

        // 训练集
        double[][][][] trainImages = new double[trainSize][][][];
        double[][] trainLabels = new double[trainSize][];

        // 验证集
        double[][][][] valImages = new double[valSize][][][];
        double[][] valLabels = new double[valSize][];

        for (int i = 0; i < trainSize; i++) {
            trainImages[i] = images[indices[i]];
            trainLabels[i] = labels[indices[i]];
        }

        for (int i = 0; i < valSize; i++) {
            valImages[i] = images[indices[trainSize + i]];
            valLabels[i] = labels[indices[trainSize + i]];
        }

        ImageDataLoader trainLoader = new ImageDataLoader(trainImages, trainLabels, batchSize, shuffle);
        ImageDataLoader valLoader = new ImageDataLoader(valImages, valLabels, batchSize, false);

        return new ImageDataLoader[]{trainLoader, valLoader};
    }

    /**
     * 批次数据类
     */
    public static class Batch {
        public final double[][][][] images;
        public final double[][] labels;
        public final int size;

        public Batch(double[][][][] images, double[][] labels, int size) {
            this.images = images;
            this.labels = labels;
            this.size = size;
        }
    }

    /**
     * 从像素数组创建图像数据
     *
     * @param pixels    像素数组 [N][height*width*channels]
     * @param height    图像高度
     * @param width     图像宽度
     * @param channels  通道数
     * @param labels    标签
     * @param batchSize 批量大小
     * @param shuffle   是否打乱
     * @return 数据加载器
     */
    public static ImageDataLoader fromPixels(double[][] pixels, int height, int width, int channels,
                                            double[][] labels, int batchSize, boolean shuffle) {
        int nSamples = pixels.length;
        double[][][][] images = new double[nSamples][channels][height][width];

        for (int n = 0; n < nSamples; n++) {
            for (int c = 0; c < channels; c++) {
                for (int h = 0; h < height; h++) {
                    for (int w = 0; w < width; w++) {
                        int idx = (h * width + w) * channels + c;
                        images[n][c][h][w] = pixels[n][idx];
                    }
                }
            }
        }

        return new ImageDataLoader(images, labels, batchSize, shuffle);
    }

    /**
     * 从像素数组创建图像数据（整数标签）
     *
     * @param pixels       像素数组
     * @param height       图像高度
     * @param width        图像宽度
     * @param channels     通道数
     * @param labelIndices 标签索引
     * @param numClasses   类别数量
     * @param batchSize    批量大小
     * @param shuffle      是否打乱
     * @return 数据加载器
     */
    public static ImageDataLoader fromPixels(double[][] pixels, int height, int width, int channels,
                                            int[] labelIndices, int numClasses,
                                            int batchSize, boolean shuffle) {
        int nSamples = pixels.length;
        double[][][][] images = new double[nSamples][channels][height][width];

        for (int n = 0; n < nSamples; n++) {
            for (int c = 0; c < channels; c++) {
                for (int h = 0; h < height; h++) {
                    for (int w = 0; w < width; w++) {
                        int idx = (h * width + w) * channels + c;
                        images[n][c][h][w] = pixels[n][idx];
                    }
                }
            }
        }

        return new ImageDataLoader(images, labelIndices, numClasses, batchSize, shuffle);
    }

    /**
     * 创建MNIST数据加载器
     *
     * @param images    图像数据 [N][784]
     * @param labels    标签 [N][10]
     * @param batchSize 批量大小
     * @param shuffle   是否打乱
     * @return 数据加载器
     */
    public static ImageDataLoader forMNIST(double[][] images, double[][] labels,
                                          int batchSize, boolean shuffle) {
        return fromPixels(images, 28, 28, 1, labels, batchSize, shuffle)
                .setNormalization(ImagePreprocessing.Normalization.MIN_MAX);
    }

    /**
     * 创建MNIST数据加载器（整数标签）
     *
     * @param images       图像数据
     * @param labelIndices 标签索引
     * @param batchSize    批量大小
     * @param shuffle      是否打乱
     * @return 数据加载器
     */
    public static ImageDataLoader forMNIST(double[][] images, int[] labelIndices,
                                          int batchSize, boolean shuffle) {
        return fromPixels(images, 28, 28, 1, labelIndices, 10, batchSize, shuffle)
                .setNormalization(ImagePreprocessing.Normalization.MIN_MAX);
    }

    /**
     * 创建CIFAR-10数据加载器
     *
     * @param images    图像数据 [N][3072]
     * @param labels    标签 [N][10]
     * @param batchSize 批量大小
     * @param shuffle   是否打乱
     * @return 数据加载器
     */
    public static ImageDataLoader forCIFAR10(double[][] images, double[][] labels,
                                            int batchSize, boolean shuffle) {
        return fromPixels(images, 32, 32, 3, labels, batchSize, shuffle)
                .setNormalization(ImagePreprocessing.Normalization.STANDARD);
    }

    /**
     * 创建CIFAR-10数据加载器（整数标签）
     *
     * @param images       图像数据
     * @param labelIndices 标签索引
     * @param batchSize    批量大小
     * @param shuffle      是否打乱
     * @return 数据加载器
     */
    public static ImageDataLoader forCIFAR10(double[][] images, int[] labelIndices,
                                            int batchSize, boolean shuffle) {
        return fromPixels(images, 32, 32, 3, labelIndices, 10, batchSize, shuffle)
                .setNormalization(ImagePreprocessing.Normalization.STANDARD);
    }
}
