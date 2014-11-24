/* Authors: Xaymar
 * Copyright: 2012-2013 (c) Inception Plugin Team.
 * License: CC BY-SA 3.0
 *      Inception by Inception Plugin Team is licensed under a
 *      Creative Commons Attribution-ShareAlike 3.0 Unported
 *      License.
 */
package de.RealityBends.Inception.World;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Cache {
    private String oDatabaseURL;
    private Connection oConnection;

    public Cache(File poDatabaseFile) throws ClassNotFoundException {
        oDatabaseURL = "jdbc:sqlite:" + poDatabaseFile.getAbsolutePath();
        
        Class.forName("org.sqlite.JDBC");
    }
    
    public void open() throws SQLException {
        oConnection = DriverManager.getConnection(oDatabaseURL);
    }
    
    public void close() throws SQLException {
        if (oConnection != null) {
            oConnection.close();
        }
    }
    
    public void execute(String pstQuery) throws SQLException {
        oConnection.createStatement().execute(pstQuery);
    }
    
    public ResultSet executeQuery(String pstQuery) throws SQLException {
        Statement oStatement = oConnection.createStatement();
        ResultSet oResult = oStatement.executeQuery(pstQuery);
        oStatement.close();
        return oResult;
        //return oConnection.createStatement().executeQuery(pstQuery);
    }
    
    public PreparedStatement prepareStatement(String pstQuery) throws SQLException {
        return oConnection.prepareStatement(pstQuery);
    }
}
