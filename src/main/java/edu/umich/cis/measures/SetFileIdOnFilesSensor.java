package edu.umich.cis.measures;

import edu.umich.cis.JdbcConnection;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;

import java.sql.*;

public class SetFileIdOnFilesSensor implements Sensor {
    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor.name("File Id for database");
    }

    @Override
    public void execute(SensorContext context) {
        FileSystem fs = context.fileSystem();
        for (InputFile file : fs.inputFiles(fs.predicates().hasType(InputFile.Type.MAIN))) {
            context.<Integer>newMeasure()
                    .forMetric(ExampleMetrics.FILE_ID)
                    .on(file)
                    .withValue(getDbFileId(file.filename()))
                    .save();
        }
    }

    private Integer getDbFileId(String filename) {
        String selectByFilename = "select * from file where filename = ? ";
        String selectMaxId = "select max(id) id from file ";
        String insertFileId = "insert into file (filename) values (?) ";
        int id = 0;
        filename.trim();
        boolean doInsert = false;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection(JdbcConnection.url, JdbcConnection.user, JdbcConnection.password);
            PreparedStatement selectStatement = connection.prepareStatement(selectByFilename);
            selectStatement.setString(1, filename);
            ResultSet rs = selectStatement.executeQuery();

            if (rs.next()) {
                id = rs.getInt("id");

                if (id == 0) {
                    doInsert = true;
                }
            } else {
                doInsert = true;
            }

            if (doInsert) {
                PreparedStatement selectMaxIdPS = connection.prepareStatement(selectMaxId);
                ResultSet rsMax = selectMaxIdPS.executeQuery();

                if (rsMax.next()) {
                    id = rsMax.getInt("id") + 1;
                }

                PreparedStatement preparedStatement = connection.prepareStatement(insertFileId);
                preparedStatement.setString(1, filename);
                preparedStatement.executeUpdate();
            }

            connection.close();
            return id;
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return id;
    }
}