/*
 * Copyright 2005-2017 Sixth and Red River Software, Bas Leijdekkers
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.sixrr.metrics.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.content.Content;
import org.picocontainer.Disposable;

import javax.swing.*;
import javax.swing.text.JTextComponent;

import java.awt.*;

import static com.intellij.remoteServer.impl.runtime.ui.DefaultServersToolWindowManager.WINDOW_ID;

public class RefactorRequestGUI implements Disposable{
    private Project project;
    private ToolWindow myToolWindow = null;
    private JTabbedPane component;
    private JTextComponent clusterizationText;
    private JTextComponent statisticText;
    private JTextComponent moveMethodText;
    private JTextComponent classChangeText;
    private JPanel distanciesPanel;

    private RefactorRequestGUI(Project project) {
        this.project = project;
        register();
        createGUI();
    }

    private void register() {
        final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        myToolWindow = toolWindowManager.registerToolWindow(WINDOW_ID, true, ToolWindowAnchor.BOTTOM);
        myToolWindow.setTitle("Refactor analyze results");
        myToolWindow.setAvailable(false, null);
    }

    private void createGUI() {
        component = new JBTabbedPane();
        JPanel panel;
        JScrollPane pane;
        //clusterization
        panel = new JPanel(new GridLayout(1, 1));
        clusterizationText = new JTextPane();
        clusterizationText.setEditable(false);
        pane = new JBScrollPane(clusterizationText);
        panel.add(pane);
        component.addTab("Разбиение на классы", panel);
        //move method actions
        panel = new JPanel(new GridLayout(1, 1));
        moveMethodText = new JTextPane();
        moveMethodText.setEditable(false);
        pane = new JBScrollPane(moveMethodText);
        panel.add(pane);
        component.addTab("Перемещение методов", panel);
        //change class actions
        panel = new JPanel(new GridLayout(1, 1));
        classChangeText = new JTextPane();
        classChangeText.setEditable(false);
        pane = new JBScrollPane(classChangeText);
        panel.add(pane);
        component.addTab("Создание и удаление классов", panel);
        //statistic
        panel = new JPanel(new GridLayout(1, 1));
        statisticText = new JTextPane();
        statisticText.setEditable(false);
        pane = new JBScrollPane(statisticText);
        panel.add(pane);
        component.addTab("Статистика", panel);
        //dist
        panel = new JPanel(new GridLayout(1, 1));
        distanciesPanel = new JPanel();
        pane = new JBScrollPane(distanciesPanel);
        panel.add(pane);
        component.addTab("Функция расстояния", panel);
        /*distanciesPanel = new JPanel(new GridLayout(1, 1));
        component.addTab("Функция расстояния", distanciesPanel);*/

        final Content content = myToolWindow.getContentManager().getFactory()
                .createContent(component, "Refactor analyze results", true);
        myToolWindow.getContentManager().addContent(content);
    }

    public void show(RefactorRequestAction.RefactorRequestResults results) {
        myToolWindow.setTitle("Refactor analyze results");
        myToolWindow.setAvailable(true, null);
        clusterizationText.setText(results.getClusterization());
        moveMethodText.setText(results.getMoveMethods());
        classChangeText.setText(results.getCreateClass() + "\n" + results.getRemoveClass());
        statisticText.setText(results.getStatistic());

        distanciesPanel.removeAll();
        distanciesPanel.setLayout(new GridLayout(results.getElementsNames().size() + 1,
                results.getElementsNames().size() + 1));
        distanciesPanel.add(new JLabel());
        for (String s : results.getElementsNames()) {
            distanciesPanel.add(new JLabel(s));
        }
        for (String s : results.getElementsNames()) {
            distanciesPanel.add(new JLabel(s));
            for (Double d : results.getDistancies().get(s)) {
                if (d < 0) {
                    distanciesPanel.add(new JLabel("Inf"));
                } else {
                    distanciesPanel.add(new JLabel(d.toString()));
                }
            }
        }
        distanciesPanel.setAutoscrolls(true);

        myToolWindow.show(null);
    }

    @Override
    public void dispose() {
        final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        toolWindowManager.unregisterToolWindow(WINDOW_ID);
    }
}
