package com.idea;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;

/**
 * @ClassName: LogAnalyzer
 * @Description: 日志分析工具类，用于分析指定目录下的日志文件，并统计用户行为。 优化目标：减少内存占用，支持处理大量日志文件和数据行。<br>
 *      第一版：仅适用于每个人一个日志文件的情况；仅处理最近7天的日志；
 * @Author: yuanbao
 * @Date: 2025/2/8
 **/
public class LogAnalyzer
{
    // 日志中日期的格式
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    // 日志中表示“写代码”行为的关键字
    private static final String ACTION_PATTERN = "com.alibabacloud.intellij.cosy.editor.CosyEditorActionHandler - execute action:EditorTab";
    // 日志中表示“提问”行为的关键字
    private static final String SELECT_PATTERN = "Select model is";

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
        String logDir = "logs/"; // 默认日志目录
        String outputDir = logDir; // 默认输出目录，同日志目录
        LocalDate date = LocalDate.now(); // 默认分析日期为当前日期

        // 解析命令行参数
        if (args.length >= 1)
        {
            date = LocalDate.parse(args[0], DateTimeFormatter.ofPattern(DATE_PATTERN));
        }
        if (args.length >= 2)
        {
            logDir = args[1];
        }
        if (args.length >= 3)
        {
            outputDir = args[2];
        }

        // 调用日志分析方法
        analyzeLogs(logDir, outputDir, date);
    }

    /**
     * 分析日志文件，统计用户行为。
     *
     * @param logDir
     *         日志文件目录
     * @param outputDir
     *         输出文件目录
     * @param date
     *         分析日期
     */
    private static void analyzeLogs(String logDir, String outputDir, LocalDate date)
    {
        try
        {

            List<Path> logFiles = new ArrayList<>();
            // 检查日志目录是否存在
            if (!Files.exists(Paths.get(logDir)))
            {
                System.err.println("错误：日志目录不存在。请检查日志目录：" + logDir);
                return;
            }
            // 检查日志目录下是否有log日志文件，有的话则获取
            try
            {
                // 获取日志目录下所有 .log 文件
                logFiles = Files.walk(Paths.get(logDir)).filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".log")).collect(Collectors.toList());
            } catch (IOException e)
            {
                System.err.println("获取日志文件出错：" + e.toString());
            }

            // 如果没有找到日志文件，输出错误信息并终止程序
            if (logFiles == null || logFiles.isEmpty())
            {
                System.err.println("错误：日志目录内未找到任何log日志文件。请检查日志目录：" + logDir);
                return;
            }

            // 用于存储统计结果的 Map
            // 结构：用户名 -> 日期 -> 行为 -> 次数
            Map<String, Map<LocalDate, Map<String, Integer>>> stats = new HashMap<>();

            // 遍历每个日志文件，分别处理避免内存占用过高
            for (Path logFile : logFiles)
            {
                // 提取用户名（从文件名中解析）
                String realName = extractRealName(logFile.getFileName().toString());
                System.out.println("正在处理文件：" + logFile.getFileName() + " 用户名：" + realName);
                stats.putIfAbsent(realName, new HashMap<>());

                // 逐行处理日志文件
                try (BufferedReader reader = Files.newBufferedReader(logFile))
                {
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        processLine(line, stats.get(realName), date);
                    }
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            // 将统计结果写入输出文件
            writeOutput(stats, outputDir, date);
        } catch (Exception e)
        {
            e.printStackTrace();
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
        return fileName.substring(fileName.lastIndexOf('_') + 1, fileName.lastIndexOf('.')); // 取最后一个下划线之后的字符串作为用户名
//        return fileName.substring(fileName.lastIndexOf('_', fileName.lastIndexOf('_') - 1) + 1, fileName.lastIndexOf('_')); // 取倒数第二个下划线到倒数第一个下划线之间的字符串作为用户名
    }

    /**
     * 处理日志文件中的一行，更新统计结果。
     *
     * @param line
     *         日志行
     * @param userStats
     *         用户行为统计结果
     * @param date
     *         分析日期
     */
    private static void processLine(String line, Map<LocalDate, Map<String, Integer>> userStats, LocalDate date)
    {
        // 按空格分割日志行，第一部分为日期，第二部分为内容
        String[] parts = line.split(" ", 2);
        if (parts.length < 2)
            return;

        // 解析日志行中的日期
        LocalDate logDate;
        try
        {
            logDate = LocalDate.parse(parts[0], DateTimeFormatter.ofPattern(DATE_PATTERN));
        } catch (DateTimeParseException e)
        {
            // 如果日期解析失败，跳过该行
            return;
        }

        // 只统计指定日期范围内的日志（过去7天）
        if (logDate.isAfter(date.minusDays(7)) && !logDate.isAfter(date))
        {
            userStats.putIfAbsent(logDate, new HashMap<>());

            // 获取当天的统计结果
            Map<String, Integer> dailyStats = userStats.get(logDate);

            // 更新总记录数-是指当日总记录行
            dailyStats.put("总记录数", dailyStats.getOrDefault("总记录数", 0) + 1);

            // 更新“写代码”行为次数
            if (line.contains(ACTION_PATTERN))
            {
                dailyStats.put("写代码", dailyStats.getOrDefault("写代码", 0) + 1);
            }

            // 更新“提问”行为次数
            if (line.contains(SELECT_PATTERN))
            {
                dailyStats.put("提问", dailyStats.getOrDefault("提问", 0) + 1);
            }
        }
    }

    /**
     * 将统计结果写入输出文件。
     *
     * @param stats
     *         统计结果
     * @param outputDir
     *         输出目录
     * @param date
     *         分析日期
     */
    private static void writeOutput(Map<String, Map<LocalDate, Map<String, Integer>>> stats, String outputDir, LocalDate date)
    {
        // 输出文件路径
        File outputFile = new File(outputDir, "result.txt");

        try (PrintWriter writer = new PrintWriter(outputFile))
        {
            // 遍历每个用户的统计结果
            for (Map.Entry<String, Map<LocalDate, Map<String, Integer>>> entry : stats.entrySet())
            {
                String realName = entry.getKey();
                writer.println(realName);
                System.out.printf("%n姓名：%s%n", realName);

                // 遍历指定日期范围内的每一天
                for (LocalDate logDate = date.minusDays(6); !logDate.isAfter(date); logDate = logDate.plusDays(1))
                {
                    // 获取当天的统计结果
                    Map<String, Integer> dailyStats = entry.getValue().getOrDefault(logDate, new HashMap<>());

                    // 写入当天的统计结果
                    // 拆分打印，领导可能不一定要展示总次数
                    writer.printf("%s", logDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))); // 日期
                    writer.printf(" 总记录数: %d", dailyStats.getOrDefault("总记录数", 0)); // 看情况需不需要展示当日总记录数
                    writer.printf(" AI总次数: %d", dailyStats.getOrDefault("写代码", 0) + dailyStats.getOrDefault("提问", 0)); // AI总次数=写代码+提问
                    writer.printf(" 写代码: %d", dailyStats.getOrDefault("写代码", 0));
                    writer.printf(" 提问: %d", dailyStats.getOrDefault("提问", 0));
                    writer.println();

                    // 控制台打印
                    System.out.printf("%s 总记录数: %d AI总次数: %d 写代码: %d 提问: %d%n", logDate.format(DateTimeFormatter.ofPattern(DATE_PATTERN))
                            , dailyStats.getOrDefault("总记录数", 0), dailyStats.getOrDefault("写代码", 0) + dailyStats.getOrDefault("提问", 0)
                            , dailyStats.getOrDefault("写代码", 0), dailyStats.getOrDefault("提问", 0));
                }
                writer.println(); // 添加一个空行分隔用户
            }
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }
}