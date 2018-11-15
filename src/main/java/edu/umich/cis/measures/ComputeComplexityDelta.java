package edu.umich.cis.measures;

import edu.umich.cis.JdbcConnection;
import edu.umich.cis.objects.Complexity;
import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.measures.CoreMetrics;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ComputeComplexityDelta implements MeasureComputer {

    @Override
    public MeasureComputerDefinition define(MeasureComputerDefinitionContext def) {
        return def.newDefinitionBuilder()
                .setInputMetrics(CoreMetrics.COMPLEXITY_KEY, ExampleMetrics.FILE_ID.key())
                .setOutputMetrics(ExampleMetrics.COMPLEXITY_DELTA.key())
                .build();
    }

    @Override
    public void compute(MeasureComputerContext context) {
        Measure complexity = context.getMeasure(CoreMetrics.COMPLEXITY_KEY);

        if (context.getComponent().getType() == Component.Type.FILE) {
            if (complexity != null) {
                Integer fileId = context.getMeasure(ExampleMetrics.FILE_ID.key()).getIntValue();

                insertComplexity(fileId, complexity.getIntValue());
                List<Complexity> complexities = getComplexityHistory(fileId);
                complexities.sort(Comparator.comparing(Complexity::getLocalDateTime));

                int complexityDelta = complexity.getIntValue();
                if (complexities.size() > 1) {
                    complexityDelta = complexity.getIntValue() - complexities.get(complexities.size()-2).getComplexity();
                }

                context.addMeasure(ExampleMetrics.COMPLEXITY_DELTA.key(), complexityDelta);
            }
        } else {
            context.addMeasure(ExampleMetrics.COMPLEXITY_DELTA.key(), getComplexityDeltaTotal());
        }
    }

    private Integer getComplexityDeltaTotal() {
        String previousComplexityQuery = "select sum(c1.complexity) from ( " +
                " select id, max(timestamp) as maxTime " +
                "    from complexity " +
                "    where timestamp not in (select max(timestamp) from complexity group by id) " +
                "    group by id " +
                ") c2 " +
                "inner join complexity c1 on c1.id = c2.id and c1.timestamp = c2.maxTime ";
        String currentComplexityQuery = "select sum(c1.complexity) from ( " +
                " select id, max(timestamp) as maxTime " +
                "    from complexity " +
                "    group by id " +
                ") c2 " +
                "inner join complexity c1 on c1.id = c2.id and c1.timestamp = c2.maxTime ";
        Integer previousComplexity = 0;
        Integer currentComplexity = 0;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection(JdbcConnection.url, JdbcConnection.user, JdbcConnection.password);
            Statement statement = connection.createStatement();
            ResultSet prevRs = statement.executeQuery(previousComplexityQuery);

            if (prevRs.next()) {
                previousComplexity = prevRs.getInt(1);
            }

            statement.executeQuery(currentComplexityQuery);
            ResultSet currentRs = statement.executeQuery(currentComplexityQuery);

            if (currentRs.next()) {
                currentComplexity = currentRs.getInt(1);
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return currentComplexity - previousComplexity;
    }

    private void insertComplexity(Integer fileId, Integer filename) {
        String insertFileId = "insert into complexity (id, complexity) values (?, ?) ";

        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection(JdbcConnection.url, JdbcConnection.user, JdbcConnection.password);
            PreparedStatement preparedStatement = connection.prepareStatement(insertFileId);
            preparedStatement.setInt(1, fileId);
            preparedStatement.setInt(2, filename);
            preparedStatement.execute();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private List<Complexity> getComplexityHistory(Integer fileId) {
        String query = "select * from complexity where id = ? ";
        List<Complexity> complexities = new ArrayList<>();

        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection(JdbcConnection.url, JdbcConnection.user, JdbcConnection.password);
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, fileId);
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                Complexity complexity = new Complexity();
                complexity.setComplexity(rs.getInt("complexity"));
                complexity.setLocalDateTime(rs.getTimestamp("timestamp").toLocalDateTime());
                complexities.add(complexity);
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return complexities;
    }

}
