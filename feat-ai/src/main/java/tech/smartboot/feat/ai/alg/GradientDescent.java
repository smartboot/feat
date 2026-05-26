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

import java.util.function.Function;

/**
 * 梯度下降算法实现
 * <p>
 * 梯度下降是一种优化算法，用于最小化损失函数。通过计算损失函数对参数的梯度，
 * 并沿着梯度的反方向更新参数，逐步逼近最优解。
 * </p>
 *
 * <p>支持多种变体：</p>
 * <ul>
 *   <li>批量梯度下降 (Batch GD)</li>
 *   <li>随机梯度下降 (SGD)</li>
 *   <li>小批量梯度下降 (Mini-batch GD)</li>
 *   <li>Momentum 动量法</li>
 *   <li>AdaGrad 自适应学习率</li>
 *   <li>RMSProp</li>
 *   <li>Adam 优化器</li>
 * </ul>
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class GradientDescent {

    /**
     * 梯度下降类型
     */
    public enum Type {
        BATCH,      // 批量梯度下降
        SGD,        // 随机梯度下降
        MINI_BATCH, // 小批量梯度下降
        MOMENTUM,   // 动量法
        ADAGRAD,    // AdaGrad
        RMSPROP,    // RMSProp
        ADAM        // Adam
    }

    /**
     * 学习率
     */
    private double learningRate;

    /**
     * 迭代次数
     */
    private int iterations;

    /**
     * 梯度下降类型
     */
    private Type type;

    /**
     * 小批量大小（用于Mini-batch）
     */
    private int batchSize;

    /**
     * 动量系数（用于Momentum）
     */
    private double momentum;

    /**
     * 衰减率（用于RMSProp和Adam）
     */
    private double beta1;

    /**
     * 二阶矩衰减率（用于Adam）
     */
    private double beta2;

    /**
     * 数值稳定性常数
     */
    private double epsilon;

    /**
     * 默认构造函数（批量梯度下降）
     */
    public GradientDescent() {
        this(Type.BATCH, 0.01, 1000);
    }

    /**
     * 构造函数
     *
     * @param type         梯度下降类型
     * @param learningRate 学习率
     * @param iterations   迭代次数
     */
    public GradientDescent(Type type, double learningRate, int iterations) {
        this.type = type;
        this.learningRate = learningRate;
        this.iterations = iterations;
        this.batchSize = 32;
        this.momentum = 0.9;
        this.beta1 = 0.9;
        this.beta2 = 0.999;
        this.epsilon = 1e-8;
    }

    /**
     * 设置小批量大小
     *
     * @param batchSize 批量大小
     * @return 当前实例
     */
    public GradientDescent batchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    /**
     * 设置动量系数
     *
     * @param momentum 动量系数
     * @return 当前实例
     */
    public GradientDescent momentum(double momentum) {
        this.momentum = momentum;
        return this;
    }

    /**
     * 设置Adam参数
     *
     * @param beta1 一阶矩衰减率
     * @param beta2 二阶矩衰减率
     * @return 当前实例
     */
    public GradientDescent adamParams(double beta1, double beta2) {
        this.beta1 = beta1;
        this.beta2 = beta2;
        return this;
    }

    /**
     * 执行梯度下降优化
     *
     * @param initialParams 初始参数
     * @param lossFunction  损失函数
     * @param gradientFunc  梯度计算函数
     * @return 优化后的参数
     */
    public double[] optimize(double[] initialParams,
                             Function<double[], Double> lossFunction,
                             Function<double[], double[]> gradientFunc) {
        return optimize(initialParams, lossFunction, gradientFunc, null);
    }

    /**
     * 执行梯度下降优化（带数据）
     *
     * @param initialParams 初始参数
     * @param lossFunction  损失函数
     * @param gradientFunc  梯度计算函数
     * @param data          训练数据
     * @return 优化后的参数
     */
    public double[] optimize(double[] initialParams,
                             Function<double[], Double> lossFunction,
                             Function<double[], double[]> gradientFunc,
                             double[][] data) {
        double[] params = initialParams.clone();
        int nParams = params.length;

        // 初始化优化器状态
        double[] velocity = new double[nParams];      // 用于Momentum
        double[] squaredGrad = new double[nParams];   // 用于AdaGrad/RMSProp
        double[] m = new double[nParams];           // 用于Adam（一阶矩）
        double[] v = new double[nParams];           // 用于Adam（二阶矩）

        for (int iter = 0; iter < iterations; iter++) {
            double[] gradient;

            switch (type) {
                case BATCH:
                    gradient = gradientFunc.apply(params);
                    params = updateBatch(params, gradient);
                    break;

                case SGD:
                    gradient = computeStochasticGradient(params, data, gradientFunc);
                    params = updateBatch(params, gradient);
                    break;

                case MINI_BATCH:
                    gradient = computeMiniBatchGradient(params, data, gradientFunc);
                    params = updateBatch(params, gradient);
                    break;

                case MOMENTUM:
                    gradient = gradientFunc.apply(params);
                    params = updateMomentum(params, gradient, velocity);
                    break;

                case ADAGRAD:
                    gradient = gradientFunc.apply(params);
                    params = updateAdaGrad(params, gradient, squaredGrad);
                    break;

                case RMSPROP:
                    gradient = gradientFunc.apply(params);
                    params = updateRMSProp(params, gradient, squaredGrad);
                    break;

                case ADAM:
                    gradient = gradientFunc.apply(params);
                    params = updateAdam(params, gradient, m, v, iter + 1);
                    break;

                default:
                    gradient = gradientFunc.apply(params);
                    params = updateBatch(params, gradient);
            }
        }

        return params;
    }

    /**
     * 批量梯度下降更新
     */
    private double[] updateBatch(double[] params, double[] gradient) {
        double[] newParams = new double[params.length];
        for (int i = 0; i < params.length; i++) {
            newParams[i] = params[i] - learningRate * gradient[i];
        }
        return newParams;
    }

    /**
     * 动量法更新
     */
    private double[] updateMomentum(double[] params, double[] gradient, double[] velocity) {
        double[] newParams = new double[params.length];
        for (int i = 0; i < params.length; i++) {
            velocity[i] = momentum * velocity[i] + learningRate * gradient[i];
            newParams[i] = params[i] - velocity[i];
        }
        return newParams;
    }

    /**
     * AdaGrad更新
     */
    private double[] updateAdaGrad(double[] params, double[] gradient, double[] squaredGrad) {
        double[] newParams = new double[params.length];
        for (int i = 0; i < params.length; i++) {
            squaredGrad[i] += gradient[i] * gradient[i];
            newParams[i] = params[i] - learningRate * gradient[i] / (Math.sqrt(squaredGrad[i]) + epsilon);
        }
        return newParams;
    }

    /**
     * RMSProp更新
     */
    private double[] updateRMSProp(double[] params, double[] gradient, double[] squaredGrad) {
        double[] newParams = new double[params.length];
        for (int i = 0; i < params.length; i++) {
            squaredGrad[i] = beta2 * squaredGrad[i] + (1 - beta2) * gradient[i] * gradient[i];
            newParams[i] = params[i] - learningRate * gradient[i] / (Math.sqrt(squaredGrad[i]) + epsilon);
        }
        return newParams;
    }

    /**
     * Adam更新
     */
    private double[] updateAdam(double[] params, double[] gradient, double[] m, double[] v, int t) {
        double[] newParams = new double[params.length];
        for (int i = 0; i < params.length; i++) {
            // 更新一阶矩估计
            m[i] = beta1 * m[i] + (1 - beta1) * gradient[i];
            // 更新二阶矩估计
            v[i] = beta2 * v[i] + (1 - beta2) * gradient[i] * gradient[i];

            // 偏差修正
            double mHat = m[i] / (1 - Math.pow(beta1, t));
            double vHat = v[i] / (1 - Math.pow(beta2, t));

            newParams[i] = params[i] - learningRate * mHat / (Math.sqrt(vHat) + epsilon);
        }
        return newParams;
    }

    /**
     * 计算随机梯度（单个样本）
     */
    private double[] computeStochasticGradient(double[] params, double[][] data,
                                                Function<double[], double[]> gradientFunc) {
        // 随机选择一个样本
        int idx = (int) (Math.random() * data.length);
        double[] sample = data[idx];
        return gradientFunc.apply(combineParamsAndSample(params, sample));
    }

    /**
     * 计算小批量梯度
     */
    private double[] computeMiniBatchGradient(double[] params, double[][] data,
                                               Function<double[], double[]> gradientFunc) {
        double[] batchGradient = new double[params.length];
        int actualBatchSize = Math.min(batchSize, data.length);

        // 随机选择小批量样本
        for (int i = 0; i < actualBatchSize; i++) {
            int idx = (int) (Math.random() * data.length);
            double[] sample = data[idx];
            double[] grad = gradientFunc.apply(combineParamsAndSample(params, sample));
            for (int j = 0; j < batchGradient.length; j++) {
                batchGradient[j] += grad[j];
            }
        }

        // 平均梯度
        for (int j = 0; j < batchGradient.length; j++) {
            batchGradient[j] /= actualBatchSize;
        }

        return batchGradient;
    }

    /**
     * 组合参数和样本（辅助方法）
     */
    private double[] combineParamsAndSample(double[] params, double[] sample) {
        double[] combined = new double[params.length + sample.length];
        System.arraycopy(params, 0, combined, 0, params.length);
        System.arraycopy(sample, 0, combined, params.length, sample.length);
        return combined;
    }

    /**
     * 学习率衰减
     *
     * @param initialRate 初始学习率
     * @param decayRate   衰减率
     * @param epoch       当前轮次
     * @return 衰减后的学习率
     */
    public static double learningRateDecay(double initialRate, double decayRate, int epoch) {
        return initialRate / (1 + decayRate * epoch);
    }

    /**
     * 指数学习率衰减
     *
     * @param initialRate 初始学习率
     * @param decayRate   衰减率
     * @param epoch       当前轮次
     * @return 衰减后的学习率
     */
    public static double exponentialDecay(double initialRate, double decayRate, int epoch) {
        return initialRate * Math.exp(-decayRate * epoch);
    }

    /**
     * 步进学习率衰减
     *
     * @param initialRate 初始学习率
     * @param dropRate    下降比例
     * @param epoch       当前轮次
     * @param epochsDrop  每多少轮下降一次
     * @return 衰减后的学习率
     */
    public static double stepDecay(double initialRate, double dropRate, int epoch, int epochsDrop) {
        return initialRate * Math.pow(dropRate, Math.floor((double) epoch / epochsDrop));
    }

    /**
     * 数值梯度计算（用于验证）
     *
     * @param func   目标函数
     * @param params 参数点
     * @param epsilon 微小增量
     * @return 数值梯度
     */
    public static double[] numericalGradient(Function<double[], Double> func,
                                              double[] params,
                                              double epsilon) {
        double[] gradient = new double[params.length];
        double f0 = func.apply(params);

        for (int i = 0; i < params.length; i++) {
            double[] paramsPlus = params.clone();
            paramsPlus[i] += epsilon;
            double fPlus = func.apply(paramsPlus);
            gradient[i] = (fPlus - f0) / epsilon;
        }

        return gradient;
    }
}
