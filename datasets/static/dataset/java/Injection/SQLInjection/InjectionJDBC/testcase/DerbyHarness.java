<filename>training_session/java_build/SPECjvm2008/src/spec/benchmarks/derby/DerbyHarness.java<fim_prefix>

        /*
         * Copyright (c) 2008 Standard Performance Evaluation Corporation (SPEC)
         *               All rights reserved.
         *
         * This source code is provided as is, without any express or implied warranty.
         */
        package spec.benchmarks.derby;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import spec.harness.Context;
import spec.harness.StopBenchmarkException;

public class DerbyHarness {

    class Client {

        byte[] buffer = new byte[Utils.CALL_SPEC];
        BigDecimal[] accResults = Utils.initResultsArray(null);
        int[] spec = new int[4];
        private Connection connection;
        private int shift;
        BigDecimal[] results = Utils.initResultsArray(null);
        BigDecimal allCalls = BigDecimal.ZERO;
        int counter = 0;
        StringBuilder sb = new StringBuilder();
        BigDecimal min = new BigDecimal(Long.MAX_VALUE);
        BigDecimal max = BigDecimal.ZERO;
        byte[] callsSpec;

        public Client(int shift) {
            connection = getNestedConnection(databaseIndex);
            this.shift = shift;
        }

        public void run() {
            handleAccounts();
            connectClose(connection);
            connection = null;
        }

        public String resultsToString() {
            sb.setLength(0);
            sb.append("sumB: " + results[0]);
            sb.append("\nsumD: " + results[1]);
            sb.append("\nsumT: " + results[2]);
            sb.append("\nmin call time: " + min);
            sb.append("\nmax call time: " + max);
            sb.append("\nmean call time: " + allCalls.divide(BigDecimal.valueOf(counter), MathContext.DECIMAL64));
            return sb.toString();
        }

        private void handleAccounts() {
            if (connection == null) {
                Context.getOut().println("ERROR: connection == null");
                return;
            }
            try {

                PreparedStatement[] pstmt1 = new PreparedStatement[tableNumber];
                PreparedStatement pstmt2 = null;
                if (prep) {
                    for (int i = 0; i < tableNumber; i++) {
                        pstmt1[i] = connection.prepareStatement(Utils.getPreparedSelectQuery(i));
                    }
                    pstmt2 = connection.prepareStatement(Utils.UPDATE_ACCOUNTS_TABLE);

                    for (int i = shift; i < accountsNumber; i += Main.THREADSPERDB/*clientsNumber*/) {
                        accResults = Utils.initResultsArray(accResults);
                        for (int j = 0; j < tableNumber; j++) {
                            int ind = (shift + j) % tableNumber;
                            pstmt1[ind].setInt(1, i);
                            handleResultSet(pstmt1[ind].executeQuery());
                            pstmt1[ind].clearWarnings();
                        }
                        pstmt2.setBigDecimal(1, accResults[0]);
                        pstmt2.setBigDecimal(2, accResults[1]);
                        pstmt2.setBigDecimal(3, accResults[2]);
                        pstmt2.setInt(4, i);
                        pstmt2.executeUpdate();
                        pstmt2.clearWarnings();
                        results = Utils.add(results, accResults);
                    }
                } else { //not prepared statements
                    Statement stmt = null;
                    for (int i = shift; i < accountsNumber; i += clientsNumber) {
                        accResults = Utils.initResultsArray(accResults);
                        for (int j = 0; j < tableNumber; j++) {
                            int ind = (shift + j) % tableNumber;
                            stmt = connection.createStatement();
                            handleResultSet(stmt.executeQuery(Utils.getSelectQuery(ind, i)));
                            stmt.clearWarnings();
                        }
                        results = Utils.add(results, accResults);
                        this.results = Utils.add(this.results, results);
                        stmt.execute(Utils.getUpdateAccountsQuery(results, i));
                        stmt.clearWarnings();
                        stmt.close();
                    }
                }
                if (trans) {
                    connection.commit();
                }
            } catch (Exception e) {
                e.printStackTrace(Context.getOut());

                if (trans) {
                    try {
                        connection.rollback();
                    } catch (SQLException e1) {
                        e1.printStackTrace(Context.getOut());
                    }
                }
            }
        }

        private void handleResultSet(ResultSet RS) throws SQLException, IOException {
            while (RS.next()) {
                callsSpec = RS.getBytes(scale + 3);
                for (int i = 0; i < scale; i++) {
                    doComputing(new BigDecimal(RS.getString(3 + i), MathContext.DECIMAL64),
                            i, callsSpec);
                }
            }
            RS.close();
        }

        private int getTimeIndex(long time) {
            int res1 = 0;
            long tmp = time;
            for (int i = 0; i < 20; i++) {
                res1 += tmp % 10;
                tmp = tmp / 10;
            }
            return (int) (res1 % 15);
        }

        private BigDecimal getRates(boolean needBaseRate, Object key) {
            return (BigDecimal) (needBaseRate ? Utils.BASERATES
                    : Utils.DISRATES).get(key);
        }

        private void updateStatistic(BigDecimal n) {
            if (n.compareTo(min) < 0 && n.longValue() != 0) {
                min = n;
            } else if (n.compareTo(max) > 0) {
                max = n;
            }
            counter++;
            allCalls = allCalls.add(n);
        }

        private void doComputing(BigDecimal n,
                                 int shift,
                                 byte[] callSpec) throws IOException {
            updateStatistic(n);
            int[] info = Utils.bytesToInts(callSpec, shift * Utils.INFO_LENGTH, spec);
            int time = getTimeIndex(Utils.bytesToLong(callSpec, 16 + shift * Utils.INFO_LENGTH));
            boolean calltype = (info[2] & 0x01) == 0;
            BigDecimal RATE = getRates(calltype, Utils.keys[info[0]][info[1]][time]);
            BigDecimal BASETAX = Utils.BASETAXES[info[3]];
            BigDecimal DISTAX = Utils.DISTAXES[info[3]];
            BigDecimal p = RATE.multiply(n, MathContext.DECIMAL64);
            BigDecimal b = p.divide(BASETAX, 2, BigDecimal.ROUND_DOWN);
            accResults[0] = accResults[0].add(b);
            BigDecimal t = p.add(b);
            BigDecimal d = BigDecimal.ZERO;
            if (!calltype) {
                d = p.divide(DISTAX, MathContext.DECIMAL64);
                d = d.setScale(2, BigDecimal.ROUND_DOWN);
                t = t.add(d);
                accResults[1] = accResults[1].add(d);
            }

            t = t.setScale(2, BigDecimal.ROUND_DOWN);
            accResults[2] = accResults[2].add(t);
        }
    }
    static int accountsPerThread;
    static int limitPerThread;
    static boolean prep;
    static boolean trans;
    static boolean readOnly;
    static int scale = 5;
    public static int tableNumber;
    public static int DATABASES_NUM;
    public int clientsNumber;
    public int databaseIndex;
    public int accountsNumber;

    static public void setDerbyProperties() {
        System.setProperty("derby.storage.initialPages", "1000");
        System.setProperty("derby.storage.pageSize", "32768");
        System.setProperty("derby.storage.pageCacheSize",
                "" + 4000 * Main.DATABASES_NUM);
    }

    static public void initDatabases() {
        setDerbyProperties();
        DATABASES_NUM = Main.DATABASES_NUM;
        rmDir();
        String DriverName = "org.apache.derby.jdbc.EmbeddedDriver";
        prep = true;
        trans = true;
        readOnly = true;
        accountsPerThread = 250;
        limitPerThread = 50000;
        tableNumber = 2;
        scale = 5;
        Utils.print("Databases: " + DATABASES_NUM);
        Utils.print("MAX CLIENTS per DB: " + Main.MAX_CLIENTS_NUMBER_PER_DB);
        Utils.print("HWT_FACTOR: " + Main.HWTFACTOR);
        Utils.print("accountsPerThread: " + accountsPerThread);
        Utils.print("limitPerThread: " + limitPerThread);
        Utils.print("tablesNumber: " + tableNumber);
        Utils.print("scale: " + scale);
        Utils.print("\ndatabaseIndex\tclients");
        for (int i = 0; i < DATABASES_NUM; i++) {
            Utils.print((i + 1) + "\t\t" + Main.clientsNumber[i]);
        }

        try {
            Class.forName(DriverName);
            createDatabases();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void shutdownDerbySystem() {
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (Exception e) {
        }
    }

    public static void main(int btid, int databaseIndex,
            int clientsNumber, int shift) throws Exception {
        DerbyHarness Me = new DerbyHarness(btid, databaseIndex, Main.THREADSPERDB, shift);
    }

    static void rmDir() {
        File file = new File("derby_dir");
        if (!file.exists()) {
            return;
        } else {
            removeFile(file);
        }
        file = new File("derby.log");
        file.delete();
    }

    static void removeFile(File file) {
        if (!file.isDirectory()) {
            file.delete();
        } else {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                removeFile(files[i]);
            }
            file.delete();
        }
    }

    public DerbyHarness(int btid, int databaseIndex,
            int clientsNumber, int shift) throws Exception {
        accountsNumber = clientsNumber * accountsPerThread;
        Utils.print("DerbyHarness(version 108.2)databaseIndex =  " + databaseIndex + ", clientsNumber = " + clientsNumber + ", accounts = " + accountsNumber + ", currentClientNumber = " + btid + ", shift = " + shift);
        this.clientsNumber = clientsNumber;
        this.databaseIndex = databaseIndex;
        try {
            Client client = new Client(shift);
            client.run();
            Context.getOut().println(client.resultsToString());
        } catch (Exception ex) {
            ex.printStackTrace(Context.getOut());
        }
    }

    static void dropTable(Connection Conn, String name) throws SQLException {
        try {
            Statement stmt = Conn.createStatement();
            stmt.execute("DROP TABLE " + name);
            stmt.clearWarnings();
            if (trans) {
                Conn.commit();
            }
            stmt.close();
        } catch (SQLException e) {
        }
    }

    static void dropTables(Connection connection)
            throws SQLException {
        dropTable(connection, "accounts");
        for (int i = 0; i < tableNumber; i++) {
            dropTable(connection, "durations" + i);
        }

    }

    static void createAccountsTable(Connection connection) throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.execute(Utils.CREATE_ACCOUNTS_TABLE);
        stmt.clearWarnings();
        if (trans) {
            connection.commit();
        }
        stmt.close();
    }

    static void createDurationsTable(String name, String ind,
            Connection connection) throws SQLException {
        Statement stmt = connection.createStatement();
        <fim_suffix>
        stmt.execute(Utils.getCreateDurationsTableQuery(name, scale));
        stmt.clearWarnings();
        stmt.execute("CREATE INDEX " + ind + " ON " + name + " (Aid)");
        stmt.clearWarnings();

        if (trans) {
            connection.commit();
        }
        stmt.close();
    }

    static void createTables(Connection connection) throws SQLException {
        createAccountsTable(connection);
        for (int i = 0; i < tableNumber; i++) {
            createDurationsTable("DURATIONS" + i, "IND" + i, connection);
        }
    }

    static void fillAccountsTable(Connection connection, int accounts) throws SQLException {
        Statement stmt = connection.createStatement();
        PreparedStatement pstmt = null;

        try {
            String query = "INSERT INTO ACCOUNTS(Aid,sumB,sumD,sumT) VALUES (?,0,0,0)";
            pstmt = connection.prepareStatement(query);
        } catch (SQLException e) {
            prep = false;
        }

        for (int i = 0; i < accounts; i++) {
            pstmt.setInt(1, i);
            pstmt.executeUpdate();
            pstmt.clearWarnings();
        }
        pstmt.close();

        if (trans) {
            connection.commit();
        }
        stmt.close();
    }

    void resetAccountsTable(Connection connection) throws SQLException {
        PreparedStatement pstmt = null;
        try {
            String query = "UPDATE accounts SET sumB = 0, sumD = 0, sumT = 0";
            pstmt = connection.prepareStatement(query);
        } catch (SQLException e) {
            prep = false;
        }
        pstmt.executeUpdate();
        pstmt.clearWarnings();
        pstmt.close();

        if (trans) {
            connection.commit();
        }

    }

    static byte[] getSpec(byte[] all, byte[] info) {
        for (int i = 0; i < scale; i++) {
            System.arraycopy(all, i * Utils.INFO_LENGTH, info,
                    i * Utils.INFO_LENGTH, Utils.INFO_LENGTH);
        }
        return info;
    }

    static void fillDurationsTable(int limit, int accounts, int databaseIndex)
            throws SQLException, IOException {
        DerbyHarness.tableNumber = 2;
        DataReader dataReader = new DataReader(limit, tableNumber);
        Thread[] threads = new Thread[Utils.INIT_THREADS_NUMBED];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new InitThread(databaseIndex, dataReader, accounts);
            threads[i].start();
        }

        for (int i = 0; i < threads.length; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new StopBenchmarkException("Cannot initialize database");
            }
        }
        Connection connection = getNestedConnection(databaseIndex);
        PreparedStatement[] pstmt = new PreparedStatement[tableNumber];
        for (int i = 0; i < tableNumber; i++) {
            pstmt[i] = connection.prepareStatement(Utils.getInsertIntoDurationQuery(scale, i));
        }
        dataReader.close();
    }

    static void fillTables(Connection connection, int limit, int accounts, int databaseIndex)
            throws SQLException, IOException {
        fillAccountsTable(connection, accounts);
        fillDurationsTable(limit, accounts, databaseIndex);
    }

    public static void createDatabases() throws Exception {
        Utils.print("Creating database N 1" + " ...");
        int limit = Main.THREADSPERDB * limitPerThread;
        int accounts = Main.THREADSPERDB * accountsPerThread;
        Utils.print("\trecords in duration table: " + limit + " (" + scale + " call's info per record)");
        Utils.print("\taccounts: " + accounts);
        Utils.print("\t\t(preparing for " + Main.THREADSPERDB + " clients)");
        createBaseDatabase(limit, accounts, Main.THREADSPERDB);
        if (Utils.MULTI_THREAD_RESTORING) {
            Thread[] threads = new Thread[DATABASES_NUM - 1];
            for (int i = 2; i < DATABASES_NUM + 1; i++) {
                threads[i - 2] = new Thread(Utils.URL + i) {

                    @Override
                    public void run() {
                        doRestoring(getName());
                    }
                };
                threads[i - 2].start();
            }

            for (int i = 2; i < DATABASES_NUM + 1; i++) {
                threads[i - 2].join();
            }

        } else {
            for (int i = 2; i < DATABASES_NUM + 1; i++) {
                doRestoring(Utils.URL + i);
            }
        }
    }

    private static void doRestoring(String databaseName) {
        try {
            Utils.print("Creating database N " + databaseName + " ...");
            Connection conn = DriverManager.getConnection(databaseName + ";createFrom=" + Utils.BACKUP_DIR);
            connectClose(conn);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new StopBenchmarkException("Cannot create " + databaseName);
        }
    }

    public static void createBaseDatabase(int limit, int accounts, int clients)
            throws Exception {
        Connection connection = getStartConnection(1);
        dropTables(connection);
        createTables(connection);
        fillTables(connection, limit, accounts, 1);
        CallableStatement cs = connection.prepareCall("CALL SYSCS_UTIL.SYSCS_BACKUP_DATABASE(?)");
        cs.setString(1, Utils.BACKUP_BASE_DIR);
        cs.execute();
        cs.close();
        connectClose(connection);
    }

    public static Connection getNestedConnection(int databaseIndex) {
        try {
            Connection conn = DriverManager.getConnection(Utils.URL + databaseIndex);
            try {
                if (trans) {
                    conn.setAutoCommit(false);
                }
            } catch (SQLException e) {
                e.printStackTrace(Context.getOut());
            }
            return conn;
        } catch (Exception e) {
            e.printStackTrace(Context.getOut());
        }

        return null;
    }

    public static Connection getStartConnection(int databaseIndex) {
        try {
            Connection conn = DriverManager.getConnection(Utils.URL + databaseIndex + ";create=true");
            try {
                if (trans) {
                    conn.setAutoCommit(false);
                }
            } catch (SQLException e) {
                e.printStackTrace(Context.getOut());
            }
            return conn;
        } catch (Exception e) {
            e.printStackTrace(Context.getOut());
        }

        return null;
    }

    public static void connectClose(Connection c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (Exception e) {
            e.printStackTrace(Context.getOut());
        }
    }
}

<fim_middle>