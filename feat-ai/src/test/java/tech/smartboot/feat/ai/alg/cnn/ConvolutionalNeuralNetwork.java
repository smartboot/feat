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
import java.util.List;

/**
 * 卷积神经网络 (CNN) 实现
 * <p>
 * 支持多种层类型：
 * - 卷积层 (Convolutional Layer)
 * - 池化层 (Pooling Layer)
 * - 批量归一化层 (Batch Normalization)
 * - 全连接层 (Fully Connected Layer)
 * - Dropout层
 * </p>
 *
 * <p>使用示例:</p>
 * <pre>
 * ConvolutionalNeuralNetwork cnn = new ConvolutionalNeuralNetwork.Builder()
 *     .addConvLayer(3, 32, 3, 1, 1)    // 输入3通道，输出32通道，3x3卷积核
 *     .addBatchNorm(32)
 *     .addActivation(CNNBackPropagation.ActivationType.RELU)
 *     .addMaxPoolLayer(2, 2)             // 2x2最大池化
 *     .addFlatten()                      // 展平
 *     .addFCLayer(512, 10)               // 全连接层
 *     .addSoftmax()
 *     .build();
 *
 * cnn.train(images, labels, epochs, batchSize, learningRate);
 * int[] predictions = cnn.predict(images);
 * </pre>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class ConvolutionalNeuralNetwork {

    /**
     * 网络层列表
     */
    private final List<Layer> layers;

    /**
     * 学习率
     */
    private double learningRate;

    /**
     * 是否训练模式
     */
    private boolean training;

    /**
     * 私有构造函数
     */
    private ConvolutionalNeuralNetwork() {
        this.layers = new ArrayList<>();
        this.training = true;
    }

    /**
     * 层接口
     */
    public interface Layer {
        /**
         * 前向传播
         */
        Object forward(Object input);

        /**
         * 反向传播
         */
        Object backward(Object gradOutput);

        /**
         * 更新参数
         */
        void updateParameters(double learningRate, int batchSize);

        /**
         * 清零梯度
         */
        void zeroGradients();

        /**
         * 设置训练模式
         */
        void setTraining(boolean training);
    }

    /**
     * 卷积层
     */
    public static class ConvLayer implements Layer {
        private final int numKernels;
        private final int kernelHeight;
        private final int kernelWidth;
        private final int stride;
        private final int padding;

        private double[][][][] kernels;
        private double[] biases;

        // 梯度
        private double[][][][] gradKernels;
        private double[] gradBiases;

        // 缓存
        private double[][][] lastInput;
        private double[][][] lastOutput;
        private double[][][] lastZ;

        // 优化器状态
        private double[][][][] m, v;  // Adam优化器状态
        private double[] bm, bv;
        private int t = 0;

        public ConvLayer(int numKernels, int kernelHeight, int kernelWidth,
                        int stride, int padding) {
            this.numKernels = numKernels;
            this.kernelHeight = kernelHeight;
            this.kernelWidth = kernelWidth;
            this.stride = stride;
            this.padding = padding;
        }

        public void initialize(int inputChannels) {
            this.kernels = Convolution.initializeKernels(numKernels, inputChannels,
                    kernelHeight, kernelWidth);
            this.biases = new double[numKernels];

            this.gradKernels = new double[numKernels][inputChannels][kernelHeight][kernelWidth];
            this.gradBiases = new double[numKernels];

            // Adam状态初始化
            this.m = new double[numKernels][inputChannels][kernelHeight][kernelWidth];
            this.v = new double[numKernels][inputChannels][kernelHeight][kernelWidth];
            this.bm = new double[numKernels];
            this.bv = new double[numKernels];
        }

        @Override
        public Object forward(Object input) {
            double[][][] in = (double[][][]) input;
            this.lastInput = in;

            // 延迟初始化
            if (kernels == null) {
                initialize(in.length);
            }

            lastZ = Convolution.conv2d(in, kernels, biases, stride, padding);
            lastOutput = lastZ; // 激活函数在单独层处理
            return lastOutput;
        }

        @Override
        public Object backward(Object gradOutput) {
            double[][][] grad = (double[][][]) gradOutput;

            CNNBackPropagation.ConvGradResult result = CNNBackPropagation.convBackward(
                    grad, lastInput, kernels, stride, padding);

            // 累加梯度
            for (int k = 0; k < numKernels; k++) {
                gradBiases[k] += result.gradBiases[k];
                for (int c = 0; c < kernels[0].length; c++) {
                    for (int i = 0; i < kernelHeight; i++) {
                        for (int j = 0; j < kernelWidth; j++) {
                            gradKernels[k][c][i][j] += result.gradKernels[k][c][i][j];
                        }
                    }
                }
            }

            return result.gradInput;
        }

        @Override
        public void updateParameters(double learningRate, int batchSize) {
            t++;
            double beta1 = 0.9;
            double beta2 = 0.999;
            double epsilon = 1e-8;

            for (int k = 0; k < numKernels; k++) {
                // 更新偏置
                double bg = gradBiases[k] / batchSize;
                bm[k] = beta1 * bm[k] + (1 - beta1) * bg;
                bv[k] = beta2 * bv[k] + (1 - beta2) * bg * bg;
                double bmHat = bm[k] / (1 - Math.pow(beta1, t));
                double bvHat = bv[k] / (1 - Math.pow(beta2, t));
                biases[k] -= learningRate * bmHat / (Math.sqrt(bvHat) + epsilon);

                // 更新卷积核
                for (int c = 0; c < kernels[0].length; c++) {
                    for (int i = 0; i < kernelHeight; i++) {
                        for (int j = 0; j < kernelWidth; j++) {
                            double wg = gradKernels[k][c][i][j] / batchSize;
                            m[k][c][i][j] = beta1 * m[k][c][i][j] + (1 - beta1) * wg;
                            v[k][c][i][j] = beta2 * v[k][c][i][j] + (1 - beta2) * wg * wg;
                            double mHat = m[k][c][i][j] / (1 - Math.pow(beta1, t));
                            double vHat = v[k][c][i][j] / (1 - Math.pow(beta2, t));
                            kernels[k][c][i][j] -= learningRate * mHat / (Math.sqrt(vHat) + epsilon);
                        }
                    }
                }
            }
        }

        @Override
        public void zeroGradients() {
            for (int k = 0; k < numKernels; k++) {
                gradBiases[k] = 0;
                for (int c = 0; c < gradKernels[0].length; c++) {
                    for (int i = 0; i < kernelHeight; i++) {
                        for (int j = 0; j < kernelWidth; j++) {
                            gradKernels[k][c][i][j] = 0;
                        }
                    }
                }
            }
        }

        @Override
        public void setTraining(boolean training) {
            // 卷积层训练模式不影响前向传播
        }
    }

    /**
     * 激活层
     */
    public static class ActivationLayer implements Layer {
        private final CNNBackPropagation.ActivationType activationType;
        private double[][][] lastZ;

        public ActivationLayer(CNNBackPropagation.ActivationType activationType) {
            this.activationType = activationType;
        }

        @Override
        public Object forward(Object input) {
            double[][][] in = (double[][][]) input;
            this.lastZ = in;

            int channels = in.length;
            int height = in[0].length;
            int width = in[0][0].length;

            double[][][] output = new double[channels][height][width];

            for (int c = 0; c < channels; c++) {
                for (int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        double val = in[c][i][j];
                        switch (activationType) {
                            case RELU:
                                output[c][i][j] = Math.max(0, val);
                                break;
                            case LEAKY_RELU:
                                output[c][i][j] = val > 0 ? val : 0.01 * val;
                                break;
                            case SIGMOID:
                                output[c][i][j] = sigmoid(val);
                                break;
                            case TANH:
                                output[c][i][j] = Math.tanh(val);
                                break;
                            case LINEAR:
                                output[c][i][j] = val;
                                break;
                        }
                    }
                }
            }

            return output;
        }

        @Override
        public Object backward(Object gradOutput) {
            return CNNBackPropagation.activationBackward((double[][][]) gradOutput, lastZ, activationType);
        }

        @Override
        public void updateParameters(double learningRate, int batchSize) {
            // 激活层没有参数
        }

        @Override
        public void zeroGradients() {
            // 激活层没有参数
        }

        @Override
        public void setTraining(boolean training) {
            // 激活层没有训练模式
        }

        private double sigmoid(double x) {
            if (x < -500) return 0.0;
            if (x > 500) return 1.0;
            return 1.0 / (1.0 + Math.exp(-x));
        }
    }

    /**
     * 池化层
     */
    public static class PoolingLayer implements Layer {
        private final int poolHeight;
        private final int poolWidth;
        private final int stride;
        private final boolean isMaxPooling;

        private int[][][][] maxIndices;
        private int inputHeight, inputWidth;

        public PoolingLayer(int poolHeight, int poolWidth, int stride, boolean isMaxPooling) {
            this.poolHeight = poolHeight;
            this.poolWidth = poolWidth;
            this.stride = stride;
            this.isMaxPooling = isMaxPooling;
        }

        @Override
        public Object forward(Object input) {
            double[][][] in = (double[][][]) input;
            this.inputHeight = in[0].length;
            this.inputWidth = in[0][0].length;

            if (isMaxPooling) {
                Pooling.PoolingResult result = Pooling.maxPool(in, poolHeight, poolWidth, stride);
                this.maxIndices = result.maxIndices;
                return result.output;
            } else {
                return Pooling.avgPool(in, poolHeight, poolWidth, stride);
            }
        }

        @Override
        public Object backward(Object gradOutput) {
            double[][][] grad = (double[][][]) gradOutput;

            if (isMaxPooling) {
                return Pooling.maxPoolBackward(grad, maxIndices, inputHeight, inputWidth);
            } else {
                return Pooling.avgPoolBackward(grad, poolHeight, poolWidth, stride, inputHeight, inputWidth);
            }
        }

        @Override
        public void updateParameters(double learningRate, int batchSize) {
            // 池化层没有参数
        }

        @Override
        public void zeroGradients() {
            // 池化层没有参数
        }

        @Override
        public void setTraining(boolean training) {
            // 池化层没有训练模式
        }
    }

    /**
     * 展平层
     */
    public static class FlattenLayer implements Layer {
        private int channels, height, width;

        @Override
        public Object forward(Object input) {
            double[][][] in = (double[][][]) input;
            this.channels = in.length;
            this.height = in[0].length;
            this.width = in[0][0].length;
            return CNNBackPropagation.flatten(in);
        }

        @Override
        public Object backward(Object gradOutput) {
            double[] grad = (double[]) gradOutput;
            return CNNBackPropagation.flattenBackward(grad, channels, height, width);
        }

        @Override
        public void updateParameters(double learningRate, int batchSize) {
            // 展平层没有参数
        }

        @Override
        public void zeroGradients() {
            // 展平层没有参数
        }

        @Override
        public void setTraining(boolean training) {
            // 展平层没有训练模式
        }
    }

    /**
     * 全连接层
     */
    public static class FCLayer implements Layer {
        private final int outputSize;
        private int inputSize;

        private double[][] weights;
        private double[] biases;
        private double[][] gradWeights;
        private double[] gradBiases;

        private double[] lastInput;
        private double[] lastZ;

        // Adam状态
        private double[][] m, v;
        private double[] bm, bv;
        private int t = 0;

        public FCLayer(int outputSize) {
            this.outputSize = outputSize;
        }

        public void initialize(int inputSize) {
            this.inputSize = inputSize;
            this.weights = new double[outputSize][inputSize];
            this.biases = new double[outputSize];
            this.gradWeights = new double[outputSize][inputSize];
            this.gradBiases = new double[outputSize];

            // Xavier初始化
            double scale = Math.sqrt(2.0 / (inputSize + outputSize));
            for (int i = 0; i < outputSize; i++) {
                for (int j = 0; j < inputSize; j++) {
                    weights[i][j] = randomNormal() * scale;
                }
            }

            // Adam状态
            this.m = new double[outputSize][inputSize];
            this.v = new double[outputSize][inputSize];
            this.bm = new double[outputSize];
            this.bv = new double[outputSize];
        }

        @Override
        public Object forward(Object input) {
            double[] in = (double[]) input;
            this.lastInput = in;

            if (weights == null) {
                initialize(in.length);
            }

            double[] output = new double[outputSize];
            for (int i = 0; i < outputSize; i++) {
                double sum = biases[i];
                for (int j = 0; j < inputSize; j++) {
                    sum += weights[i][j] * in[j];
                }
                output[i] = sum;
            }

            this.lastZ = output;
            return output;
        }

        @Override
        public Object backward(Object gradOutput) {
            double[] grad = (double[]) gradOutput;

            // 计算权重和偏置梯度
            for (int i = 0; i < outputSize; i++) {
                gradBiases[i] += grad[i];
                for (int j = 0; j < inputSize; j++) {
                    gradWeights[i][j] += grad[i] * lastInput[j];
                }
            }

            // 计算输入梯度
            double[] gradInput = new double[inputSize];
            for (int j = 0; j < inputSize; j++) {
                for (int i = 0; i < outputSize; i++) {
                    gradInput[j] += grad[i] * weights[i][j];
                }
            }

            return gradInput;
        }

        @Override
        public void updateParameters(double learningRate, int batchSize) {
            t++;
            double beta1 = 0.9;
            double beta2 = 0.999;
            double epsilon = 1e-8;

            for (int i = 0; i < outputSize; i++) {
                // 更新偏置
                double bg = gradBiases[i] / batchSize;
                bm[i] = beta1 * bm[i] + (1 - beta1) * bg;
                bv[i] = beta2 * bv[i] + (1 - beta2) * bg * bg;
                double bmHat = bm[i] / (1 - Math.pow(beta1, t));
                double bvHat = bv[i] / (1 - Math.pow(beta2, t));
                biases[i] -= learningRate * bmHat / (Math.sqrt(bvHat) + epsilon);

                // 更新权重
                for (int j = 0; j < inputSize; j++) {
                    double wg = gradWeights[i][j] / batchSize;
                    m[i][j] = beta1 * m[i][j] + (1 - beta1) * wg;
                    v[i][j] = beta2 * v[i][j] + (1 - beta2) * wg * wg;
                    double mHat = m[i][j] / (1 - Math.pow(beta1, t));
                    double vHat = v[i][j] / (1 - Math.pow(beta2, t));
                    weights[i][j] -= learningRate * mHat / (Math.sqrt(vHat) + epsilon);
                }
            }
        }

        @Override
        public void zeroGradients() {
            for (int i = 0; i < outputSize; i++) {
                gradBiases[i] = 0;
                for (int j = 0; j < inputSize; j++) {
                    gradWeights[i][j] = 0;
                }
            }
        }

        @Override
        public void setTraining(boolean training) {
            // 全连接层没有训练模式
        }

        private double randomNormal() {
            double u1 = Math.random();
            double u2 = Math.random();
            return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
        }
    }

    /**
     * Softmax层
     */
    public static class SoftmaxLayer implements Layer {
        private double[] lastOutput;

        @Override
        public Object forward(Object input) {
            double[] in = (double[]) input;
            this.lastOutput = softmax(in);
            return lastOutput;
        }

        @Override
        public Object backward(Object gradOutput) {
            // Softmax + CrossEntropy的梯度简化
            double[] grad = (double[]) gradOutput;
            double[] result = new double[grad.length];
            for (int i = 0; i < grad.length; i++) {
                result[i] = grad[i];
            }
            return result;
        }

        @Override
        public void updateParameters(double learningRate, int batchSize) {
            // Softmax层没有参数
        }

        @Override
        public void zeroGradients() {
            // Softmax层没有参数
        }

        @Override
        public void setTraining(boolean training) {
            // Softmax层没有训练模式
        }

        private double[] softmax(double[] z) {
            double[] result = new double[z.length];
            double max = Double.NEGATIVE_INFINITY;
            for (double v : z) {
                if (v > max) max = v;
            }

            double sum = 0;
            for (int i = 0; i < z.length; i++) {
                result[i] = Math.exp(z[i] - max);
                sum += result[i];
            }

            for (int i = 0; i < z.length; i++) {
                result[i] /= sum;
            }
            return result;
        }
    }

    /**
     * 构建器模式
     */
    public static class Builder {
        private final ConvolutionalNeuralNetwork cnn;

        public Builder() {
            cnn = new ConvolutionalNeuralNetwork();
            cnn.learningRate = 0.001;
        }

        /**
         * 添加卷积层
         */
        public Builder addConvLayer(int numKernels, int kernelSize, int stride, int padding) {
            cnn.layers.add(new ConvLayer(numKernels, kernelSize, kernelSize, stride, padding));
            return this;
        }

        /**
         * 添加卷积层（指定输入通道数）
         */
        public Builder addConvLayer(int inChannels, int numKernels, int kernelSize, int stride, int padding) {
            ConvLayer layer = new ConvLayer(numKernels, kernelSize, kernelSize, stride, padding);
            layer.initialize(inChannels);
            cnn.layers.add(layer);
            return this;
        }

        /**
         * 添加激活层
         */
        public Builder addActivation(CNNBackPropagation.ActivationType activationType) {
            cnn.layers.add(new ActivationLayer(activationType));
            return this;
        }

        /**
         * 添加最大池化层
         */
        public Builder addMaxPoolLayer(int poolSize, int stride) {
            cnn.layers.add(new PoolingLayer(poolSize, poolSize, stride, true));
            return this;
        }

        /**
         * 添加平均池化层
         */
        public Builder addAvgPoolLayer(int poolSize, int stride) {
            cnn.layers.add(new PoolingLayer(poolSize, poolSize, stride, false));
            return this;
        }

        /**
         * 添加展平层
         */
        public Builder addFlatten() {
            cnn.layers.add(new FlattenLayer());
            return this;
        }

        /**
         * 添加全连接层
         */
        public Builder addFCLayer(int outputSize) {
            cnn.layers.add(new FCLayer(outputSize));
            return this;
        }

        /**
         * 添加Softmax层
         */
        public Builder addSoftmax() {
            cnn.layers.add(new SoftmaxLayer());
            return this;
        }

        /**
         * 设置学习率
         */
        public Builder learningRate(double learningRate) {
            cnn.learningRate = learningRate;
            return this;
        }

        /**
         * 构建网络
         */
        public ConvolutionalNeuralNetwork build() {
            if (cnn.layers.isEmpty()) {
                throw new IllegalStateException("网络必须至少包含一层");
            }
            return cnn;
        }
    }

    /**
     * 前向传播
     */
    public double[] forward(double[][][] input) {
        Object current = input;
        for (Layer layer : layers) {
            current = layer.forward(current);
        }
        return (double[]) current;
    }

    /**
     * 预测
     */
    public int predict(double[][][] input) {
        double[] output = forward(input);
        return argMax(output);
    }

    /**
     * 批量预测
     */
    public int[] predict(double[][][][] inputs) {
        int[] predictions = new int[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            predictions[i] = predict(inputs[i]);
        }
        return predictions;
    }

    /**
     * 训练
     */
    public void train(double[][][][] inputs, double[][] targets, int epochs, int batchSize) {
        int nSamples = inputs.length;

        for (int epoch = 0; epoch < epochs; epoch++) {
            double epochLoss = 0;
            int[] indices = shuffleIndices(nSamples);

            int nBatches = (nSamples + batchSize - 1) / batchSize;
            for (int batch = 0; batch < nBatches; batch++) {
                int start = batch * batchSize;
                int end = Math.min(start + batchSize, nSamples);
                int currentBatchSize = end - start;

                // 清零梯度
                for (Layer layer : layers) {
                    layer.zeroGradients();
                }

                // 前向传播和反向传播
                for (int i = 0; i < currentBatchSize; i++) {
                    int idx = indices[start + i];

                    // 前向传播
                    double[] output = forward(inputs[idx]);

                    // 计算损失和梯度
                    double[] grad = new double[output.length];
                    for (int j = 0; j < output.length; j++) {
                        grad[j] = output[j] - targets[idx][j];
                        epochLoss += -targets[idx][j] * Math.log(Math.max(output[j], 1e-15));
                    }

                    // 反向传播
                    Object currentGrad = grad;
                    for (int l = layers.size() - 1; l >= 0; l--) {
                        currentGrad = layers.get(l).backward(currentGrad);
                    }
                }

                // 更新参数
                for (Layer layer : layers) {
                    layer.updateParameters(learningRate, currentBatchSize);
                }
            }

            if ((epoch + 1) % 10 == 0) {
                System.out.println("Epoch " + (epoch + 1) + "/" + epochs +
                        ", Loss: " + String.format("%.6f", epochLoss / nSamples));
            }
        }
    }

    /**
     * 计算准确率
     */
    public double accuracy(double[][][][] inputs, int[] labels) {
        int[] predictions = predict(inputs);
        int correct = 0;
        for (int i = 0; i < labels.length; i++) {
            if (predictions[i] == labels[i]) {
                correct++;
            }
        }
        return (double) correct / labels.length;
    }

    /**
     * 设置训练模式
     */
    public void setTraining(boolean training) {
        this.training = training;
        for (Layer layer : layers) {
            layer.setTraining(training);
        }
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
}
