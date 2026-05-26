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
 * 机器学习算法测试类
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class AlgorithmTest {

    public static void main(String[] args) {
        System.out.println("=== Feat AI 算法测试 ===\n");

        testLinearRegression();
        testLogisticRegression();
        testMSELoss();
        testGradientDescent();
        testBackPropagation();

        System.out.println("\n=== 所有测试完成 ===");
    }

    /**
     * 测试线性回归
     */
    static void testLinearRegression() {
        System.out.println("--- 线性回归测试 ---");

        // 生成简单数据: y = 2x + 1
        double[][] X = new double[100][1];
        double[] y = new double[100];

        for (int i = 0; i < 100; i++) {
            X[i][0] = i;
            y[i] = 2 * i + 1 + (Math.random() - 0.5) * 10;  // 添加噪声
        }

        // 使用梯度下降训练
        LinearRegression model = new LinearRegression(0.0001, 5000);
        model.fit(X, y);

        System.out.println("梯度下降结果:");
        System.out.println("  权重: " + model.getWeights()[0]);
        System.out.println("  偏置: " + model.getBias());
        System.out.println("  MSE: " + model.score(X, y));

        // 使用正规方程训练
        LinearRegression model2 = new LinearRegression();
        model2.fitNormalEquation(X, y);

        System.out.println("正规方程结果:");
        System.out.println("  权重: " + model2.getWeights()[0]);
        System.out.println("  偏置: " + model2.getBias());
        System.out.println("  MSE: " + model2.score(X, y));

        // 预测
        double[][] testX = {{50}, {100}};
        double[] predictions = model2.predict(testX);
        System.out.println("预测结果 (x=50, x=100): " + predictions[0] + ", " + predictions[1]);
        System.out.println();
    }

    /**
     * 测试逻辑回归
     */
    static void testLogisticRegression() {
        System.out.println("--- 逻辑回归测试 ---");

        // 生成二分类数据
        double[][] X = new double[200][2];
        double[] y = new double[200];

        // 类别0: 中心在(2, 2)
        for (int i = 0; i < 100; i++) {
            X[i][0] = 2 + (Math.random() - 0.5) * 2;
            X[i][1] = 2 + (Math.random() - 0.5) * 2;
            y[i] = 0;
        }

        // 类别1: 中心在(6, 6)
        for (int i = 100; i < 200; i++) {
            X[i][0] = 6 + (Math.random() - 0.5) * 2;
            X[i][1] = 6 + (Math.random() - 0.5) * 2;
            y[i] = 1;
        }

        LogisticRegression model = new LogisticRegression(0.1, 1000, 0.01);
        model.fit(X, y);

        System.out.println("权重: [" + model.getWeights()[0] + ", " + model.getWeights()[1] + "]");
        System.out.println("偏置: " + model.getBias());
        System.out.println("准确率: " + model.score(X, y));
        System.out.println("Log Loss: " + model.logLoss(X, y));

        // 预测
        double[][] testX = {{2, 2}, {6, 6}, {4, 4}};
        int[] predictions = model.predict(testX);
        double[] probs = model.predictProbability(testX);
        System.out.println("预测结果:");
        for (int i = 0; i < testX.length; i++) {
            System.out.println("  (" + testX[i][0] + ", " + testX[i][1] + ") => 类别: " +
                    predictions[i] + ", 概率: " + probs[i]);
        }
        System.out.println();
    }

    /**
     * 测试MSE损失函数
     */
    static void testMSELoss() {
        System.out.println("--- MSE损失函数测试 ---");

        double[] predictions = {1.5, 2.3, 3.1, 4.8, 5.2};
        double[] targets = {1.0, 2.0, 3.0, 5.0, 5.0};

        MSELoss loss = new MSELoss();
        double mse = loss.compute(predictions, targets);
        double rmse = MSELoss.rmse(predictions, targets);
        double mae = MSELoss.mae(predictions, targets);
        double r2 = MSELoss.r2Score(predictions, targets);

        System.out.println("预测值: [1.5, 2.3, 3.1, 4.8, 5.2]");
        System.out.println("真实值: [1.0, 2.0, 3.0, 5.0, 5.0]");
        System.out.println("MSE: " + mse);
        System.out.println("RMSE: " + rmse);
        System.out.println("MAE: " + mae);
        System.out.println("R²: " + r2);

        // 测试梯度
        double[] grad = loss.gradient(predictions, targets);
        System.out.print("梯度: [");
        for (int i = 0; i < grad.length; i++) {
            System.out.print(String.format("%.4f", grad[i]));
            if (i < grad.length - 1) System.out.print(", ");
        }
        System.out.println("]");

        // 测试Huber损失
        double huber = MSELoss.huberLoss(predictions, targets, 0.5);
        System.out.println("Huber Loss (delta=0.5): " + huber);
        System.out.println();
    }

    /**
     * 测试梯度下降
     */
    static void testGradientDescent() {
        System.out.println("--- 梯度下降测试 ---");

        // 最小化 f(x, y) = x² + 2y²，最优解在 (0, 0)

        // 定义损失函数
        java.util.function.Function<double[], Double> lossFunc = params -> {
            return params[0] * params[0] + 2 * params[1] * params[1];
        };

        // 定义梯度函数
        java.util.function.Function<double[], double[]> gradFunc = params -> {
            return new double[]{2 * params[0], 4 * params[1]};
        };

        double[] initialParams = {5.0, 3.0};
        System.out.println("初始参数: [5.0, 3.0]");
        System.out.println("目标函数: f(x, y) = x² + 2y²");
        System.out.println("理论最优解: [0.0, 0.0]");

        // 批量梯度下降
        GradientDescent gd = new GradientDescent(GradientDescent.Type.BATCH, 0.1, 100);
        double[] result = gd.optimize(initialParams.clone(), lossFunc, gradFunc);
        System.out.println("批量GD结果: [" + result[0] + ", " + result[1] + "]");

        // Momentum
        GradientDescent momentum = new GradientDescent(GradientDescent.Type.MOMENTUM, 0.1, 100)
                .momentum(0.9);
        result = momentum.optimize(initialParams.clone(), lossFunc, gradFunc);
        System.out.println("Momentum结果: [" + result[0] + ", " + result[1] + "]");

        // Adam
        GradientDescent adam = new GradientDescent(GradientDescent.Type.ADAM, 0.1, 100);
        result = adam.optimize(initialParams.clone(), lossFunc, gradFunc);
        System.out.println("Adam结果: [" + result[0] + ", " + result[1] + "]");

        // 数值梯度验证
        double[] numericalGrad = GradientDescent.numericalGradient(lossFunc, new double[]{1.0, 1.0}, 1e-5);
        System.out.println("数值梯度在(1,1): [" + numericalGrad[0] + ", " + numericalGrad[1] + "]");
        System.out.println("解析梯度在(1,1): [2.0, 4.0]");
        System.out.println();
    }

    /**
     * 测试反向传播
     */
    static void testBackPropagation() {
        System.out.println("--- 反向传播测试 ---");

        // 构建一个简单的神经网络: 2 -> 4 -> 1
        // 用于学习 XOR 问题
        BackPropagation.NeuralNetwork network = new BackPropagation.NeuralNetwork(0.5);
        network.addLayer(new BackPropagation.Layer(2, 4,
                BackPropagation.Activations.SIGMOID,
                BackPropagation.Activations.SIGMOID_DERIVATIVE));
        network.addLayer(new BackPropagation.Layer(4, 1,
                BackPropagation.Activations.SIGMOID,
                BackPropagation.Activations.SIGMOID_DERIVATIVE));

        // XOR 训练数据
        double[][] X = {
                {0, 0},
                {0, 1},
                {1, 0},
                {1, 1}
        };
        double[][] y = {
                {0},
                {1},
                {1},
                {0}
        };

        System.out.println("训练 XOR 问题...");
        System.out.println("网络结构: 2 -> 4 -> 1");

        // 训练前预测
        System.out.println("\n训练前预测:");
        for (int i = 0; i < X.length; i++) {
            double[] pred = network.predict(X[i]);
            System.out.println("  " + (int)X[i][0] + " XOR " + (int)X[i][1] + " = " + pred[0]);
        }

        // 训练
        BackPropagation.train(network, X, y, 5000, 4, false);

        // 训练后预测
        System.out.println("\n训练后预测:");
        for (int i = 0; i < X.length; i++) {
            double[] pred = network.predict(X[i]);
            System.out.println("  " + (int)X[i][0] + " XOR " + (int)X[i][1] + " = " +
                    String.format("%.4f", pred[0]) + " (目标: " + (int)y[i][0] + ")");
        }

        // 测试交叉熵损失
        double[] predProbs = {0.8, 0.2, 0.1};
        double[] trueLabels = {1.0, 0.0, 0.0};
        double ceLoss = BackPropagation.crossEntropyLoss(predProbs, trueLabels);
        System.out.println("\n交叉熵损失测试: " + ceLoss);
        System.out.println();
    }
}
