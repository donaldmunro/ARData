package to.augmented.reality.android.ardb.jdbc;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public interface IJdbcRequestHandler
//==================================
{
   Connection connect(String driver, String url,
                      StringBuilder errbuf) throws SQLException, ClassNotFoundException;

   PreparedStatement prepare(Connection connection, String select, Map<String, Object> params,
                             Map<String, int[]> paramIndices) throws SQLException;

   boolean setParameters(PreparedStatement pst, Map<String, Object> params, Map<String, int[]> paramIndices,
                         StringBuilder errbuf) throws SQLException;

   Cursor select(PreparedStatement query, SQLiteDatabase localDatabase, String localTableName,
                 boolean isReplace, StringBuilder errbuf) throws SQLException;

   int modify(PreparedStatement pst, StringBuilder errbuf);
}
