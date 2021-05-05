package com.anatawa12.jarInJar.creator.gui;

import com.anatawa12.jarInJar.creator.EmbedJarInJar;
import com.anatawa12.jarInJar.creator.Logger;
import com.anatawa12.jarInJar.creator.TargetPreset;
import com.anatawa12.jarInJar.creator.Utils;
import com.anatawa12.jarInJar.creator.commandline.LoggingListener;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

public class MainFrame extends JFrame {
    private static final int titleColumn = 0;
    private static final int valueColumn = 1;
    private static final int maxColumn = 2;

    private static final int sourceRow = 0;
    private static final int targetRow = 1;
    private static final int createRow = 2;

    final JButton selectSourceJarButton;
    final JComboBox<TargetPresetElement> targetPreset;
    File inputFile;

    MainFrame(){
        setSize(500, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Jar In Jar Creator");
        setResizable(false);

        JPanel globalPanel = new JPanel();
        globalPanel.setLayout(new BoxLayout(globalPanel, BoxLayout.Y_AXIS));

        {
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new GridBagLayout());
            mainPanel.setVisible(true);

            addGridded(mainPanel, newJLabel("source jar"), titleColumn, sourceRow);
            selectSourceJarButton = new JButton("please select file");
            selectSourceJarButton.addActionListener(this::selectSourceJar);
            addGridded(mainPanel, selectSourceJarButton, valueColumn, sourceRow);

            addGridded(mainPanel, newJLabel("target version"), titleColumn, targetRow);
            targetPreset = new JComboBox<>(new TargetPresetElement[]{
                    new TargetPresetElement("1.6.2 ~ 1.7.10", TargetPreset.FMLInCpw),
                    new TargetPresetElement("1.8.0 ~ 1.12.2", TargetPreset.FMLInForge),
            });
            addGridded(mainPanel, targetPreset, valueColumn, targetRow);

            {
                JButton create = new JButton("create jar in jar!");
                create.addActionListener(this::createJar);
                GridBagConstraints constraints = new GridBagConstraints();
                constraints.gridx = 0;
                constraints.gridy = createRow;
                constraints.gridwidth = maxColumn + 1;
                constraints.fill = GridBagConstraints.HORIZONTAL;
                constraints.anchor = GridBagConstraints.NORTHWEST;
                ((GridBagLayout) mainPanel.getLayout()).setConstraints(create, constraints);
                mainPanel.add(create);
            }
            globalPanel.add(mainPanel);
        }

        getContentPane().add(globalPanel);
    }

    private JLabel newJLabel(String text) {
        JLabel label = new JLabel();
        label.setText(text);
        return label;
    }

    private void addGridded(JPanel panel, JComponent component, int x, int y) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        ((GridBagLayout)panel.getLayout()).setConstraints(component, constraints);
        panel.add(component);
    }

    public void selectSourceJar(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("jar or zip file", "jar", "zip"));

        int selected = fileChooser.showOpenDialog(this);
        if (selected == JFileChooser.APPROVE_OPTION) {
            inputFile = fileChooser.getSelectedFile();
            selectSourceJarButton.setText(inputFile.getName());
            Logger.INSTANCE.trace("selected file is " + inputFile);
        }
    }

    public void createJar(ActionEvent e) {
        EmbedJarInJar embedJarInJar = new EmbedJarInJar();
        try {
            embedJarInJar.input = new FileInputStream(inputFile);
        } catch (FileNotFoundException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
            JOptionPane.showMessageDialog(this, "no input file specified OR file not found.",
                    "ERROR", JOptionPane.ERROR_MESSAGE);
        }
        embedJarInJar.target = ((TargetPresetElement) Objects.requireNonNull(targetPreset.getSelectedItem())).preset;
        File temp;
        try {
            temp = File.createTempFile("jar-in-jar-dest-test", ".jar");
            embedJarInJar.destination = new FileOutputStream(temp);
            embedJarInJar.listener = new LoggingListener();
            embedJarInJar.runTask();
        } catch (IOException ioException) {
            ioException.printStackTrace();
            JOptionPane.showMessageDialog(this, "error creating jar in jar\n"
                            + ioException.getMessage(),
                    "ERROR", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (Exception exception) {
            exception.printStackTrace();
            JOptionPane.showMessageDialog(this, "internal error creating jar in jar\n"
                            + exception.getMessage(),
                    "ERROR", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("jar file", "jar"));

        int selected = fileChooser.showSaveDialog(this);
        if (selected == JFileChooser.APPROVE_OPTION) {
            try (FileOutputStream out = new FileOutputStream(fileChooser.getSelectedFile());
                 FileInputStream in = new FileInputStream(temp)) {
                Utils.copyStream(in, out, false);
            } catch (IOException ioException) {
                ioException.printStackTrace();
                JOptionPane.showMessageDialog(this, "error writing jar in jar\n"
                                + ioException.getMessage(),
                        "ERROR", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else {
            Logger.INSTANCE.trace("canceled to write");
            return;
        }
        Logger.INSTANCE.trace("finished writing to " + fileChooser.getSelectedFile());
    }

    public static class TargetPresetElement {
        private final String name;
        final TargetPreset preset;

        public TargetPresetElement(String name, TargetPreset preset) {
            this.name = name;
            this.preset = preset;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
