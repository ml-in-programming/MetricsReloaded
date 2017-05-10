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

package com.sixrr.stockmetrics.classMetrics;

import com.sixrr.metrics.MetricCalculator;
import com.sixrr.metrics.MetricType;
import com.sixrr.stockmetrics.classCalculators.LooseClassCouplingCalculator;
import com.sixrr.stockmetrics.i18n.StockMetricsBundle;
import org.jetbrains.annotations.NotNull;

/**
 * @author Aleksandr Chudov.
 */
public class LooseClassCouplingMetric extends ClassMetric {
    @NotNull
    @Override
    public String getDisplayName() {
        return StockMetricsBundle.message("loose.class.coupling.class.metric.display.name");
    }

    @NotNull
    @Override
    public String getAbbreviation() {
        return StockMetricsBundle.message("loose.class.coupling.class.metric.abbreviation");
    }

    @NotNull
    @Override
    public MetricType getType() {
        return MetricType.Ratio;
    }

    @NotNull
    @Override
    public MetricCalculator createCalculator() {
        return new LooseClassCouplingCalculator();
    }
}
