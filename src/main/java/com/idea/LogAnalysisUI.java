    package com.idea;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @ClassName: LogAnalysisUI
 * @Description: 增加UI交互界面，进行日期的输入、日志目录的选择。<br>
 * @Author: yuanbao
 * @Date: 2025/3/4
 **/
public class LogAnalysisUI
{

    private JFrame frame; // 主窗体
    private JTextField dateField; // 日期输入框
    private JTextField logDirField; // 日志目录输入框
    private JButton confirmButton; // 确认按钮
    private LogAnalyzer2 logAnalyzer;

    public LogAnalysisUI()
    {
        logAnalyzer = new LogAnalyzer2();
        initializeUI();
    }

    private void initializeUI()
    {
        // 创建主窗体
        frame = new JFrame("Log Analysis Tool");
        frame.setSize(700, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);

        // 设置字体
        Font font = new Font("Arial", Font.PLAIN, 16);

        // 创建日期输入框
        JLabel label = new JLabel("Select Date (yyyyMMdd or yyyyMM): ");
        label.setBounds(50, 50, 300, 40);
        label.setFont(font);
        frame.add(label);

        dateField = new JTextField();
        dateField.setBounds(360, 50, 280, 40);
        dateField.setFont(font);
        // 设置默认值为当天日期
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        dateField.setText(dateFormat.format(new Date()));
        frame.add(dateField);

        // 增加选择日志目录和显示
        logDirField = new PlaceholderTextField("(default current directory)");
        logDirField.setBounds(360, 120, 280, 40);
        logDirField.setFont(font);
        frame.add(logDirField);

        JButton selectButton = new JButton("Select Log Directory: ");
        selectButton.setBounds(50, 120, 200, 40);
        selectButton.setFont(font);
        frame.add(selectButton);
        selectButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                JFileChooser fileChooser = new JFileChooser();
                // 设置初始目录为当前程序所在的目录
                fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int result = fileChooser.showOpenDialog(frame);
                if (result == JFileChooser.APPROVE_OPTION)
                {
                    String logDir = fileChooser.getSelectedFile().getAbsolutePath();
                    // 显示选择的日志目录
                    logDirField.setText(logDir);
                }
            }
        });

        // 创建确认按钮
        confirmButton = new JButton("CONFIRM");
        confirmButton.setBounds(250, 240, 200, 50);
        confirmButton.setFont(font);
        frame.add(confirmButton);

        confirmButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String inputDate = dateField.getText();
                String logDir = logDirField.getText();
                if (logDir.equals("(default current directory)"))
                {
                    logDir = null;
                }

                try
                {
                    // 将确认按钮置灰不可用，且修改为“请稍候..”
                    confirmButton.setText("Processing... Wait.");
                    confirmButton.setEnabled(false);

                    // 使用 SwingWorker 处理耗时任务
                    String finalLogDir = logDir;
                    new SwingWorker<Void, Void>()
                    {
                        @Override
                        protected Void doInBackground() throws Exception
                        {
                            // 在后台线程中执行日志分析方法
                            logAnalyzer.userSelectAndAnalyze(inputDate, finalLogDir);
                            return null;
                        }

                        @Override
                        protected void done()
                        {
                            try
                            {
                                get(); // 关键：触发异常传播
                                // 任务完成后恢复按钮状态并显示消息
                                JOptionPane.showMessageDialog(frame, "Log analysis completed for: " + inputDate);
                            }
                            catch (Exception ex)
                            {
                                System.err.println("Error: " + ex.getMessage());
                                JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            }
                            finally
                            {
                                // 恢复确认按钮可用
                                confirmButton.setText("CONFIRM");
                                confirmButton.setEnabled(true);
                            }
                        }
                    }.execute();
                } catch (Exception ex)
                {
                    System.err.println("Error: " + ex.getMessage());
                    JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);

                    // 如果在启动 SwingWorker 时发生异常，恢复按钮状态
                    confirmButton.setText("CONFIRM");
                    confirmButton.setEnabled(true);
                }
            }
        });


        frame.setVisible(true);
    }

    public static void main(String[] args)
    {
        new LogAnalysisUI();
    }

    public class PlaceholderTextField extends JTextField
    {
        private String placeholder;

        public PlaceholderTextField(String placeholder)
        {
            this.placeholder = placeholder;
            setText(placeholder);
            setOpaque(false);
            setEditable(true);
            addFocusListener(new java.awt.event.FocusAdapter()
            {
                public void focusGained(java.awt.event.FocusEvent evt)
                {
                    if (getText().equals(placeholder))
                    {
                        setText("");
                        setOpaque(true);
                    }
                }

                public void focusLost(java.awt.event.FocusEvent evt)
                {
                    if (getText().length() == 0)
                    {
                        setText(placeholder);
                        setOpaque(false);
                    }
                }
            });
        }

        @Override
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            if (getText().length() == 0)
            {
                int h = getHeight();
                ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                Insets ins = getInsets();
                FontMetrics fm = g.getFontMetrics();
                int c0 = getBackground().getRGB();
                int c1 = getForeground().getRGB();
                int m = 0xfefefefe;
                int c2 = ((c0 & m) >>> 1) + ((c1 & m) >>> 1);
                g.setColor(new Color(c2, true));
                g.drawString(placeholder, ins.left, h / 2 + fm.getAscent() / 2 - 2);
            }
        }
    }
}