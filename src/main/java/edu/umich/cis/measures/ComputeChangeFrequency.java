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

        /*
         * setInputMetrics() : Set the metrics to read(instead of line number, we read sqale rating and sqale debt ratio.
         * setOutputMetrics() : Set the metrics to write(instead of change frequency, we output the maintainability delta changes.
         *
         */
        return def.newDefinitionBuilder()
                .setInputMetrics(CoreMetrics.NCLOC_KEY, CoreMetrics.SQALE_RATING_KEY, CoreMetrics.SQALE_DEBT_RATIO_KEY, ExampleMetrics.FILE_ID.key())
                .setOutputMetrics(ExampleMetrics.CHANGE_FREQUENCY.key(), ExampleMetrics.CHANGE_MAINTAINABILITY.key())
                .build();
    }

    @Override
    public void compute(MeasureComputerContext context) {
        Measure newLines = context.getMeasure(CoreMetrics.NCLOC_KEY);
        Measure newSqaleRating = context.getMeasure(CoreMetrics.SQALE_RATING_KEY); 			// Read the sqale rating.
        Measure newSqaleDebtRatio = context.getMeasure(CoreMetrics.SQALE_DEBT_RATIO_KEY); 	// Read the sqal debt ratio.
        Double changes = 0.0;
        if (context.getComponent().getType() == Component.Type.FILE) {
            if (newLines != null && newLines.getIntValue() > 0) {
                Integer fileId = context.getMeasure(ExampleMetrics.FILE_ID.key()).getIntValue();
                Integer changeFrequency = incrementChangeFrequencyCounter(fileId, newLines.getIntValue());
                if (newSqaleRating != null && newSqaleDebtRatio != null) {

                    // Save sqale rating & sqale debt ratio to DB and calculates the changes between the old sqale debt ration and the new one.
                    changes = saveToDB(fileId, newSqaleRating.getIntValue(), newSqaleDebtRatio.getDoubleValue());
                    // Output the changes of sqale debt ratio to the dashboard.
                    context.addMeasure(ExampleMetrics.CHANGE_MAINTAINABILITY.key(), changes);
                }
                context.addMeasure(ExampleMetrics.CHANGE_FREQUENCY.key(), changeFrequency);
            }
        } else {
            context.addMeasure(ExampleMetrics.CHANGE_FREQUENCY.key(), getTotalChangeFrequency());
            // if not the file, output the changes of sqale debt ratio of all files(whole project) to the dashboard
            context.addMeasure(ExampleMetrics.CHANGE_MAINTAINABILITY.key(), getTotalChangeMaintainability());
        }

    }

    private Double saveToDB(Integer fileId, Integer newSqaleRating, Double newSqaleDebtRatio) {
        String selectChangeSqale = "select * from change_sqale where id = ?";
        String insertChangeSqale = "insert into change_sqale (id, old_sqale_rating, new_sqale_rating, old_sqale_ratio, new_sqale_ratio, change_sqale_ratio) values (?, ?, ?, ?, ?, ?) ";
        String updateChangeSqale = "update change_sqale set old_sqale_rating = ?, new_sqale_rating = ?, old_sqale_ratio = ?, new_sqale_ratio = ?, change_sqale_ratio = ? where id = ?";
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection(JdbcConnection.url, JdbcConnection.user, JdbcConnection.password);
            PreparedStatement preparedStatement = connection.prepareStatement(selectChangeSqale);
            preparedStatement.setInt(1, fileId);
            ResultSet rs = preparedStatement.executeQuery();
            if(!rs.next()) {  // if the specified file is not found in the database, insert.
                preparedStatement = connection.prepareStatement(insertChangeSqale);
                preparedStatement.setInt(1, fileId);
                preparedStatement.setInt(2, newSqaleRating);	// set the old sqale rating & new sqale rating same because no record exists in the database.
                preparedStatement.setInt(3, newSqaleRating);
                preparedStatement.setDouble(4, newSqaleDebtRatio);
                preparedStatement.setDouble(5, newSqaleDebtRatio); // set the sqale debt ratio.
                preparedStatement.setDouble(6, 0);					// set the changes of sqale debt ratio as 0 because this is the first scan of the project(empty database).
                preparedStatement.executeUpdate();
                connection.commit();
                connection.close();
                return 0.0;
            } else {			// if the specified file is found in the database, update.
                Integer oldSqaleRating = rs.getInt("new_sqale_rating");  // read the old sqale rating of the file.
                Double oldSqaleDebtRatio = rs.getDouble("old_sqale_ratio"); // read the old sqale debt ratio of the file.
                Double changeSqaleDebtRatio = newSqaleDebtRatio - oldSqaleDebtRatio;  // calculates the differences between old sqale debt ratio and new one of the file.
                preparedStatement = connection.prepareStatement(updateChangeSqale);		// update the records of the file.
                preparedStatement.setInt(6, fileId);
                preparedStatement.setInt(1, oldSqaleRating);
                preparedStatement.setInt(2, newSqaleRating);
                preparedStatement.setDouble(3, oldSqaleDebtRatio);
                preparedStatement.setDouble(4, newSqaleDebtRatio);
                preparedStatement.setDouble(5, changeSqaleDebtRatio);
                preparedStatement.executeUpdate();
                connection.commit();
                connection.close();
                return changeSqaleDebtRatio;
            }


        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return 0.0;
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
                    preparedStatement.executeUpdate();
                } else {
                    PreparedStatement preparedStatement = connection.prepareStatement(updateChangeFrequency);
                    preparedStatement.setInt(1, fileId);
                    preparedStatement.setInt(2, changeFrequency);
                    preparedStatement.setInt(3, ncloc);
                    preparedStatement.executeUpdate();
                    preparedStatement.setInt(1, changeFrequency);
                }
                connection.commit();
            }
            connection.close();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }



        return changeFrequency;
    }

    private Integer getTotalChangeFrequency() {
        String query = "select sum(change_frequency) as change_frequency_total from change_frequency where id != 0 ";
        Integer changeFrequencyTotal = 0;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection(JdbcConnection.url, JdbcConnection.user, JdbcConnection.password);
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet rs = preparedStatement.executeQuery();


            if (rs.next()) {
                changeFrequencyTotal = rs.getInt("change_frequency_total");
            }
            connection.close();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return changeFrequencyTotal;
    }

    private Double getTotalChangeMaintainability() {

        // sum the changes of sqale debt ratio of all files in the database.
        String query = "select sum(change_sqale_ratio) as change_sqale_total from change_sqale ";
        Double changeMaintainabilityTotal = 0.0;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection(JdbcConnection.url, JdbcConnection.user, JdbcConnection.password);
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next()) {
                changeMaintainabilityTotal = rs.getDouble("change_sqale_total"); // get the total changes.
            }
            connection.close();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return changeMaintainabilityTotal;  // return the total changes.
    }

}