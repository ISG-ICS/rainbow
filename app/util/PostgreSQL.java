package util;

import model.Point;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class PostgreSQL {

    public Connection conn = null;

    public boolean connectDB() {
        try {
            conn = DriverManager.getConnection(Constants.DB_URL, Constants.DB_USERNAME, Constants.DB_PASSWORD);
            System.out.println("Connected to the PostgreSQL server successfully.");
            return true;
        } catch (SQLException e) {
            System.err.println("Connecting to the PostgreSQL server failed. Exceptions:");
            System.err.println(e.getMessage());
            return false;
        }
    }

    public void disconnectDB() {
        try {
            conn.close();
            System.out.println("Disconnected from the PostgreSQL server successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Point> queryPointsForKeyword(String keyword) {

        if (this.conn == null) {
            if(!this.connectDB()) {
                return null;
            }
        }

        System.out.println("Querying PostgreSQL with keyword: [" + keyword + "] ... ...");
        List<Point> result = new ArrayList<>();
        String sql = "SELECT x, y FROM " + Constants.DB_TABLENAME + " WHERE to_tsvector('english', text)@@to_tsquery('english', ?)";
        long start = System.nanoTime();
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, keyword);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Double x = rs.getDouble(1);
                Double y = rs.getDouble(2);
                result.add(new Point(x, y));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        long end = System.nanoTime();
        System.out.println("Querying PostgreSQL with keyword: [" + keyword + "] is done! ");
        System.out.println("Takes time: " + TimeUnit.SECONDS.convert(end - start, TimeUnit.NANOSECONDS) + " seconds");
        System.out.println("Result size: " + result.size());
        return result;
    }

    public List<Point> queryPointsForKeywordAndTime(String keyword, Date sd, Date ed) {

        if (this.conn == null) {
            if(!this.connectDB()) {
                return null;
            }
        }

        System.out.println("Querying PostgreSQL with keyword: [" + keyword + "] and time [" + sd + ", " + ed + "]... ...");
        List<Point> result = new ArrayList<>();
        String sql = "SELECT x, y FROM " + Constants.DB_TABLENAME + " WHERE to_tsvector('english', text)@@to_tsquery('english', ?) and create_at between ? and ?";
        long start = System.nanoTime();
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, keyword);
            statement.setTimestamp(2, new Timestamp(sd.getTime()));
            statement.setTimestamp(3, new Timestamp(ed.getTime()));
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Double x = rs.getDouble(1);
                Double y = rs.getDouble(2);
                result.add(new Point(x, y));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        long end = System.nanoTime();
        System.out.println("Querying PostgreSQL with keyword: [" + keyword + "] and time [" + sd + ", " + ed +  "] is done! ");
        System.out.println("Takes time: " + TimeUnit.SECONDS.convert(end - start, TimeUnit.NANOSECONDS) + " seconds");
        System.out.println("Result size: " + result.size());
        return result;
    }

    public List<Point> queryPointsForTime(Date sd, Date ed) {

        if (this.conn == null) {
            if(!this.connectDB()) {
                return null;
            }
        }

        System.out.println("Querying PostgreSQL with time [" + sd + ", " + ed + "]... ...");
        List<Point> result = new ArrayList<>();
        String sql = "SELECT x, y FROM " + Constants.DB_TABLENAME + " WHERE create_at between ? and ?";
        long start = System.nanoTime();
        try {
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setTimestamp(1, new Timestamp(sd.getTime()));
            statement.setTimestamp(2, new Timestamp(ed.getTime()));
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Double x = rs.getDouble(1);
                Double y = rs.getDouble(2);
                result.add(new Point(x, y));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        long end = System.nanoTime();
        System.out.println("Querying PostgreSQL with time [" + sd + ", " + ed +  "] is done! ");
        System.out.println("Database time: " + TimeUnit.SECONDS.convert(end - start, TimeUnit.NANOSECONDS) + " seconds");
        System.out.println("Result size: " + result.size());
        return result;
    }
}
