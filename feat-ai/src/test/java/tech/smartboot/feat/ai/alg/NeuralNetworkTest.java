/*
 *  Copyright (C) [2024] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循Apache-2.0开源协议合理合法使用本项目。
 *
 *   Enterprise users are required to use this project reasonably
 *   and legally in accordance with the Apache-2.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.feat.ai.alg;

/**
 * 全连接神经网络测试类
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class NeuralNetworkTest {

    public static void main(String[] args) {
        System.out.println("=== 全连接神经网络测试 ===\n");

        testXOR();
        testClassification();
        testRegression();
        testMNISTLike();

        System.out.println("\n=== 所有测试完成 ===");
    }

    /**
     * 测试XOR问题
     */
    static void testXOR() {
        System.out.println("--- XOR问题测试 ---");
        System.out.println("网络结构: 2 -> 4 -> 1");
        System.out.println("激活函数: Sigmoid");
        System.out.println("损失函数: MSE");
        System.out.println("优化器: Adam");

        // 构建网络
        NeuralNetwork nn = new NeuralNetwork.Builder()
                .addLayer(2, 4, NeuralNetwork.Activation.SIGMOID)
                .addLayer(4, 1, NeuralNetwork.Activation.SIGMOID)
                .lossFunction(NeuralNetwork.LossFunction.MSE)
                .optimizer(NeuralNetwork.Optimizer.ADAM, 0.1)
                .verbose(false)
                .build();

        // XOR数据
        double[][] X = {{0, 0}, {0, 1}, {1, 0}, {1, 1}};
        double[][] y = {{0}, {1}, {1}, {0}};

        System.out.println("\n训练前预测:");
        for (int i = 0; i < X.length; i++) {
            double[] pred = nn.predict(X[i]);
            System.out.printf("  %.0f XOR %.0f = %.4f (目标: %.0f)%n",
                    X[i][0], X[i][1], pred[0], y[i][0]);
        }

        // 训练
        System.out.println("\n开始训练...");
        nn.fit(X, y, 2000, 4);

        System.out.println("\n训练后预测:");
        for (int i = 0; i < X.length; i++) {
            double[] pred = nn.predict(X[i]);
            System.out.printf("  %.0f XOR %.0f = %.4f (目标: %.0f)%n",
                    X[i][0], X[i][1], pred[0], y[i][0]);
        }

        double loss = nn.evaluate(X, y);
        System.out.printf("最终损失: %.6f%n", loss);
        System.out.println();
    }

    /**
     * 测试多分类问题
     */
    static void testClassification() {
        System.out.println("--- 多分类问题测试 ---");
        System.out.println("网络结构: 2 -> 8 -> 3");
        System.out.println("激活函数: ReLU -> Softmax");
        System.out.println("损失函数: CrossEntropy");
        System.out.println("优化器: Adam");

        // 构建网络
        NeuralNetwork nn = new NeuralNetwork.Builder()
                .addLayer(2, 8, NeuralNetwork.Activation.RELU)
                .addLayer(8, 3, NeuralNetwork.Activation.SOFTMAX)
                .lossFunction(NeuralNetwork.LossFunction.CROSS_ENTROPY)
                .optimizer(NeuralNetwork.Optimizer.ADAM, 0.01)
                .verbose(true)
                .build();

        // 生成三类数据
        // 类别0: 中心(1, 1)
        // 类别1: 中心(3, 3)
        // 类别2: 中心(5, 1)
        int nSamplesPerClass = 50;
        double[][] X = new double[nSamplesPerClass * 3][2];
        double[][] y = new double[nSamplesPerClass * 3][3];

        for (int i = 0; i < nSamplesPerClass; i++) {
            // 类别0
            X[i][0] = 1 + (Math.random() - 0.5) * 1.5;
            X[i][1] = 1 + (Math.random() - 0.5) * 1.5;
            y[i][0] = 1;

            // 类别1
            X[nSamplesPerClass + i][0] = 3 + (Math.random() - 0.5) * 1.5;
            X[nSamplesPerClass + i][1] = 3 + (Math.random() - 0.5) * 1.5;
            y[nSamplesPerClass + i][1] = 1;

            // 类别2
            X[2 * nSamplesPerClass + i][0] = 5 + (Math.random() - 0.5) * 1.5;
            X[2 * nSamplesPerClass + i][1] = 1 + (Math.random() - 0.5) * 1.5;
            y[2 * nSamplesPerClass + i][2] = 1;
        }

        System.out.println("\n开始训练...");
        nn.fit(X, y, 100, 16);

        double accuracy = nn.accuracy(X, y);
        System.out.printf("训练集准确率: %.2f%%%n", accuracy * 100);

        // 测试几个点
        double[][] testX = {
                {1, 1},   // 应该是类别0
                {3, 3},   // 应该是类别1
                {5, 1},   // 应该是类别2
                {2, 2}    // 边界点
        };

        System.out.println("\n测试预测:");
        for (double[] x : testX) {
            double[] pred = nn.predict(x);
            int predClass = argMax(pred);
            System.out.printf("  (%.1f, %.1f) => 类别 %d, 概率 [%.3f, %.3f, %.3f]%n",
                    x[0], x[1], predClass, pred[0], pred[1], pred[2]);
        }
        System.out.println();
    }

    /**
     * 测试回归问题
     */
    static void testRegression() {
        System.out.println("--- 回归问题测试 ---");
        System.out.println("网络结构: 1 -> 16 -> 16 -> 1");
        System.out.println("激活函数: ReLU -> ReLU -> Linear");
        System.out.println("损失函数: MSE");
        System.out.println("优化器: Adam");

        // 构建网络
        NeuralNetwork nn = new NeuralNetwork.Builder()
                .addLayer(1, 16, NeuralNetwork.Activation.RELU)
                .addLayer(16, 16, NeuralNetwork.Activation.RELU)
                .addLayer(16, 1, NeuralNetwork.Activation.LINEAR)
                .lossFunction(NeuralNetwork.LossFunction.MSE)
                .optimizer(NeuralNetwork.Optimizer.ADAM, 0.01)
                .verbose(true)
                .build();

        // 生成数据: y = sin(x) + noise
        int nSamples = 200;
        double[][] X = new double[nSamples][1];
        double[][] y = new double[nSamples][1];

        for (int i = 0; i < nSamples; i++) {
            X[i][0] = i * 2 * Math.PI / nSamples;
            y[i][0] = Math.sin(X[i][0]) + (Math.random() - 0.5) * 0.2;
        }

        System.out.println("\n开始训练...");
        nn.fit(X, y, 200, 32);

        double loss = nn.evaluate(X, y);
        System.out.printf("最终MSE: %.6f%n", loss);

        // 测试几个点
        double[][] testX = {{0}, {Math.PI / 2}, {Math.PI}, {3 * Math.PI / 2}};
        System.out.println("\n测试预测 (y = sin(x)):");
        for (double[] x : testX) {
            double[] pred = nn.predict(x);
            System.out.printf("  x=%.4f, 预测=%.4f, 真实=%.4f%n",
                    x[0], pred[0], Math.sin(x[0]));
        }
        System.out.println();
    }

    /**
     * 测试类似MNIST的分类任务
     */
    static void testMNISTLike() {
        System.out.println("--- MNIST-like分类测试 ---");
        System.out.println("网络结构: 784 -> 128 -> 64 -> 10");
        System.out.println("激活函数: ReLU -> ReLU -> Softmax");
        System.out.println("损失函数: CrossEntropy");
        System.out.println("优化器: Adam");

        // 构建网络
        NeuralNetwork nn = new NeuralNetwork.Builder()
                .addLayer(784, 128, NeuralNetwork.Activation.RELU)
                .addLayer(128, 64, NeuralNetwork.Activation.RELU)
                .addLayer(64, 10, NeuralNetwork.Activation.SOFTMAX)
                .lossFunction(NeuralNetwork.LossFunction.CROSS_ENTROPY)
                .optimizer(NeuralNetwork.Optimizer.ADAM, 0.001)
                .verbose(true)
                .build();

        // 生成模拟MNIST数据
        int nSamples = 500;
        double[][] X = new double[nSamples][784];
        double[][] y = new double[nSamples][10];

        for (int i = 0; i < nSamples; i++) {
            int label = i % 10;
            y[i][label] = 1;

            // 为每个数字生成不同的特征模式
            for (int j = 0; j < 784; j++) {
                // 添加一些与标签相关的模式
                double base = (j % (label + 1)) / 10.0;
                X[i][j] = base + (Math.random() - 0.5) * 0.3;
                if (X[i][j] < 0) X[i][j] = 0;
                if (X[i][j] > 1) X[i][j] = 1;
            }
        }

        System.out.println("\n开始训练...");
        nn.fit(X, y, 50, 32);

        double accuracy = nn.accuracy(X, y);
        System.out.printf("训练集准确率: %.2f%%%n", accuracy * 100);

        // 测试几个样本
        System.out.println("\n测试预测:");
        for (int i = 0; i < 5; i++) {
            double[] pred = nn.predict(X[i]);
            int predClass = argMax(pred);
            int trueClass = argMax(y[i]);
            System.out.printf("  样本 %d: 预测=%d, 真实=%d%n", i, predClass, trueClass);
        }
        System.out.println();
    }

    /**
     * 获取数组最大值的索引
     */
    static int argMax(double[] arr) {
        int maxIdx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[maxIdx]) {
                maxIdx = i;
            }
        }
        return maxIdx;
    }
}
