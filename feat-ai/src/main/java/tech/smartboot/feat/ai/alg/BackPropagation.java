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
import java.util.function.Function;

/**
 * 反向传播算法实现
 * <p>
 * 反向传播（Backpropagation）是训练神经网络的核心算法，通过链式法则计算损失函数
 * 对各层参数的梯度，从而使用梯度下降更新网络权重。
 * </p>
 *
 * <p>算法流程：</p>
 * <ol>
 *   <li>前向传播：计算网络输出</li>
 *   <li>计算损失：比较输出与目标</li>
 *   <li>反向传播：从输出层向输入层计算梯度</li>
 *   <li>参数更新：使用梯度下降更新权重</li>
 * </ol>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class BackPropagation {

    /**
     * 神经网络层
     */
    public static class Layer {
        /**
         * 输入维度
         */
        int inputSize;

        /**
         * 输出维度
         */
        int outputSize;

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
        Function<Double, Double> activation;

        /**
         * 激活函数的导数
         */
        Function<Double, Double> activationDerivative;

        /**
         * 前向传播缓存
         */
        double[] lastInput;
        double[] lastZ;  // 激活前的值
        double[] lastOutput;

        /**
         * 梯度缓存
         */
        double[][] weightGradients;
        double[] biasGradients;

        /**
         * 构造函数
         *
         * @param inputSize              输入维度
         * @param outputSize             输出维度
         * @param activation             激活函数
         * @param activationDerivative   激活函数导数
         */
        public Layer(int inputSize, int outputSize,
                     Function<Double, Double> activation,
                     Function<Double, Double> activationDerivative) {
            this.inputSize = inputSize;
            this.outputSize = outputSize;
            this.activation = activation;
            this.activationDerivative = activationDerivative;

            // Xavier初始化权重
            this.weights = new double[outputSize][inputSize];
            this.biases = new double[outputSize];
            double scale = Math.sqrt(2.0 / (inputSize + outputSize));

            for (int i = 0; i < outputSize; i++) {
                for (int j = 0; j < inputSize; j++) {
                    weights[i][j] = (Math.random() - 0.5) * 2 * scale;
                }
            }

            this.weightGradients = new double[outputSize][inputSize];
            this.biasGradients = new double[outputSize];
        }

        /**
         * 前向传播
         *
         * @param input 输入向量
         * @return 输出向量
         */
        public double[] forward(double[] input) {
            this.lastInput = input.clone();
            this.lastZ = new double[outputSize];
            this.lastOutput = new double[outputSize];

            for (int i = 0; i < outputSize; i++) {
                double sum = biases[i];
                for (int j = 0; j < inputSize; j++) {
                    sum += weights[i][j] * input[j];
                }
                lastZ[i] = sum;
                lastOutput[i] = activation.apply(sum);
            }

            return lastOutput.clone();
        }

        /**
         * 反向传播
         *
         * @param outputGrad 输出梯度
         * @return 输入梯度（传递给前一层）
         */
        public double[] backward(double[] outputGrad) {
            double[] inputGrad = new double[inputSize];

            for (int i = 0; i < outputSize; i++) {
                // 应用激活函数导数
                double grad = outputGrad[i] * activationDerivative.apply(lastZ[i]);

                // 计算偏置梯度
                biasGradients[i] += grad;

                // 计算权重梯度和输入梯度
                for (int j = 0; j < inputSize; j++) {
                    weightGradients[i][j] += grad * lastInput[j];
                    inputGrad[j] += grad * weights[i][j];
                }
            }

            return inputGrad;
        }

        /**
         * 清零梯度
         */
        public void zeroGradients() {
            for (int i = 0; i < outputSize; i++) {
                biasGradients[i] = 0.0;
                for (int j = 0; j < inputSize; j++) {
                    weightGradients[i][j] = 0.0;
                }
            }
        }

        /**
         * 更新参数
         *
         * @param learningRate 学习率
         * @param batchSize    批量大小
         */
        public void updateParameters(double learningRate, int batchSize) {
            double scale = learningRate / batchSize;
            for (int i = 0; i < outputSize; i++) {
                biases[i] -= scale * biasGradients[i];
                for (int j = 0; j < inputSize; j++) {
                    weights[i][j] -= scale * weightGradients[i][j];
                }
            }
        }
    }

    /**
     * 神经网络
     */
    public static class NeuralNetwork {
        private final List<Layer> layers;
        private final double learningRate;

        /**
         * 构造函数
         *
         * @param learningRate 学习率
         */
        public NeuralNetwork(double learningRate) {
            this.layers = new ArrayList<>();
            this.learningRate = learningRate;
        }

        /**
         * 添加层
         *
         * @param layer 网络层
         * @return 当前实例
         */
        public NeuralNetwork addLayer(Layer layer) {
            layers.add(layer);
            return this;
        }

        /**
         * 前向传播
         *
         * @param input 输入向量
         * @return 输出向量
         */
        public double[] forward(double[] input) {
            double[] current = input;
            for (Layer layer : layers) {
                current = layer.forward(current);
            }
            return current;
        }

        /**
         * 训练一个批次
         *
         * @param inputs  输入批次
         * @param targets 目标批次
         * @return 平均损失
         */
        public double trainBatch(double[][] inputs, double[][] targets) {
            int batchSize = inputs.length;
            double totalLoss = 0.0;

            // 清零梯度
            for (Layer layer : layers) {
                layer.zeroGradients();
            }

            // 前向传播和反向传播
            for (int i = 0; i < batchSize; i++) {
                // 前向传播
                double[] output = forward(inputs[i]);

                // 计算损失和输出梯度
                double[] outputGrad = new double[output.length];
                for (int j = 0; j < output.length; j++) {
                    double diff = output[j] - targets[i][j];
                    totalLoss += diff * diff;
                    outputGrad[j] = 2 * diff;  // MSE梯度
                }

                // 反向传播
                for (int l = layers.size() - 1; l >= 0; l--) {
                    outputGrad = layers.get(l).backward(outputGrad);
                }
            }

            // 更新参数
            for (Layer layer : layers) {
                layer.updateParameters(learningRate, batchSize);
            }

            return totalLoss / batchSize;
        }

        /**
         * 预测
         *
         * @param input 输入
         * @return 预测输出
         */
        public double[] predict(double[] input) {
            return forward(input);
        }

        /**
         * 预测多个样本
         *
         * @param inputs 输入数组
         * @return 预测结果数组
         */
        public double[][] predict(double[][] inputs) {
            double[][] outputs = new double[inputs.length][];
            for (int i = 0; i < inputs.length; i++) {
                outputs[i] = predict(inputs[i]);
            }
            return outputs;
        }
    }

    /**
     * 常用激活函数
     */
    public static class Activations {

        /**
         * Sigmoid激活函数
         */
        public static final Function<Double, Double> SIGMOID = z -> {
            if (z < -500) return 0.0;
            if (z > 500) return 1.0;
            return 1.0 / (1.0 + Math.exp(-z));
        };

        /**
         * Sigmoid导数
         */
        public static final Function<Double, Double> SIGMOID_DERIVATIVE = z -> {
            double s = SIGMOID.apply(z);
            return s * (1 - s);
        };

        /**
         * ReLU激活函数
         */
        public static final Function<Double, Double> RELU = z -> Math.max(0, z);

        /**
         * ReLU导数
         */
        public static final Function<Double, Double> RELU_DERIVATIVE = z -> z > 0 ? 1.0 : 0.0;

        /**
         * Leaky ReLU激活函数
         */
        public static final Function<Double, Double> LEAKY_RELU = z -> z > 0 ? z : 0.01 * z;

        /**
         * Leaky ReLU导数
         */
        public static final Function<Double, Double> LEAKY_RELU_DERIVATIVE = z -> z > 0 ? 1.0 : 0.01;

        /**
         * Tanh激活函数
         */
        public static final Function<Double, Double> TANH = Math::tanh;

        /**
         * Tanh导数
         */
        public static final Function<Double, Double> TANH_DERIVATIVE = z -> {
            double t = Math.tanh(z);
            return 1 - t * t;
        };

        /**
         * Softmax激活函数（用于输出层）
         *
         * @param z 输入向量
         * @return 概率分布
         */
        public static double[] softmax(double[] z) {
            double[] exp = new double[z.length];
            double sum = 0.0;
            double max = Double.NEGATIVE_INFINITY;

            for (double v : z) {
                if (v > max) max = v;
            }

            for (int i = 0; i < z.length; i++) {
                exp[i] = Math.exp(z[i] - max);
                sum += exp[i];
            }

            for (int i = 0; i < z.length; i++) {
                exp[i] /= sum;
            }
            return exp;
        }
    }

    /**
     * 计算交叉熵损失
     *
     * @param predictions 预测概率分布
     * @param targets     目标one-hot编码
     * @return 交叉熵损失
     */
    public static double crossEntropyLoss(double[] predictions, double[] targets) {
        double loss = 0.0;
        for (int i = 0; i < predictions.length; i++) {
            if (targets[i] > 0) {
                loss -= targets[i] * Math.log(Math.max(predictions[i], 1e-15));
            }
        }
        return loss;
    }

    /**
     * 计算交叉熵损失梯度
     *
     * @param predictions 预测概率分布
     * @param targets     目标one-hot编码
     * @return 梯度
     */
    public static double[] crossEntropyGradient(double[] predictions, double[] targets) {
        double[] grad = new double[predictions.length];
        for (int i = 0; i < predictions.length; i++) {
            grad[i] = predictions[i] - targets[i];
        }
        return grad;
    }

    /**
     * 简单的全连接网络构建器
     *
     * @param inputSize    输入维度
     * @param hiddenSizes  隐藏层维度数组
     * @param outputSize   输出维度
     * @param learningRate 学习率
     * @return 构建好的神经网络
     */
    public static NeuralNetwork buildFCNetwork(int inputSize, int[] hiddenSizes,
                                                int outputSize, double learningRate) {
        NeuralNetwork network = new NeuralNetwork(learningRate);

        int currentSize = inputSize;
        for (int hiddenSize : hiddenSizes) {
            network.addLayer(new Layer(currentSize, hiddenSize,
                    Activations.RELU, Activations.RELU_DERIVATIVE));
            currentSize = hiddenSize;
        }

        // 输出层使用Sigmoid（用于二分类）或线性（用于回归）
        network.addLayer(new Layer(currentSize, outputSize,
                Activations.SIGMOID, Activations.SIGMOID_DERIVATIVE));

        return network;
    }

    /**
     * 训练神经网络
     *
     * @param network    神经网络
     * @param X          训练数据
     * @param y          训练标签
     * @param epochs     训练轮数
     * @param batchSize  批量大小
     * @param verbose    是否打印训练过程
     */
    public static void train(NeuralNetwork network, double[][] X, double[][] y,
                             int epochs, int batchSize, boolean verbose) {
        int nSamples = X.length;
        int nBatches = (nSamples + batchSize - 1) / batchSize;

        for (int epoch = 0; epoch < epochs; epoch++) {
            double epochLoss = 0.0;

            // 随机打乱数据
            int[] indices = shuffleIndices(nSamples);

            for (int batch = 0; batch < nBatches; batch++) {
                int start = batch * batchSize;
                int end = Math.min(start + batchSize, nSamples);
                int currentBatchSize = end - start;

                double[][] batchX = new double[currentBatchSize][];
                double[][] batchY = new double[currentBatchSize][];

                for (int i = 0; i < currentBatchSize; i++) {
                    batchX[i] = X[indices[start + i]];
                    batchY[i] = y[indices[start + i]];
                }

                epochLoss += network.trainBatch(batchX, batchY);
            }

            if (verbose && (epoch + 1) % 100 == 0) {
                System.out.println("Epoch " + (epoch + 1) + "/" + epochs +
                        ", Loss: " + String.format("%.6f", epochLoss / nBatches));
            }
        }
    }

    /**
     * 生成随机索引序列
     *
     * @param n 序列长度
     * @return 随机索引数组
     */
    private static int[] shuffleIndices(int n) {
        int[] indices = new int[n];
        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }

        // Fisher-Yates shuffle
        for (int i = n - 1; i > 0; i--) {
            int j = (int) (Math.random() * (i + 1));
            int temp = indices[i];
            indices[i] = indices[j];
            indices[j] = temp;
        }

        return indices;
    }
}
