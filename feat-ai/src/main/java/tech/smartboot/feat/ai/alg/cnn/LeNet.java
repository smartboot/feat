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

/**
 * LeNet-5 卷积神经网络实现
 * <p>
 * LeNet-5是Yann LeCun于1998年提出的经典CNN架构，
 * 最初用于手写数字识别（MNIST数据集）。
 * </p>
 *
 * <p>网络结构：</p>
 * <pre>
 * Input: 1x32x32 (单通道灰度图)
 *   ↓
 * C1: Conv(6@5x5) → 6x28x28
 *   ↓
 * S2: MaxPool(2x2) → 6x14x14
 *   ↓
 * C3: Conv(16@5x5) → 16x10x10
 *   ↓
 * S4: MaxPool(2x2) → 16x5x5
 *   ↓
 * C5: Conv(120@5x5) → 120x1x1
 *   ↓
 * Flatten → 120
 *   ↓
 * F6: FC(120, 84) → 84
 *   ↓
 * Output: FC(84, 10) + Softmax → 10
 * </pre>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class LeNet {

    private final ConvolutionalNeuralNetwork cnn;

    /**
     * 构造函数
     *
     * @param numClasses 分类数量（MNIST为10）
     * @param learningRate 学习率
     */
    public LeNet(int numClasses, double learningRate) {
        this.cnn = new ConvolutionalNeuralNetwork.Builder()
                // C1: 输入1通道，输出6通道，5x5卷积核
                .addConvLayer(1, 6, 5, 1, 0)
                .addActivation(CNNBackPropagation.ActivationType.TANH)
                // S2: 2x2最大池化
                .addMaxPoolLayer(2, 2)
                // C3: 输入6通道，输出16通道，5x5卷积核
                .addConvLayer(6, 16, 5, 1, 0)
                .addActivation(CNNBackPropagation.ActivationType.TANH)
                // S4: 2x2最大池化
                .addMaxPoolLayer(2, 2)
                // C5: 输入16通道，输出120通道，5x5卷积核
                .addConvLayer(16, 120, 5, 1, 0)
                .addActivation(CNNBackPropagation.ActivationType.TANH)
                // 展平
                .addFlatten()
                // F6: 全连接层 120 -> 84
                .addFCLayer(84)
                .addActivation(CNNBackPropagation.ActivationType.TANH)
                // 输出层: 84 -> numClasses
                .addFCLayer(numClasses)
                .addSoftmax()
                .learningRate(learningRate)
                .build();
    }

    /**
     * 训练模型
     *
     * @param images   训练图像 [N][1][32][32]
     * @param labels   标签（one-hot编码） [N][numClasses]
     * @param epochs   训练轮数
     * @param batchSize 批量大小
     */
    public void train(double[][][][] images, double[][] labels, int epochs, int batchSize) {
        cnn.train(images, labels, epochs, batchSize);
    }

    /**
     * 预测
     *
     * @param image 输入图像 [1][32][32]
     * @return 预测类别
     */
    public int predict(double[][][] image) {
        return cnn.predict(image);
    }

    /**
     * 批量预测
     *
     * @param images 输入图像 [N][1][32][32]
     * @return 预测类别数组
     */
    public int[] predict(double[][][][] images) {
        return cnn.predict(images);
    }

    /**
     * 计算准确率
     *
     * @param images 测试图像
     * @param labels 真实标签
     * @return 准确率
     */
    public double accuracy(double[][][][] images, int[] labels) {
        return cnn.accuracy(images, labels);
    }

    /**
     * 设置训练模式
     *
     * @param training 是否训练模式
     */
    public void setTraining(boolean training) {
        cnn.setTraining(training);
    }

    /**
     * 创建MNIST数据集的LeNet模型
     *
     * @return LeNet实例（10分类，学习率0.001）
     */
    public static LeNet forMNIST() {
        return new LeNet(10, 0.001);
    }

    /**
     * 创建MNIST数据集的LeNet模型（自定义学习率）
     *
     * @param learningRate 学习率
     * @return LeNet实例
     */
    public static LeNet forMNIST(double learningRate) {
        return new LeNet(10, learningRate);
    }
}
