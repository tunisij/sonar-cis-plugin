package edu.umich.cis.measures;

import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;

public class TestMeasure implements MeasureComputer {

    @Override
    public MeasureComputerDefinition define(MeasureComputerDefinitionContext def) {
        return def.newDefinitionBuilder()
                .setOutputMetrics(ExampleMetrics.TEST_PROJECT_SIZE.key())
                .build();
    }

    @Override
    public void compute(MeasureComputerContext context) {
        // measure is already defined on files by {@link SetSizeOnFilesSensor}
        // in scanner stack
        if (context.getComponent().getType() != Component.Type.FILE) {
            int sum = 0;
            int count = 0;
            for (Measure child : context.getChildrenMeasures(ExampleMetrics.TEST_PROJECT_SIZE.key())) {
                sum += child.getIntValue();
                count++;
            }
            int average = count == 0 ? 0 : sum / count;
            context.addMeasure(ExampleMetrics.TEST_PROJECT_SIZE.key(), average);
        }
    }
}
