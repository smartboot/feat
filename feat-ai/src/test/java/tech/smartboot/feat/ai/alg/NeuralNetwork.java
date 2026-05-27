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

import java.util.ArrayList;
import java.util.List;

/**
 * 全连接神经网络 (Fully Connected Neural Network / Multi-Layer Perceptron)
 * <p>
 * 支持多隐藏层、多种激活函数、多种优化器和损失函数。
 * 提供简洁的链式API用于构建和训练神经网络。
 * </p>
 *
 * <p>使用示例:</p>
 * <pre>
 * NeuralNetwork nn = new NeuralNetwork.Builder()
 *     .addLayer(784, 256, Activation.RELU)
 *     .addLayer(256, 128, Activation.RELU)
 *     .addLayer(128, 10, Activation.SOFTMAX)
 *     .lossFunction(LossFunction.CROSS_ENTROPY)
 *     .optimizer(Optimizer.ADAM, 0.001)
 *     .build();
 *
 * nn.fit(X_train, y_train, 100, 32);
 * double[] prediction = nn.predict(X_test[0]);
 * </pre>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class NeuralNetwork {

    /**
     * 网络层列表
     */
    private final List<FCLayer> layers;

    /**
     * 损失函数
     */
    private LossFunction lossFunction;

    /**
     * 优化器
     */
    private Optimizer optimizer;

    /**
     * 学习率
     */
    private double learningRate;

    /**
     * 是否打印训练日志
     */
    private boolean verbose;

    /**
     * 私有构造函数，使用Builder模式创建
     */
    private NeuralNetwork() {
        this.layers = new ArrayList<>();
        this.verbose = true;
    }

    /**
     * 全连接层
     */
    public static class FCLayer {
        /**
         * 输入维度
         */
        final int inputSize;

        /**
         * 输出维度
         */
        final int outputSize;

        /**
         * 权重矩阵 [outputSize][inputSize]
         */
        double[][] weights;

        /**
         * 偏置向量 [outputSize]
         */
        double[] biases;

        /**
         * 激活函数
         */
        final Activation activation;

        // 前向传播缓存
        double[] lastInput;
        double[] lastZ;
        double[] lastOutput;

        // 梯度缓存
        double[][] weightGradients;
        double[] biasGradients;

        // 优化器状态
        double[][] m;  // Adam一阶矩
        double[][] v;  // Adam二阶矩
        double[] bm;   // 偏置一阶矩
        double[] bv;   // 偏置二阶矩

        /**
         * 构造函数
         *
         * @param inputSize  输入维度
         * @param outputSize 输出维度
         * @param activation 激活函数
         */
        FCLayer(int inputSize, int outputSize, Activation activation) {
            this.inputSize = inputSize;
            this.outputSize = outputSize;
            this.activation = activation;

            // Xavier/He初始化
            initializeWeights();

            // 初始化梯度缓存
            this.weightGradients = new double[outputSize][inputSize];
            this.biasGradients = new double[outputSize];

            // 初始化Adam状态
            this.m = new double[outputSize][inputSize];
            this.v = new double[outputSize][inputSize];
            this.bm = new double[outputSize];
            this.bv = new double[outputSize];
        }

        /**
         * 初始化权重
         */
        private void initializeWeights() {
            weights = new double[outputSize][inputSize];
            biases = new double[outputSize];

            double scale;
            if (activation == Activation.RELU || activation == Activation.LEAKY_RELU) {
                // He初始化
                scale = Math.sqrt(2.0 / inputSize);
            } else {
                // Xavier初始化
                scale = Math.sqrt(2.0 / (inputSize + outputSize));
            }

            for (int i = 0; i < outputSize; i++) {
                for (int j = 0; j < inputSize; j++) {
                    weights[i][j] = randomNormal() * scale;
                }
                biases[i] = 0.0;
            }
        }

        /**
         * 标准正态分布随机数（Box-Muller变换）
         */
        private double randomNormal() {
            double u1 = Math.random();
            double u2 = Math.random();
            return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
        }

        /**
         * 前向传播
         *
         * @param input 输入向量
         * @return 输出向量
         */
        double[] forward(double[] input) {
            this.lastInput = input.clone();
            this.lastZ = new double[outputSize];
            this.lastOutput = new double[outputSize];

            // 线性变换
            for (int i = 0; i < outputSize; i++) {
                double sum = biases[i];
                for (int j = 0; j < inputSize; j++) {
                    sum += weights[i][j] * input[j];
                }
                lastZ[i] = sum;
            }

            // 应用激活函数
            lastOutput = activation.apply(lastZ);
            return lastOutput.clone();
        }

        /**
         * 反向传播
         *
         * @param outputGrad 输出层梯度 (dL/da)
         * @return 输入层梯度 (dL/dx)
         */
        double[] backward(double[] outputGrad) {
            double[] inputGrad = new double[inputSize];

            // 应用激活函数导数
            double[] gradZ = activation.derivative(lastZ, outputGrad);

            // 计算梯度
            for (int i = 0; i < outputSize; i++) {
                // 偏置梯度
                biasGradients[i] += gradZ[i];

                // 权重梯度和输入梯度
                for (int j = 0; j < inputSize; j++) {
                    weightGradients[i][j] += gradZ[i] * lastInput[j];
                    inputGrad[j] += gradZ[i] * weights[i][j];
                }
            }

            return inputGrad;
        }

        /**
         * 清零梯度
         */
        void zeroGradients() {
            for (int i = 0; i < outputSize; i++) {
                biasGradients[i] = 0.0;
                for (int j = 0; j < inputSize; j++) {
                    weightGradients[i][j] = 0.0;
                }
            }
        }
    }

    /**
     * 激活函数枚举
     */
    public enum Activation {
        SIGMOID {
            @Override
            double[] apply(double[] z) {
                double[] result = new double[z.length];
                for (int i = 0; i < z.length; i++) {
                    result[i] = sigmoid(z[i]);
                }
                return result;
            }

            @Override
            double[] derivative(double[] z, double[] grad) {
                double[] result = new double[z.length];
                for (int i = 0; i < z.length; i++) {
                    double s = sigmoid(z[i]);
                    result[i] = grad[i] * s * (1 - s);
                }
                return result;
            }
        },

        RELU {
            @Override
            double[] apply(double[] z) {
                double[] result = new double[z.length];
                for (int i = 0; i < z.length; i++) {
                    result[i] = Math.max(0, z[i]);
                }
                return result;
            }

            @Override
            double[] derivative(double[] z, double[] grad) {
                double[] result = new double[z.length];
                for (int i = 0; i < z.length; i++) {
                    result[i] = grad[i] * (z[i] > 0 ? 1 : 0);
                }
                return result;
            }
        },

        LEAKY_RELU {
            private static final double ALPHA = 0.01;

            @Override
            double[] apply(double[] z) {
                double[] result = new double[z.length];
                for (int i = 0; i < z.length; i++) {
                    result[i] = z[i] > 0 ? z[i] : ALPHA * z[i];
                }
                return result;
            }

            @Override
            double[] derivative(double[] z, double[] grad) {
                double[] result = new double[z.length];
                for (int i = 0; i < z.length; i++) {
                    result[i] = grad[i] * (z[i] > 0 ? 1 : ALPHA);
                }
                return result;
            }
        },

        TANH {
            @Override
            double[] apply(double[] z) {
                double[] result = new double[z.length];
                for (int i = 0; i < z.length; i++) {
                    result[i] = Math.tanh(z[i]);
                }
                return result;
            }

            @Override
            double[] derivative(double[] z, double[] grad) {
                double[] result = new double[z.length];
                for (int i = 0; i < z.length; i++) {
                    double t = Math.tanh(z[i]);
                    result[i] = grad[i] * (1 - t * t);
                }
                return result;
            }
        },

        LINEAR {
            @Override
            double[] apply(double[] z) {
                return z.clone();
            }

            @Override
            double[] derivative(double[] z, double[] grad) {
                return grad.clone();
            }
        },

        SOFTMAX {
            @Override
            double[] apply(double[] z) {
                return softmax(z);
            }

            @Override
            double[] derivative(double[] z, double[] grad) {
                // Softmax + CrossEntropy 的组合导数简化
                // 返回 softmax(z) - target，但这里只返回grad
                // 实际计算在损失函数中处理
                double[] s = softmax(z);
                double[] result = new double[z.length];
                for (int i = 0; i < z.length; i++) {
                    result[i] = grad[i] * s[i] * (1 - s[i]);
                }
                return result;
            }
        };

        abstract double[] apply(double[] z);

        abstract double[] derivative(double[] z, double[] grad);
    }

    /**
     * 损失函数枚举
     */
    public enum LossFunction {
        MSE {
            @Override
            double compute(double[] pred, double[] target) {
                double sum = 0.0;
                for (int i = 0; i < pred.length; i++) {
                    double diff = pred[i] - target[i];
                    sum += diff * diff;
                }
                return sum / pred.length;
            }

            @Override
            double[] gradient(double[] pred, double[] target) {
                double[] grad = new double[pred.length];
                for (int i = 0; i < pred.length; i++) {
                    grad[i] = 2.0 * (pred[i] - target[i]) / pred.length;
                }
                return grad;
            }
        },

        CROSS_ENTROPY {
            @Override
            double compute(double[] pred, double[] target) {
                double loss = 0.0;
                for (int i = 0; i < pred.length; i++) {
                    if (target[i] > 0) {
                        loss -= target[i] * Math.log(Math.max(pred[i], 1e-15));
                    }
                }
                return loss;
            }

            @Override
            double[] gradient(double[] pred, double[] target) {
                // Softmax + CrossEntropy 的梯度 = pred - target
                double[] grad = new double[pred.length];
                for (int i = 0; i < pred.length; i++) {
                    grad[i] = pred[i] - target[i];
                }
                return grad;
            }
        },

        BINARY_CROSS_ENTROPY {
            @Override
            double compute(double[] pred, double[] target) {
                double loss = 0.0;
                for (int i = 0; i < pred.length; i++) {
                    double p = Math.max(Math.min(pred[i], 1 - 1e-15), 1e-15);
                    loss -= target[i] * Math.log(p) + (1 - target[i]) * Math.log(1 - p);
                }
                return loss / pred.length;
            }

            @Override
            double[] gradient(double[] pred, double[] target) {
                double[] grad = new double[pred.length];
                for (int i = 0; i < pred.length; i++) {
                    double p = Math.max(Math.min(pred[i], 1 - 1e-15), 1e-15);
                    grad[i] = (pred[i] - target[i]) / (p * (1 - p) * pred.length);
                }
                return grad;
            }
        };

        abstract double compute(double[] pred, double[] target);

        abstract double[] gradient(double[] pred, double[] target);
    }

    /**
     * 优化器枚举
     */
    public enum Optimizer {
        SGD, MOMENTUM, RMSPROP, ADAM
    }

    /**
     * 构建器模式
     */
    public static class Builder {
        private final NeuralNetwork network;

        public Builder() {
            network = new NeuralNetwork();
            network.lossFunction = LossFunction.MSE;
            network.optimizer = Optimizer.SGD;
            network.learningRate = 0.01;
        }

        /**
         * 添加全连接层
         *
         * @param inputSize  输入维度
         * @param outputSize 输出维度
         * @param activation 激活函数
         * @return Builder实例
         */
        public Builder addLayer(int inputSize, int outputSize, Activation activation) {
            network.layers.add(new FCLayer(inputSize, outputSize, activation));
            return this;
        }

        /**
         * 设置损失函数
         *
         * @param lossFunction 损失函数
         * @return Builder实例
         */
        public Builder lossFunction(LossFunction lossFunction) {
            network.lossFunction = lossFunction;
            return this;
        }

        /**
         * 设置优化器
         *
         * @param optimizer    优化器类型
         * @param learningRate 学习率
         * @return Builder实例
         */
        public Builder optimizer(Optimizer optimizer, double learningRate) {
            network.optimizer = optimizer;
            network.learningRate = learningRate;
            return this;
        }

        /**
         * 设置是否打印训练日志
         *
         * @param verbose 是否打印
         * @return Builder实例
         */
        public Builder verbose(boolean verbose) {
            network.verbose = verbose;
            return this;
        }

        /**
         * 构建神经网络
         *
         * @return NeuralNetwork实例
         */
        public NeuralNetwork build() {
            if (network.layers.isEmpty()) {
                throw new IllegalStateException("网络必须至少包含一层");
            }
            return network;
        }
    }

    /**
     * 前向传播
     *
     * @param input 输入向量
     * @return 输出向量
     */
    public double[] forward(double[] input) {
        double[] current = input;
        for (FCLayer layer : layers) {
            current = layer.forward(current);
        }
        return current;
    }

    /**
     * 预测单个样本
     *
     * @param input 输入向量
     * @return 预测结果
     */
    public double[] predict(double[] input) {
        return forward(input);
    }

    /**
     * 预测多个样本
     *
     * @param inputs 输入矩阵
     * @return 预测结果矩阵
     */
    public double[][] predict(double[][] inputs) {
        double[][] outputs = new double[inputs.length][];
        for (int i = 0; i < inputs.length; i++) {
            outputs[i] = predict(inputs[i]);
        }
        return outputs;
    }

    /**
     * 训练模型
     *
     * @param X      训练数据
     * @param y      训练标签
     * @param epochs 训练轮数
     * @param batchSize 批量大小
     */
    public void fit(double[][] X, double[][] y, int epochs, int batchSize) {
        int nSamples = X.length;

        for (int epoch = 0; epoch < epochs; epoch++) {
            double epochLoss = 0.0;
            int[] indices = shuffleIndices(nSamples);

            int nBatches = (nSamples + batchSize - 1) / batchSize;
            for (int batch = 0; batch < nBatches; batch++) {
                int start = batch * batchSize;
                int end = Math.min(start + batchSize, nSamples);
                int currentBatchSize = end - start;

                // 清零梯度
                for (FCLayer layer : layers) {
                    layer.zeroGradients();
                }

                // 前向传播和反向传播
                for (int i = 0; i < currentBatchSize; i++) {
                    int idx = indices[start + i];
                    double[] output = forward(X[idx]);
                    double[] outputGrad = lossFunction.gradient(output, y[idx]);
                    epochLoss += lossFunction.compute(output, y[idx]);

                    // 反向传播
                    for (int l = layers.size() - 1; l >= 0; l--) {
                        outputGrad = layers.get(l).backward(outputGrad);
                    }
                }

                // 更新参数
                updateParameters(currentBatchSize, epoch * nBatches + batch + 1);
            }

            if (verbose && (epoch + 1) % 10 == 0) {
                System.out.println("Epoch " + (epoch + 1) + "/" + epochs +
                        ", Loss: " + String.format("%.6f", epochLoss / nSamples));
            }
        }
    }

    /**
     * 更新参数
     *
     * @param batchSize 批量大小
     * @param t         时间步（用于Adam）
     */
    private void updateParameters(int batchSize, int t) {
        double beta1 = 0.9;
        double beta2 = 0.999;
        double epsilon = 1e-8;

        for (FCLayer layer : layers) {
            for (int i = 0; i < layer.outputSize; i++) {
                // 更新偏置
                double bg = layer.biasGradients[i] / batchSize;

                switch (optimizer) {
                    case SGD:
                        layer.biases[i] -= learningRate * bg;
                        break;
                    case MOMENTUM:
                        layer.bm[i] = 0.9 * layer.bm[i] + bg;
                        layer.biases[i] -= learningRate * layer.bm[i];
                        break;
                    case RMSPROP:
                        layer.bv[i] = 0.9 * layer.bv[i] + 0.1 * bg * bg;
                        layer.biases[i] -= learningRate * bg / (Math.sqrt(layer.bv[i]) + epsilon);
                        break;
                    case ADAM:
                        layer.bm[i] = beta1 * layer.bm[i] + (1 - beta1) * bg;
                        layer.bv[i] = beta2 * layer.bv[i] + (1 - beta2) * bg * bg;
                        double bmHat = layer.bm[i] / (1 - Math.pow(beta1, t));
                        double bvHat = layer.bv[i] / (1 - Math.pow(beta2, t));
                        layer.biases[i] -= learningRate * bmHat / (Math.sqrt(bvHat) + epsilon);
                        break;
                }

                // 更新权重
                for (int j = 0; j < layer.inputSize; j++) {
                    double wg = layer.weightGradients[i][j] / batchSize;

                    switch (optimizer) {
                        case SGD:
                            layer.weights[i][j] -= learningRate * wg;
                            break;
                        case MOMENTUM:
                            layer.m[i][j] = 0.9 * layer.m[i][j] + wg;
                            layer.weights[i][j] -= learningRate * layer.m[i][j];
                            break;
                        case RMSPROP:
                            layer.v[i][j] = 0.9 * layer.v[i][j] + 0.1 * wg * wg;
                            layer.weights[i][j] -= learningRate * wg / (Math.sqrt(layer.v[i][j]) + epsilon);
                            break;
                        case ADAM:
                            layer.m[i][j] = beta1 * layer.m[i][j] + (1 - beta1) * wg;
                            layer.v[i][j] = beta2 * layer.v[i][j] + (1 - beta2) * wg * wg;
                            double mHat = layer.m[i][j] / (1 - Math.pow(beta1, t));
                            double vHat = layer.v[i][j] / (1 - Math.pow(beta2, t));
                            layer.weights[i][j] -= learningRate * mHat / (Math.sqrt(vHat) + epsilon);
                            break;
                    }
                }
            }
        }
    }

    /**
     * 计算准确率（用于分类任务）
     *
     * @param X 测试数据
     * @param y 真实标签（one-hot编码）
     * @return 准确率
     */
    public double accuracy(double[][] X, double[][] y) {
        int correct = 0;
        for (int i = 0; i < X.length; i++) {
            double[] pred = predict(X[i]);
            int predClass = argMax(pred);
            int trueClass = argMax(y[i]);
            if (predClass == trueClass) {
                correct++;
            }
        }
        return (double) correct / X.length;
    }

    /**
     * 计算损失
     *
     * @param X 数据
     * @param y 标签
     * @return 平均损失
     */
    public double evaluate(double[][] X, double[][] y) {
        double totalLoss = 0.0;
        for (int i = 0; i < X.length; i++) {
            double[] pred = predict(X[i]);
            totalLoss += lossFunction.compute(pred, y[i]);
        }
        return totalLoss / X.length;
    }

    /**
     * 获取数组最大值的索引
     */
    private int argMax(double[] arr) {
        int maxIdx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[maxIdx]) {
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    /**
     * Softmax函数
     */
    private static double[] softmax(double[] z) {
        double[] result = new double[z.length];
        double max = Double.NEGATIVE_INFINITY;
        for (double v : z) {
            if (v > max) max = v;
        }

        double sum = 0.0;
        for (int i = 0; i < z.length; i++) {
            result[i] = Math.exp(z[i] - max);
            sum += result[i];
        }

        for (int i = 0; i < z.length; i++) {
            result[i] /= sum;
        }
        return result;
    }

    /**
     * Sigmoid函数
     */
    private static double sigmoid(double z) {
        if (z < -500) return 0.0;
        if (z > 500) return 1.0;
        return 1.0 / (1.0 + Math.exp(-z));
    }

    /**
     * 随机打乱索引
     */
    private int[] shuffleIndices(int n) {
        int[] indices = new int[n];
        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = (int) (Math.random() * (i + 1));
            int temp = indices[i];
            indices[i] = indices[j];
            indices[j] = temp;
        }
        return indices;
    }

    /**
     * 获取网络层数
     *
     * @return 层数
     */
    public int getLayerCount() {
        return layers.size();
    }

    /**
     * 获取指定层
     *
     * @param index 层索引
     * @return 全连接层
     */
    public FCLayer getLayer(int index) {
        return layers.get(index);
    }
}
