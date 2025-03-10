package com.idea;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * @ClassName: LogAnalyzer2
 * @Description: 日志分析工具类，用于分析指定目录下的日志文件，并统计用户行为。 优化目标：减少内存占用，支持处理大量日志文件和数据行。<br>
 * 新增功能：
 *      1. 支持同一用户多个日志文件合并统计
 *      2. 支持按日、按月统计两种模式，按月则是包含当月及前6个月
 *      3. 改进文件名解析逻辑
 *      4. 支持参数输入控制统计日期、日志目录、输出目录
 *      5. 增加生成excel报告
 * @Author: yuanbao
 * @Date: 2025/3/3
 **/
public class LogAnalyzer2
{
    // 日志中日期的格式
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    // 日志中表示通义灵码-“写代码”行为的关键字
    private static final String ACTION_PATTERN = "com.alibabacloud.intellij.cosy.editor.CosyEditorActionHandler - execute action:EditorTab";
    // 日志中表示通义灵码-“提问”行为的关键字
    private static final String SELECT_PATTERN = "Select model is";

    // 增加Github Copilot的”写代码“的关键字
    private static final String COPILOT_ACTION_PATTERN = "https://proxy.individual.githubcopilot.com/v1/engines/copilot-codex/completions";
    // 增加Github Copilot的”提问“的关键字
    private static final String COPILOT_SELECT_PATTERN = "https://api.individual.githubcopilot.com/chat/completions";

    // 月格式
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    // 统计模式枚举
    enum AnalysisMode {
        DAILY,  // 日统计模式（包含当前日期的前一周）
        MONTHLY  // 月统计模式（包含当前月份的前6个月）
    }

    // 静态初始化块
    static {
        try {
            System.out.println("Initializing LogAnalyzer2...");
            // 其他静态初始化代码
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize LogAnalyzer2", e);
        }
    }

    /**
     * 主方法，程序入口。
     *
     * @param args
     * 命令行参数：
     *         1. 日期（可选，格式为 yyyy-MM-dd，默认为当前日期）
     *         2. 日志目录（可选，默认为 "logs/"）
     *         3. 输出目录（可选，默认与日志目录一致）
     */
    public static void main(String[] args)
    {
        // 获取当前JAR包所在目录
        String jarDir = getJarDirectory();
        if (jarDir == null)
        {
            System.err.println("无法获取JAR包所在目录，程序退出。");
//            return;
            throw new RuntimeException("无法获取JAR包所在目录，程序退出。");
        }
        System.out.println("当前JAR包所在目录：" + jarDir);

        // 参数初始化
        String logDir = jarDir; // 日志目录-默认当前目录（TEST:logs/）
        String outputDir = ""; // 输出目录，默认同日志目录
        LocalDate baseDate = LocalDate.now(); // 默认分析日期为当前日期
        AnalysisMode mode = AnalysisMode.DAILY; // 默认统计模式为日

        // 解析命令行参数
        if (args.length >= 1)
        {
            String dateArg = args[0]; // args[0]
            if (dateArg.length() == 6)
            { // 月模式参数，如202503
                try
                {
                    baseDate = YearMonth.parse(dateArg, DateTimeFormatter.ofPattern("yyyyMM")).atDay(1);
                } catch (Exception e)
                {
                    System.err.println("传参月份格式不对，应为yyyyMM。当前为：" + dateArg);
//                    return;
                    throw new RuntimeException("传参月份格式不对，应为yyyyMM。当前为：" + dateArg);
                }
                mode = AnalysisMode.MONTHLY;
            } else
            { // 周模式参数，如20250303
                try
                {
                    baseDate = LocalDate.parse(dateArg, DateTimeFormatter.ofPattern("yyyyMMdd"));
                } catch (Exception e)
                {
                    System.err.println("传参日期格式不对，应为yyyyMMdd。当前为：" + dateArg);
//                    return;
                    throw new RuntimeException("传参日期格式不对，应为yyyyMMdd。当前为：" + dateArg);
                }
                mode = AnalysisMode.DAILY;
            }
        }
        if (args.length >= 2)
        {
            logDir = args[1];
        }
        if (args.length >= 3)
        {
            outputDir = args[2];
        } else
        {
            outputDir = logDir; // 默认输出目录，同日志目录
        }
        // 调用日志分析方法
        analyzeLogs(logDir, outputDir, baseDate, mode);
    }

    /**
     * 获取当前JAR包所在目录
     *
     * @return JAR包所在目录的路径，如果无法获取则返回null
     */
    private static String getJarDirectory() {
        try {
            // 获取JAR包的路径
            String jarPath = LogAnalyzer2.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File jarFile = new File(jarPath);
            return jarFile.getParent(); // 返回JAR包所在目录
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 核心方法：分析日志文件，统计用户行为。
     * 步骤：
     *      1、检查校验目录和文件
     *      2、分析日志信息进行统计
     *      3、输出统计结果
     *
     * @param logDir
     *         日志文件目录
     * @param outputDir
     *         输出文件目录
     * @param baseDate
     *         分析日期
     */
    private static void analyzeLogs(String logDir, String outputDir, LocalDate baseDate, AnalysisMode mode)
    {
        try
        {

            List<Path> logFiles = new ArrayList<>();
            // 检查日志目录是否存在
            if (!Files.exists(Paths.get(logDir)))
            {
                System.err.println("错误：日志目录不存在。请检查日志目录：" + logDir);
//                return;
                throw new RuntimeException("错误：日志目录不存在。请检查日志目录：" + logDir);
            }
            // 检查日志目录下是否有log日志文件，有的话则获取
            try
            {
                // 获取日志目录下所有 .log 文件 和 .log.1、.log.2等文件
                logFiles = Files.walk(Paths.get(logDir))
                        .filter(Files::isRegularFile)
                        // 修改此处增加适配".log.1"、".log.2"等格式，原来只适配".log"。取文件名而不是取路径防止无匹配
                        .filter(path -> path.getFileName().toString().matches(".*\\.log(\\.\\d+)?$"))
                        .collect(Collectors.toList());
            } catch (IOException e)
            {
                System.err.println("获取日志文件出错：" + e.toString());
                throw new RuntimeException("获取日志文件出错：" + e.toString());
            }

            // 如果没有找到日志文件，输出错误信息并终止程序
            if (logFiles == null || logFiles.isEmpty())
            {
                System.err.println("错误：日志目录内未找到任何log日志文件。请检查日志目录：" + logDir);
//                return;
                throw new RuntimeException("错误：日志目录内未找到任何log日志文件。请检查日志目录：" + logDir);
            }

            // 用于存储统计结果的 Map
            // 数据结构：用户名 -> 日期 -> 行为 -> 次数   【重要】
            Map<String, Map<LocalDate, Map<String, Integer>>> stats = new HashMap<>();

            // 遍历每个日志文件，分别处理避免内存占用过高
            for (Path logFile : logFiles)
            {
                // 提取用户名（从文件名中解析）
                String userName = extractRealName(logFile.getFileName().toString());
                System.out.print("正在处理文件：" + logFile.getFileName() + " 用户名：" + userName);
                stats.putIfAbsent(userName, new HashMap<>());

                // 缓冲流逐行处理日志文件
                try (BufferedReader reader = Files.newBufferedReader(logFile))
                {
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        processLine(line, stats.get(userName), baseDate, mode);
                    }
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
                System.out.println(" finished!");
            }

            // 将统计结果输出写入报告文件
            generateReport(stats, outputDir, baseDate, mode);

            // 新增：生成Excel报告
            generateExcelReport(stats, outputDir, baseDate, mode);
        } catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException("处理日志文件出错：" + e.toString());
        }
    }

    /**
     * 从日志文件名中提取用户名，倒数第二个下划线到最后一个下划线之间的字符串即为用户名。
     *
     * @param fileName 日志文件名
     * @return 用户名
     */
    private static String extractRealName(String fileName)
    {
        // 文件名格式：xxx_用户名_idea.log
//        return fileName.substring(fileName.lastIndexOf('_') + 1, fileName.lastIndexOf('.')); // 取最后一个下划线之后的字符串作为用户名
        return fileName.substring(fileName.lastIndexOf('_', fileName.lastIndexOf('_') - 1) + 1, fileName.lastIndexOf('_')); // 取倒数第二个下划线到倒数第一个下划线之间的字符串作为用户名
    }

    /**
     * 是否在日期范围内的判断逻辑
     * 日模式：baseDate前7天
     * 月模式：baseDate所在月及前6个月
     */
    private static boolean isWithinRange(LocalDate logDate, LocalDate baseDate, AnalysisMode mode) {

        // 获取baseDate所在月的最后一天
        LocalDate lastDayOfMonth = baseDate.withDayOfMonth(baseDate.lengthOfMonth());

        return mode == AnalysisMode.DAILY ?
                logDate.isAfter(baseDate.minusDays(7)) && !logDate.isAfter(baseDate) :
                logDate.isAfter(baseDate.minusMonths(6).withDayOfMonth(1)) && !logDate.isAfter(lastDayOfMonth); // 如果是月模式的话就取6个月前的第一天到当月的最后一天
    }

    /**
     * 处理日志文件中的一行，更新统计结果。
     *
     * @param line
     *         日志行
     * @param userStats
     *         用户行为统计结果
     * @param baseDate
     *         分析日期
     */
    private static void processLine(String line, Map<LocalDate, Map<String, Integer>> userStats, LocalDate baseDate, AnalysisMode mode)
    {
        // 按空格分割日志行，第一部分为日期，第二部分为内容
        String[] parts = line.split(" ", 2);
        if (parts.length < 2)
            return;

        try
        {
            // 解析日志行中的日期
            LocalDate logDate = LocalDate.parse(parts[0], DateTimeFormatter.ofPattern(DATE_PATTERN));

            // 根据模式判断是否在统计范围内
            if (isWithinRange(logDate, baseDate, mode))
            {
                // 如果是按月统计，则需要将日期设置为月份的1号
                if (mode == AnalysisMode.MONTHLY)
                    logDate = logDate.withDayOfMonth(1);


                // 更新统计结果
                userStats.computeIfAbsent(logDate, k -> new HashMap<>());
                Map<String, Integer> dailyStats = userStats.get(logDate);

                // 基础统计
                dailyStats.merge("总记录数", 1, Integer::sum);

                // 行为统计
                if (line.contains(ACTION_PATTERN)) // 写代码行为
                    dailyStats.merge(ACTION_PATTERN, 1, Integer::sum);
                if (line.contains(SELECT_PATTERN)) // 提问行为
                    dailyStats.merge(SELECT_PATTERN, 1, Integer::sum);
                if (line.contains(COPILOT_ACTION_PATTERN)) // Copilot写代码行为
                    dailyStats.merge(COPILOT_ACTION_PATTERN, 1, Integer::sum);
                if (line.contains(COPILOT_SELECT_PATTERN)) // Copilot提问行为
                    dailyStats.merge(COPILOT_SELECT_PATTERN, 1, Integer::sum);
            }
        } catch (DateTimeParseException e)
        {
            // 如果日期解析失败，跳过该行
            return;
        }
    }

    /**
     * 将统计结果写入输出文件。
     *
     * @param stats
     *         统计结果
     * @param outputDir
     *         输出目录
     * @param baseDate
     *         分析日期
     */
    private static void generateReport(Map<String, Map<LocalDate, Map<String, Integer>>> stats, String outputDir, LocalDate baseDate, AnalysisMode mode) throws IOException
    {
        // 输出文件路径
        // 按日模式和月模式分别生成不同的文件名
        String fileName = mode == AnalysisMode.DAILY ? "analysis_report_daily.txt" : "analysis_report_monthly.txt";
        Path outputPath = Paths.get(outputDir, fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath))
        {
            // 遍历每个用户的统计结果
            for (Map.Entry<String, Map<LocalDate, Map<String, Integer>>> entry : stats.entrySet())
            {
                String realName = entry.getKey();
                writer.write(realName + "\n");
                System.out.printf("%n姓名：%s%n", realName);

                // 获取日期范围
                List<LocalDate> dateRange = generateDateRange(baseDate, mode);


                // 遍历指定日期范围内的每一天
                for (LocalDate logDate : dateRange)
                {
                    // 获取当天的统计结果
                    Map<String, Integer> dailyStats = entry.getValue().getOrDefault(logDate, new HashMap<>());

                    // 写入当天的统计结果
                    // 拆分打印，领导可能不一定要展示总次数
                    if (mode == AnalysisMode.DAILY)
                    {
                        writer.write(String.format("%s", logDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))));// 日期
                    } else if (mode == AnalysisMode.MONTHLY)
                    {
                        writer.write(String.format("%s", logDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))));// 月份
                    }
                    writer.write(String.format(" 总记录数: %d", dailyStats.getOrDefault("总记录数", 0))); // 看情况需不需要展示当日总记录数
                    writer.write(String.format(" AI总次数: %d", dailyStats.getOrDefault(ACTION_PATTERN, 0) + dailyStats.getOrDefault(SELECT_PATTERN, 0))); // AI总次数=写代码+提问
                    writer.write(String.format(" 写代码: %d", dailyStats.getOrDefault(ACTION_PATTERN, 0)));
                    writer.write(String.format(" 提问: %d", dailyStats.getOrDefault(SELECT_PATTERN, 0)));
                    writer.write("\n");

                    // 控制台打印
                    System.out.printf("%s 总记录数: %d AI总次数: %d 写代码: %d 提问: %d%n", mode == AnalysisMode.DAILY ? logDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))
                            : logDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                            , dailyStats.getOrDefault("总记录数", 0), dailyStats.getOrDefault(ACTION_PATTERN, 0) + dailyStats.getOrDefault(SELECT_PATTERN, 0)
                            , dailyStats.getOrDefault(ACTION_PATTERN, 0), dailyStats.getOrDefault(SELECT_PATTERN, 0));
                }
                writer.write("\n"); // 添加一个空行分隔用户
            }
        } catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException("写入输出文件出错：" + e.toString());
        }
    }

    /**
     * 新增生成Excel报告
     */
    private static void generateExcelReport(Map<String, Map<LocalDate, Map<String, Integer>>> stats, String outputDir, LocalDate baseDate, AnalysisMode mode) throws IOException
    {
        // 创建Excel工作簿和工作表
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("日志分析报告");

        // 表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"序号", "姓名", "日期/月份", "总记录数", "通义-总次数", "通义-写代码", "通义-提问", "Copilot-写代码", "Copilot-提问"};
        for (int i = 0; i < headers.length; i++)
        {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);

            // 设置列宽
            sheet.setColumnWidth(i, 256 * 10); // 每列宽度为10个字符

            // 特殊列宽设置
            if (i == 0)
            {
                sheet.setColumnWidth(i, 256 * 5); // 第1列宽度为5个字符
            } else if (i == 2)
            {
                sheet.setColumnWidth(i, 256 * 15); // 第3列宽度为15个字符   日期/月份
            } else if (i >= 4)
            {
                sheet.setColumnWidth(i, 256 * 15); // 第5列及之后宽度为15个字符   通义-总次数
            }

        }

        // 填充数据
        int rowNum = 1;
        List<LocalDate> dateRange = generateDateRange(baseDate, mode);
        for (Map.Entry<String, Map<LocalDate, Map<String, Integer>>> entry : stats.entrySet())
        {
            String realName = entry.getKey();
            for (LocalDate logDate : dateRange)
            {
                Map<String, Integer> dailyStats = entry.getValue().getOrDefault(logDate, new HashMap<>());
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rowNum - 1); // 序号
                row.createCell(1).setCellValue(realName); // 姓名
                row.createCell(2).setCellValue(mode == AnalysisMode.DAILY ? logDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))
                        : logDate.format(DateTimeFormatter.ofPattern("yyyy-MM")));
                row.createCell(3).setCellValue(dailyStats.getOrDefault("总记录数", 0));
                row.createCell(4).setCellValue(dailyStats.getOrDefault(ACTION_PATTERN, 0) + dailyStats.getOrDefault(SELECT_PATTERN, 0));
                row.createCell(5).setCellValue(dailyStats.getOrDefault(ACTION_PATTERN, 0));
                row.createCell(6).setCellValue(dailyStats.getOrDefault(SELECT_PATTERN, 0));
                row.createCell(7).setCellValue(dailyStats.getOrDefault(COPILOT_ACTION_PATTERN, 0));
                row.createCell(8).setCellValue(dailyStats.getOrDefault(COPILOT_SELECT_PATTERN, 0) / 3); // Copilot提问次数除以3为真实数
            }
        }

        // 写入文件
        // 按日模式和月模式分别生成不同的文件名
        String fileName = mode == AnalysisMode.DAILY ? "analysis_report_daily.xlsx" : "analysis_report_monthly.xlsx";
        Path excelPath = Paths.get(outputDir, fileName);
        try (FileOutputStream fileOut = new FileOutputStream(excelPath.toFile()))
        {
            workbook.write(fileOut);
        } catch (IOException e)
        {
            e.printStackTrace();
            System.err.println("写入Excel文件出错：" + e.toString());
            throw new RuntimeException("写入Excel文件出错：" + e.toString());
        }

        // 关闭工作簿
        workbook.close();
    }

    /**
     * 动态生成日期范围
     * 周模式：生成7天日期
     * 月模式：生成包含当前月及前6个月的每月首日
     */
    private static List<LocalDate> generateDateRange(LocalDate date, AnalysisMode mode)
    {
        List<LocalDate> dates = new ArrayList<>();
        if (mode == AnalysisMode.DAILY)
        {
            for (int i = 0; i < 7; i++)
            {
                dates.add(date.minusDays(6 - i));
            }
        } else
        {
            for (int i = 6; i >= 0; i--)
            {
                YearMonth targetMonth = YearMonth.from(date).minusMonths(i);
                dates.add(targetMonth.atDay(1));
            }
        }
        return dates;
    }


    /**
     * @MethodName: userSelectAndAnalyze
     * @Description: 提供方法给交互界面使用，传参输入日期和日志目录，进行日志分析。与Main方法相似。
     * @param selectDateStr 选择的日期/月份
     * @param selectLogDir 选择的日志目录
     * @Return void
     * @Author: yuanbao
     * @Date: 2025/3/4
     **/
    public void userSelectAndAnalyze(String selectDateStr, String selectLogDir) throws Exception
    {
        // 根据用户输入的日期进行日志分析
        System.out.println("Analyzing logs for the date: " + selectDateStr);

        // 获取当前JAR包所在目录
        String jarDir = getJarDirectory();
        if (jarDir == null)
        {
            System.err.println("无法获取JAR包所在目录，程序退出。");
            //            return;
            throw new RuntimeException("无法获取JAR包所在目录，程序退出。");
        }
        System.out.println("当前JAR包所在目录：" + jarDir);

        // 参数初始化
        String logDir = jarDir; // 日志目录-默认当前目录（TEST:logs/）
        String outputDir = ""; // 输出目录，默认同日志目录
        LocalDate baseDate = LocalDate.now(); // 默认分析日期为当前日期
        AnalysisMode mode = AnalysisMode.DAILY; // 默认统计模式为日

        // 解析命令行参数
        String dateArg = selectDateStr; // args[0]
        if (dateArg.length() == 6)
        { // 月模式参数，如202503
            try
            {
                baseDate = YearMonth.parse(dateArg, DateTimeFormatter.ofPattern("yyyyMM")).atDay(1);
            } catch (Exception e)
            {
                System.err.println("传参月份格式不对，应为yyyyMM。当前为：" + dateArg);
                //                    return;
                throw new RuntimeException("传参月份格式不对，应为yyyyMM。当前为：" + dateArg);
            }
            mode = AnalysisMode.MONTHLY;
        } else
        { // 周模式参数，如20250303
            try
            {
                baseDate = LocalDate.parse(dateArg, DateTimeFormatter.ofPattern("yyyyMMdd"));
            } catch (Exception e)
            {
                System.err.println("传参日期格式不对，应为yyyyMMdd。当前为：" + dateArg);
                //                    return;
                throw new RuntimeException("传参日期格式不对，应为yyyyMMdd。当前为：" + dateArg);
            }
            mode = AnalysisMode.DAILY;

        }
        if (selectLogDir != null && selectLogDir.length() > 0)
        {
            logDir = selectLogDir;
        }
        outputDir = logDir; // 默认输出目录，同日志目录

        // 调用日志分析方法
        analyzeLogs(logDir, outputDir, baseDate, mode);
    }
}