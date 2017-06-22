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

import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.BaseAnalysisAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.intellij.refactoring.RefactoringFactory;
import com.intellij.ui.table.TableView;
import com.sixrr.metrics.Metric;
import com.sixrr.metrics.MetricCategory;
import com.sixrr.metrics.config.MetricsReloadedConfig;
import com.sixrr.metrics.metricModel.MetricsExecutionContextImpl;
import com.sixrr.metrics.metricModel.MetricsResult;
import com.sixrr.metrics.metricModel.MetricsRunImpl;
import com.sixrr.metrics.metricModel.TimeStamp;
import com.sixrr.metrics.profile.MetricsProfile;
import com.sixrr.metrics.profile.MetricsProfileRepository;
import com.sixrr.metrics.ui.metricdisplay.MetricsToolWindow;
import com.sixrr.metrics.utils.ClassUtils;
import com.sixrr.metrics.utils.MethodUtils;
import com.sixrr.metrics.utils.MetricsReloadedBundle;
import com.sixrr.stockmetrics.i18n.StockMetricsBundle;
import com.sixrr.stockmetrics.utils.ProjectContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.lang.reflect.Method;
import java.util.*;

public class RefactorRequestAction extends BaseAnalysisAction {

    private static final double INF = 1.0;

    public RefactorRequestAction() {
        super(MetricsReloadedBundle.message("metrics.calculation"), MetricsReloadedBundle.message("metrics"));
        //TODO constructor
    }

    @Override
    public void analyze(@NotNull final Project project, @NotNull final AnalysisScope analysisScope) {
        final MetricsProfileRepository repository = MetricsProfileRepository.getInstance();
        final MetricsProfile profile = repository.getProfileForName(StockMetricsBundle.message("automatic.refactoring.profile.name"));
        final MetricsToolWindow toolWindow = MetricsToolWindow.getInstance(project);
        final MetricsRunImpl metricsRun = new MetricsRunImpl();
        ProjectContainerUtil.setProject(project);
        new MetricsExecutionContextImpl(project, analysisScope) {

            @Override
            public void onFinish() {

                final boolean showOnlyWarnings = MetricsReloadedConfig.getInstance().isShowOnlyWarnings();
                if(!metricsRun.hasWarnings(profile) && showOnlyWarnings) {
                    ToolWindowManager.getInstance(project).notifyByBalloon(MetricsToolWindow.METRICS_TOOL_WINDOW_ID,
                            MessageType.INFO, MetricsReloadedBundle.message("no.metrics.warnings.found"));
                    return;
                }

                MetricsResult classResults = metricsRun.getResultsForCategory(MetricCategory.Class);
                MetricsResult methodResults =  metricsRun.getResultsForCategory(MetricCategory.Method);
                ArrayList<PsiMethod> methods = new ArrayList<PsiMethod>();
                ArrayList<PsiClass> classes = new ArrayList<PsiClass>();
                HashMap<PsiElement, String> names = new HashMap<PsiElement, String>();
                for (String s : methodResults.getMeasuredObjects()) {
                    PsiElement el = methodResults.getElementForMeasuredObject(s);
                    if (el != null) {
                        methods.add((PsiMethod)el);
                        names.put(el, s);
                    }
                }
                for (String s : classResults.getMeasuredObjects()) {
                    PsiElement el = classResults.getElementForMeasuredObject(s);
                    if (el != null) {
                        classes.add((PsiClass)el);
                        names.put(el, s);
                    }
                }

                List<Metric> methodMetrics = Arrays.asList(methodResults.getMetrics());
                Collections.sort(methodMetrics, new Comparator<Metric>() {
                    @Override
                    public int compare(Metric o1, Metric o2) {
                        return o1.getDisplayName().compareTo(o2.getDisplayName());
                    }
                });
                List<Metric> classMetrics = Arrays.asList(classResults.getMetrics());
                Collections.sort(classMetrics, new Comparator<Metric>() {
                    @Override
                    public int compare(Metric o1, Metric o2) {
                        return o1.getDisplayName().compareTo(o2.getDisplayName());
                    }
                });

                HashMap<PsiElement, List<Double>> metricsVectors = new HashMap<PsiElement, List<Double>>();
                Double sum = 0.0;
                for (PsiElement el : methods) {
                    for (Metric m : methodMetrics) {
                        sum = sum + methodResults.getValueForMetric(m, names.get(el));
                    }
                }
                for (PsiElement el : classes) {
                    for (Metric m : classMetrics) {
                        sum = sum + classResults.getValueForMetric(m, names.get(el));
                    }
                }
                for (PsiElement el : methods) {
                    ArrayList<Double> metricsValue = new ArrayList<Double>();
                    for (Metric m : methodMetrics) {
                        metricsValue.add(methodResults.getValueForMetric(m, names.get(el))/sum);
                    }
                    metricsVectors.put(el, metricsValue);
                }
                for (PsiElement el : classes) {
                    ArrayList<Double> metricsValue = new ArrayList<Double>();
                    for (Metric m : classMetrics) {
                        metricsValue.add(classResults.getValueForMetric(m, names.get(el))/sum);
                    }
                    metricsVectors.put(el, metricsValue);
                }

                HashMap<PsiMethod, List<PsiMethod>> methodChildren = new HashMap<PsiMethod, List<PsiMethod>>();
                for (PsiMethod el : methods) {
                    methodChildren.put(el, new ArrayList<PsiMethod>());
                }
                for (PsiMethod el : methods) {
                    for (PsiMethod method : el.findSuperMethods()) {
                        if (methodChildren.containsKey(method)) {
                            methodChildren.get(method).add(el);
                        }
                    }
                }
                HashMap<PsiElement, Set<PsiElement>> features = new HashMap<PsiElement, Set<PsiElement>>();
                for (PsiElement el : methods) {
                    features.put(el,
                            calculateFeatures((PsiMethod)el,
                                    methodChildren.get(el)));
                }
                for (PsiElement el : classes) {
                    features.put(el,
                            calculateFeatures((PsiClass)el));
                }

                //Clusterization
                HashMap<PsiElement, List<PsiMethod>> clusters = new HashMap<PsiElement, List<PsiMethod>>();
                ArrayList<PsiElement> newClusters = new ArrayList<PsiElement>();
                for (PsiElement el : classes) {
                    clusters.put(el, new ArrayList<PsiMethod>());
                }
                for (PsiMethod m : methods) {
                    double closestDistance = INF * 2;
                    PsiElement closest = null;
                    for (PsiElement el : classes) {
                        double diffConstant = calculateDiffKoef(
                                features.get(el),
                                features.get(m)
                        );
                        if (diffConstant == 0) {
                            continue;
                        }
                        double distance = calculateDistance(metricsVectors.get(el),
                                metricsVectors.get(m), diffConstant);
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closest = el;
                        }
                    }
                    if (closestDistance < INF) {
                        clusters.get(closest).add(m);
                        continue;
                    }
                    for (PsiElement el : newClusters) {
                        double diffConstant = calculateDiffKoef(
                                features.get(el),
                                features.get(m)
                        );
                        if (diffConstant == 0) {
                            continue;
                        }
                        double distance = calculateDistance(metricsVectors.get(el),
                                metricsVectors.get(m), diffConstant);
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closest = el;
                        }
                    }
                    if (closestDistance < INF) {
                        clusters.get(closest).add(m);
                        continue;
                    }
                    newClusters.add(m);
                    clusters.put(m, new ArrayList<PsiMethod>());
                    clusters.get(m).add(m);
                }

                StringBuilder clusterization = new StringBuilder();
                StringBuilder moveMethod = new StringBuilder();
                StringBuilder removeClass = new StringBuilder();
                StringBuilder createClass = new StringBuilder();
                StringBuilder statistic = new StringBuilder();
                for (PsiElement el : clusters.keySet()) {
                    clusterization.append("Cluster \"" + names.get(el) + "\" should contain "
                            + clusters.get(el).size() + " methods:" + "\n");
                    for (PsiElement e : clusters.get(el)) {
                        clusterization.append("---> " + names.get(e) + "\n");
                    }
                    clusterization.append("\n");
                }

                int moveMethodTotal = 0;
                int createClassTotal = 0;
                int removeClassTotal = 0;

                for (PsiClass cl : classes) {
                    if (clusters.get(cl).isEmpty()) {
                        removeClass.append("Remove class: " + names.get(cl) + "\n");
                        removeClassTotal++;
                    }
                    for (PsiMethod el : clusters.get(cl)) {
                        if (!cl.equals(el.getContainingClass())) {
                            moveMethod.append("Move method " + names.get(el) + " to class " + names.get(cl) + "\n");
                            moveMethodTotal++;
                        }
                    }
                }
                for (PsiElement el : newClusters) {
                    createClass.append("Create new class for " + names.get(el) + "\n");
                    createClassTotal++;
                    for (PsiMethod m : clusters.get(el)) {
                        moveMethod.append("Move method " + names.get(m) + " to class " + names.get(el) + "\n");
                        moveMethodTotal++;
                    }
                }

                statistic.append("Move Method count: " + moveMethodTotal + "\n");
                statistic.append("Create Class count: " + createClassTotal + "\n");
                statistic.append("Remove Class count: " + removeClassTotal + "\n");
                statistic.append("Method Count: " + methods.size() + "\n");
                statistic.append("ClassCount: " + classes.size() + "\n");

                ArrayList<PsiElement> els = new ArrayList<PsiElement>(names.keySet());
                ArrayList<String> n = new ArrayList<String>();
                HashMap<String, ArrayList<Double>> dist = new HashMap<String, ArrayList<Double>>();
                for (PsiElement el1 : els) {
                    ArrayList<Double> tmp = new ArrayList<Double>();
                    n.add(names.get(el1));
                    for (PsiElement el2 : els) {
                        double diffConstant = calculateDiffKoef(
                                features.get(el1),
                                features.get(el2)
                        );
                        if (diffConstant == 0) {
                            tmp.add(-1.0);
                            continue;
                        }
                        tmp.add(calculateDistance(metricsVectors.get(el1),
                                metricsVectors.get(el2), diffConstant));
                    }
                    dist.put(names.get(el1), tmp);
                }

                ServiceManager.getService(project, RefactorRequestGUI.class).show(
                        new RefactorRequestResults(
                                clusterization.toString(),
                                moveMethod.toString(),
                                createClass.toString(),
                                removeClass.toString(),
                                statistic.toString(),
                                n,
                                dist)
                );
            }
        }.execute(profile, metricsRun);
    }

    private double calculateDistance(List<Double> d1, List<Double> d2, double diffConstant) {
        double preanswer = INF - diffConstant;
        double sum = 0.0;
        for (int i = 0; i < d1.size(); i++) {
            sum = sum + (d1.get(i) - d2.get(i)) * (d1.get(i) - d2.get(i));
        }
        return Math.sqrt((preanswer + sum)/(d1.size()));
    }

    private double calculateDiffKoef(Set<PsiElement> features1, Set<PsiElement> features2) {
        Set<PsiElement> intersection = new HashSet<PsiElement>(features1);
        intersection.retainAll(features2);
        Set<PsiElement> union = new HashSet<PsiElement>(features1);
        union.addAll(features2);
        return (double)intersection.size() / (double)union.size();
    }

    private Set<PsiElement> calculateFeatures(PsiClass psiClass) {
        HashSet<PsiElement> result = new HashSet<PsiElement>();
        result.add(psiClass);
        Collections.addAll(result, psiClass.getMethods());
        Collections.addAll(result, psiClass.getFields());
        HashSet<PsiElement> tmp = new HashSet<PsiElement>();
        Collections.addAll(tmp, psiClass.getSupers());
        Collections.addAll(tmp, psiClass.getInterfaces());
        tmp.retainAll(ProjectContainerUtil.getClasses());
        result.addAll(tmp);
        return result;
    }

    private Set<PsiElement> calculateFeatures(PsiMethod psiMethod, List<PsiMethod> additionalFeatures) {
        HashSet<PsiElement> result = new HashSet<PsiElement>(additionalFeatures);
        HashSet<PsiElement> methods = new HashSet<PsiElement>();
        HashSet<PsiElement> fields = new HashSet<PsiElement>();
        methods.add(psiMethod);
        Collections.addAll(methods, psiMethod.findSuperMethods());
        if (psiMethod.getBody() != null) {
            recursiveFeatureGetter(psiMethod.getBody(), methods, fields);
        }
        result.add(psiMethod.getContainingClass());
        methods.retainAll(ProjectContainerUtil.getMethods());
        result.addAll(methods);
        result.addAll(fields);
        return result;
    }

    private void recursiveFeatureGetter(PsiElement element, Set<PsiElement> methods, Set<PsiElement> fields) {
        if (element != null) {
            for (PsiElement el : element.getChildren()) {
                if (el instanceof PsiReference) {
                    PsiElement e = ((PsiReference) el).resolve();
                    if (e == null) {
                        continue;
                    }
                    if (e instanceof PsiMethod) {
                        methods.add(e);
                    }
                    if (e instanceof PsiField) {
                        fields.add(e);
                    }
                } else {
                    recursiveFeatureGetter(el, methods, fields);
                }
            }
        }
    }

    public class RefactorRequestResults {
        private String clusterization;
        private String moveMethods;
        private String createClass;
        private String removeClass;
        private String statistics;
        private ArrayList<String> elementNames;
        private HashMap<String, ArrayList<Double>> distancies;

        public RefactorRequestResults(
                String clusterization,
                String moveMethods,
                String createClass,
                String removeClass,
                String statistics,
                ArrayList<String> elementNames,
                HashMap<String, ArrayList<Double>> distancies) {
            this.clusterization = clusterization;
            this.moveMethods = moveMethods;
            this.createClass = createClass;
            this.removeClass = removeClass;
            this.statistics = statistics;
            this.elementNames = elementNames;
            this.distancies = distancies;
        }

        public String getClusterization() {
            return clusterization;
        }

        public String getStatistic() {
            return statistics;
        }

        public String getMoveMethods() {
            return moveMethods;
        }

        public String getCreateClass() {
            return createClass;
        }

        public String getRemoveClass() {
            return removeClass;
        }

        public ArrayList<String> getElementsNames() {
            return elementNames;
        }

        public HashMap<String, ArrayList<Double>> getDistancies() {
            return distancies;
        }
    }
}
