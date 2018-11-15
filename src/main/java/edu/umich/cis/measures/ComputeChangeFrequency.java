package edu.umich.cis.measures;

import edu.umich.cis.JdbcConnection;
import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.measures.CoreMetrics;

import java.sql.*;

public class ComputeChangeFrequency implements MeasureComputer {

    @Override
    public MeasureComputerDefinition define(MeasureComputerDefinitionContext def) {
        return def.newDefinitionBuilder()
                .setInputMetrics(CoreMetrics.NCLOC_KEY, ExampleMetrics.FILE_ID.key())
                .setOutputMetrics(ExampleMetrics.CHANGE_FREQUENCY.key())
                .build();
    }

    @Override
    public void compute(MeasureComputerContext context) {
        Measure newLines = context.getMeasure(CoreMetrics.NCLOC_KEY);

        if (context.getComponent().getType() == Component.Type.FILE) {
            if (newLines != null && newLines.getIntValue() > 0) {
                Integer fileId = context.getMeasure(ExampleMetrics.FILE_ID.key()).getIntValue();
                Integer changeFrequency = incrementChangeFrequencyCounter(fileId, newLines.getIntValue());
                context.addMeasure(ExampleMetrics.CHANGE_FREQUENCY.key(), changeFrequency);
            }
        } else {
            context.addMeasure(ExampleMetrics.CHANGE_FREQUENCY.key(), getTotalChangeFrequency());
        }
    }

    private Integer incrementChangeFrequencyCounter(Integer fileId, Integer ncloc) {
        String selectChangeFrequency = "select * from change_frequency where id = ? ";
        String insertChangeFrequency = "insert into change_frequency (id, change_frequency, last_loc_count) values (?, ?, ?) ";
        String updateChangeFrequency = "update change_frequency set change_frequency = ?, last_loc_count = ? where id = ?";
        Integer changeFrequency = 0;
        Integer lastLocCount = 0;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection(JdbcConnection.url, JdbcConnection.user, JdbcConnection.password);

            PreparedStatement selectPreparedStatement = connection.prepareStatement(selectChangeFrequency);
            selectPreparedStatement.setInt(1, fileId);
            ResultSet rs = selectPreparedStatement.executeQuery();

            if (rs.next()) {
                changeFrequency = rs.getInt("change_frequency");
                lastLocCount = rs.getInt("last_loc_count");
            }

            if (lastLocCount != ncloc) {
                changeFrequency++;

                if (changeFrequency == 1) {
                    PreparedStatement preparedStatement = connection.prepareStatement(insertChangeFrequency);
                    preparedStatement.setInt(1, fileId);
                    preparedStatement.setInt(2, changeFrequency);
                    preparedStatement.setInt(3, ncloc);
                    preparedStatement.execute();
                } else {
                    PreparedStatement preparedStatement = connection.prepareStatement(updateChangeFrequency);
                    preparedStatement.setInt(1, changeFrequency);
                    preparedStatement.setInt(2, ncloc);
                    preparedStatement.setInt(3, fileId);
                    preparedStatement.execute();
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return changeFrequency;
    }

    private Integer getTotalChangeFrequency() {
        String query = "select sum(change_frequency) as change_frequency_total from change_frequency ";
        Integer changeFrequencyTotal = 0;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection(JdbcConnection.url, JdbcConnection.user, JdbcConnection.password);
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet rs = preparedStatement.executeQuery();


            if (rs.next()) {
                changeFrequencyTotal = rs.getInt("change_frequency_total");
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return changeFrequencyTotal;
    }

}
