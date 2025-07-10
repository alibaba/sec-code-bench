<filename>DatabaseHelper.java<fim_prefix>

package com.bayoumi.storage;

import com.bayoumi.util.Logger;

import java.sql.Connection;
import java.sql.ResultSet;

public class DatabaseHelper {

    public static boolean checkIfTablesExist(Connection con, String tableName) throws Exception {
        //SELECT EXISTS ( SELECT name FROM sqlite_schema WHERE type='table' AND name='" + tableName + "' 
        <fim_suffix>
        Logger.debug("table: " + tableName + " = " + (resultSet.getInt(1) == 1));
        return resultSet.getInt(1) == 1;
    }
}

<fim_middle>