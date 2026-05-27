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
 * CNN算法测试类
 *
 * @author 三刀 zhengjunweimail@163.com
 * @version v1.0.0
 */
public class CNNAlgorithmTest {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("=== CNN算法测试 ===\n");

        testConvolution();
        testPadding();
        testMaxPooling();
        testAvgPooling();
        testGlobalAvgPool();
        testImageResize();
        testImageNormalization();
        testImageFlip();
        testConvBackward();
        testFlatten();
        testCNNBuilder();
        testLeNet();
        testDataLoader();
        testBatchNormalization();
        testImageCrop();
        testImageRotation();
        testActivationBackward();
        testDataLoaderSplit();

        System.out.println("\n=== 测试完成 ===");
        System.out.println("通过: " + testsPassed);
        System.out.println("失败: " + testsFailed);
    }

    private static void assertEquals(String testName, double expected, double actual, double delta) {
        if (Math.abs(expected - actual) <= delta) {
            System.out.println("✓ " + testName);
            testsPassed++;
        } else {
            System.out.println("✗ " + testName + " - 期望: " + expected + ", 实际: " + actual);
            testsFailed++;
        }
    }

    private static void assertEquals(String testName, int expected, int actual) {
        if (expected == actual) {
            System.out.println("✓ " + testName);
            testsPassed++;
        } else {
            System.out.println("✗ " + testName + " - 期望: " + expected + ", 实际: " + actual);
            testsFailed++;
        }
    }

    private static void assertNotNull(String testName, Object obj) {
        if (obj != null) {
            System.out.println("✓ " + testName);
            testsPassed++;
        } else {
            System.out.println("✗ " + testName + " - 对象为null");
            testsFailed++;
        }
    }

    private static void assertTrue(String testName, boolean condition) {
        if (condition) {
            System.out.println("✓ " + testName);
            testsPassed++;
        } else {
            System.out.println("✗ " + testName + " - 条件不满足");
            testsFailed++;
        }
    }

    /**
     * 测试卷积运算
     */
    public static void testConvolution() {
        System.out.println("\n--- 测试卷积运算 ---");

        double[][][] input = new double[][][]{
            {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
            }
        };

        double[][][][] kernels = new double[][][][]{
            {
                {
                    {1, 0},
                    {0, -1}
                }
            }
        };

        double[] biases = {0};
        double[][][] output = Convolution.conv2d(input, kernels, biases, 1, 0);

        assertEquals("卷积输出通道数", 1, output.length);
        assertEquals("卷积输出高度", 2, output[0].length);
        assertEquals("卷积输出宽度", 2, output[0][0].length);
        assertEquals("卷积结果(0,0)", -4, output[0][0][0], 0.001);
    }

    /**
     * 测试填充
     */
    public static void testPadding() {
        System.out.println("\n--- 测试填充 ---");

        double[][][] input = new double[][][]{
            {
                {1, 2},
                {3, 4}
            }
        };

        double[][][] padded = Convolution.pad(input, 1);

        assertEquals("填充后通道数", 1, padded.length);
        assertEquals("填充后高度", 4, padded[0].length);
        assertEquals("填充后宽度", 4, padded[0][0].length);
        assertEquals("原值位置(1,1)", 1, padded[0][1][1], 0.001);
        assertEquals("原值位置(2,2)", 4, padded[0][2][2], 0.001);
    }

    /**
     * 测试最大池化
     */
    public static void testMaxPooling() {
        System.out.println("\n--- 测试最大池化 ---");

        double[][][] input = new double[][][]{
            {
                {1, 2, 3, 4},
                {5, 6, 7, 8},
                {9, 10, 11, 12},
                {13, 14, 15, 16}
            }
        };

        Pooling.PoolingResult result = Pooling.maxPool(input, 2, 2, 2);
        double[][][] output = result.output;

        assertEquals("池化输出通道数", 1, output.length);
        assertEquals("池化输出高度", 2, output[0].length);
        assertEquals("池化输出宽度", 2, output[0][0].length);
        assertEquals("最大值(0,0)", 6, output[0][0][0], 0.001);
        assertEquals("最大值(0,1)", 8, output[0][0][1], 0.001);
        assertEquals("最大值(1,0)", 14, output[0][1][0], 0.001);
        assertEquals("最大值(1,1)", 16, output[0][1][1], 0.001);
    }

    /**
     * 测试平均池化
     */
    public static void testAvgPooling() {
        System.out.println("\n--- 测试平均池化 ---");

        double[][][] input = new double[][][]{
            {
                {1, 2, 3, 4},
                {5, 6, 7, 8},
                {9, 10, 11, 12},
                {13, 14, 15, 16}
            }
        };

        double[][][] output = Pooling.avgPool(input, 2, 2, 2);

        assertEquals("平均池化输出高度", 2, output[0].length);
        assertEquals("平均池化输出宽度", 2, output[0][0].length);
        assertEquals("平均值(0,0)", 3.5, output[0][0][0], 0.001);
        assertEquals("平均值(0,1)", 5.5, output[0][0][1], 0.001);
    }

    /**
     * 测试全局平均池化
     */
    public static void testGlobalAvgPool() {
        System.out.println("\n--- 测试全局平均池化 ---");

        double[][][] input = new double[][][]{
            {
                {1, 2},
                {3, 4}
            },
            {
                {5, 6},
                {7, 8}
            }
        };

        double[] output = Pooling.globalAvgPool(input);

        assertEquals("全局池化输出长度", 2, output.length);
        assertEquals("通道0平均值", 2.5, output[0], 0.001);
        assertEquals("通道1平均值", 6.5, output[1], 0.001);
    }

    /**
     * 测试图像缩放
     */
    public static void testImageResize() {
        System.out.println("\n--- 测试图像缩放 ---");

        double[][][] image = new double[][][]{
            {
                {1, 2},
                {3, 4}
            }
        };

        double[][][] resized = ImagePreprocessing.resize(image, 4, 4, ImagePreprocessing.Interpolation.NEAREST);

        assertEquals("缩放后通道数", 1, resized.length);
        assertEquals("缩放后高度", 4, resized[0].length);
        assertEquals("缩放后宽度", 4, resized[0][0].length);
    }

    /**
     * 测试图像归一化
     */
    public static void testImageNormalization() {
        System.out.println("\n--- 测试图像归一化 ---");

        double[][][] image = new double[][][]{
            {
                {0, 100},
                {200, 255}
            }
        };

        double[][][] normalized = ImagePreprocessing.normalize(image, ImagePreprocessing.Normalization.MIN_MAX);

        assertEquals("归一化最小值", 0, normalized[0][0][0], 0.001);
        assertEquals("归一化最大值", 1, normalized[0][1][1], 0.001);
    }

    /**
     * 测试图像翻转
     */
    public static void testImageFlip() {
        System.out.println("\n--- 测试图像翻转 ---");

        double[][][] image = new double[][][]{
            {
                {1, 2, 3},
                {4, 5, 6}
            }
        };

        double[][][] flipped = ImagePreprocessing.horizontalFlip(image);

        assertEquals("水平翻转(0,0)", 3, flipped[0][0][0], 0.001);
        assertEquals("水平翻转(0,2)", 1, flipped[0][0][2], 0.001);
        assertEquals("水平翻转(1,0)", 6, flipped[0][1][0], 0.001);
        assertEquals("水平翻转(1,2)", 4, flipped[0][1][2], 0.001);
    }

    /**
     * 测试卷积层反向传播
     */
    public static void testConvBackward() {
        System.out.println("\n--- 测试卷积层反向传播 ---");

        double[][][] input = new double[][][]{
            {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
            }
        };

        double[][][] gradOutput = new double[][][]{
            {
                {1, 0},
                {0, 1}
            }
        };

        double[][][][] kernels = new double[][][][]{
            {
                {
                    {1, 0},
                    {0, 1}
                }
            }
        };

        CNNBackPropagation.ConvGradResult result = CNNBackPropagation.convBackward(
                gradOutput, input, kernels, 1, 0);

        assertNotNull("输入梯度不为null", result.gradInput);
        assertNotNull("卷积核梯度不为null", result.gradKernels);
        assertNotNull("偏置梯度不为null", result.gradBiases);
        assertEquals("偏置梯度", 2, result.gradBiases[0], 0.001);
    }

    /**
     * 测试展平操作
     */
    public static void testFlatten() {
        System.out.println("\n--- 测试展平操作 ---");

        double[][][] input = new double[][][]{
            {
                {1, 2},
                {3, 4}
            },
            {
                {5, 6},
                {7, 8}
            }
        };

        double[] flattened = CNNBackPropagation.flatten(input);

        assertEquals("展平后长度", 8, flattened.length);
        assertEquals("展平后第一个值", 1, flattened[0], 0.001);
        assertEquals("展平后最后一个值", 8, flattened[7], 0.001);

        double[][][] recovered = CNNBackPropagation.flattenBackward(flattened, 2, 2, 2);
        assertEquals("恢复后通道数", 2, recovered.length);
        assertEquals("恢复后高度", 2, recovered[0].length);
        assertEquals("恢复后宽度", 2, recovered[0][0].length);
        assertEquals("恢复后(0,0,0)", 1, recovered[0][0][0], 0.001);
        assertEquals("恢复后(1,1,1)", 8, recovered[1][1][1], 0.001);
    }

    /**
     * 测试CNN构建
     */
    public static void testCNNBuilder() {
        System.out.println("\n--- 测试CNN构建 ---");

        ConvolutionalNeuralNetwork cnn = new ConvolutionalNeuralNetwork.Builder()
                .addConvLayer(1, 6, 5, 1, 0)
                .addActivation(CNNBackPropagation.ActivationType.RELU)
                .addMaxPoolLayer(2, 2)
                .addFlatten()
                .addFCLayer(10)
                .addSoftmax()
                .learningRate(0.001)
                .build();

        assertNotNull("CNN不为null", cnn);
    }

    /**
     * 测试LeNet构建
     */
    public static void testLeNet() {
        System.out.println("\n--- 测试LeNet构建 ---");

        LeNet lenet = LeNet.forMNIST(0.001);
        assertNotNull("LeNet不为null", lenet);
    }

    /**
     * 测试数据加载器
     */
    public static void testDataLoader() {
        System.out.println("\n--- 测试数据加载器 ---");

        double[][][][] images = new double[10][1][28][28];
        double[][] labels = new double[10][10];

        for (int i = 0; i < 10; i++) {
            labels[i][i % 10] = 1.0;
        }

        ImageDataLoader loader = new ImageDataLoader(images, labels, 2, true);

        assertEquals("批次数量", 5, loader.getNumBatches());
        assertTrue("有下一个批次", loader.hasNext());

        ImageDataLoader.Batch batch = loader.next();
        assertEquals("批次大小", 2, batch.size);
        assertEquals("批次图像数量", 2, batch.images.length);
        assertEquals("批次标签数量", 2, batch.labels.length);
    }

    /**
     * 测试批量归一化
     */
    public static void testBatchNormalization() {
        System.out.println("\n--- 测试批量归一化 ---");

        BatchNormalization bn = new BatchNormalization(2);

        double[][][] input = new double[][][]{
            {
                {1, 2, 3},
                {4, 5, 6}
            },
            {
                {7, 8, 9},
                {10, 11, 12}
            }
        };

        double[][][] output = bn.forward(input);

        assertEquals("BN输出通道数", 2, output.length);
        assertEquals("BN输出高度", 2, output[0].length);
        assertEquals("BN输出宽度", 3, output[0][0].length);
    }

    /**
     * 测试图像裁剪
     */
    public static void testImageCrop() {
        System.out.println("\n--- 测试图像裁剪 ---");

        double[][][] image = new double[][][]{
            {
                {1, 2, 3, 4},
                {5, 6, 7, 8},
                {9, 10, 11, 12},
                {13, 14, 15, 16}
            }
        };

        double[][][] cropped = ImagePreprocessing.centerCrop(image, 2, 2);

        assertEquals("裁剪后通道数", 1, cropped.length);
        assertEquals("裁剪后高度", 2, cropped[0].length);
        assertEquals("裁剪后宽度", 2, cropped[0][0].length);
        assertEquals("裁剪后(0,0)", 6, cropped[0][0][0], 0.001);
        assertEquals("裁剪后(1,1)", 11, cropped[0][1][1], 0.001);
    }

    /**
     * 测试图像旋转
     */
    public static void testImageRotation() {
        System.out.println("\n--- 测试图像旋转 ---");

        double[][][] image = new double[][][]{
            {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
            }
        };

        double[][][] rotated = ImagePreprocessing.rotate90(image, 1);

        assertEquals("旋转后通道数", 1, rotated.length);
        assertEquals("旋转后高度", 3, rotated[0].length);
        assertEquals("旋转后宽度", 3, rotated[0][0].length);
        assertEquals("旋转后(0,0)", 7, rotated[0][0][0], 0.001);
        assertEquals("旋转后(0,2)", 1, rotated[0][0][2], 0.001);
    }

    /**
     * 测试激活函数反向传播
     */
    public static void testActivationBackward() {
        System.out.println("\n--- 测试激活函数反向传播 ---");

        double[][][] gradOutput = new double[][][]{
            {
                {1, 1},
                {1, 1}
            }
        };

        double[][][] z = new double[][][]{
            {
                {1, -1},
                {0, 2}
            }
        };

        double[][][] gradInput = CNNBackPropagation.activationBackward(
                gradOutput, z, CNNBackPropagation.ActivationType.RELU);

        assertEquals("ReLU梯度(0,0)", 1, gradInput[0][0][0], 0.001);
        assertEquals("ReLU梯度(0,1)", 0, gradInput[0][0][1], 0.001);
        assertEquals("ReLU梯度(1,0)", 0, gradInput[0][1][0], 0.001);
        assertEquals("ReLU梯度(1,1)", 1, gradInput[0][1][1], 0.001);
    }

    /**
     * 测试数据加载器分割
     */
    public static void testDataLoaderSplit() {
        System.out.println("\n--- 测试数据加载器分割 ---");

        double[][][][] images = new double[100][1][28][28];
        double[][] labels = new double[100][10];

        ImageDataLoader loader = new ImageDataLoader(images, labels, 10, false);
        ImageDataLoader[] split = loader.split(0.8);

        assertEquals("分割后数量", 2, split.length);
        assertEquals("训练集大小", 80, split[0].getDatasetSize());
        assertEquals("验证集大小", 20, split[1].getDatasetSize());
    }
}
