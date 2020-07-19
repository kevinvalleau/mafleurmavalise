package worker;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import java.sql.*;
import org.json.JSONObject;

class Worker {
  /**
   * Maim method of the worker
   * Loop through all the redis entries and update votes to postgresql
   * @param args
   */
  public static void main(String[] args) {
    try {
      Jedis redis = connectToRedis("redis");
      Connection dbConn = connectToDB("db");

      System.err.println("Watching vote queue");

      while (true) {
        String voteJSON = redis.blpop(0, "votes").get(1);
        JSONObject voteData = new JSONObject(voteJSON);
        String voterID = voteData.getString("voter_id");
        String vote = voteData.getString("vote");
        String name = voteData.getString("name");
        String date = voteData.getString("date");

        System.err.printf("Processing vote for '%s' by '%s %s the %s'\n", vote, voterID, name, date);
        updateVote(dbConn, voterID, vote, name, date);
      }
    } catch (SQLException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Method to insert or update the vote if it already exists
   * @param dbConn db connection to PostgreSql
   * @param voterID voter id
   * @param vote voted choice
   * @param name name of the voter
   * @param date date of the vote
   * @throws SQLException
   */
  static void updateVote(Connection dbConn, String voterID, String vote, String name, String date) throws SQLException {
    PreparedStatement insert = dbConn.prepareStatement(
      "INSERT INTO votes (id, vote, name, date) VALUES (?, ?, ?, ?)");
    insert.setString(1, voterID);
    insert.setString(2, vote);
    insert.setString(3, name);
    insert.setString(4, date);

    try {
      insert.executeUpdate();
    } catch (SQLException e) {
      PreparedStatement update = dbConn.prepareStatement(
        "UPDATE votes SET vote = ? WHERE id = ?");
      update.setString(1, vote);
      update.setString(2, voterID);
      update.executeUpdate();
    }
  }

  /**
   * Method to connect to redis
   * @param host host url
   * @return Jedis connection
   */
  static Jedis connectToRedis(String host) {
    Jedis conn = new Jedis(host);

    while (true) {
      try {
        conn.keys("*");
        break;
      } catch (JedisConnectionException e) {
        System.err.println("Waiting for redis");
        sleep(1000);
      }
    }

    System.err.println("Connected to redis");
    return conn;
  }

  /**
   * Method to connect to PostgreSql. Creates the table if needed.
   * @param host host url
   * @return Connection to PostgreSql
   * @throws SQLException
   */
  static Connection connectToDB(String host) throws SQLException {
    Connection conn = null;

    try {

      Class.forName("org.postgresql.Driver");
      String url = "jdbc:postgresql://" + host + "/postgres";

      while (conn == null) {
        try {
          conn = DriverManager.getConnection(url, "postgres", "postgres");
        } catch (SQLException e) {
          System.err.println("Waiting for db");
          sleep(1000);
        }
      }

      PreparedStatement st = conn.prepareStatement(
        "CREATE TABLE IF NOT EXISTS votes (id VARCHAR(255) NOT NULL UNIQUE, vote VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, date VARCHAR(255) NOT NULL)");
      st.executeUpdate();

    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      System.exit(1);
    }

    System.err.println("Connected to db");
    return conn;
  }

  /**
   * Sleep method
   * @param duration duration in milliseconds
   */
  static void sleep(long duration) {
    try {
      Thread.sleep(duration);
    } catch (InterruptedException e) {
      System.exit(1);
    }
  }
}
