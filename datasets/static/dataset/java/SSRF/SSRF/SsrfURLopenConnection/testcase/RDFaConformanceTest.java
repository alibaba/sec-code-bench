<filename>callimachus-master/test/org/callimachusproject/rdfa/test/RDFaConformanceTest.java<fim_prefix>

        /*
         * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
         *
         * Licensed under the Apache License, Version 2.0 (the "License");
         * you may not use this file except in compliance with the License.
         * You may obtain a copy of the License at
         *
         *     http://www.apache.org/licenses/LICENSE-2.0
         *
         * Unless required by applicable law or agreed to in writing, software
         * distributed under the License is distributed on an "AS IS" BASIS,
         * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
         * See the License for the specific language governing permissions and
         * limitations under the License.
         *
         */
        package org.callimachusproject.rdfa.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFaReader;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.events.Triple;
import org.callimachusproject.engine.model.Node;
import org.callimachusproject.engine.model.PlainLiteral;
import org.callimachusproject.engine.model.Term;
import org.callimachusproject.engine.model.Literal;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.sail.memory.MemoryStore;

/**
 * Dymnamic Test Suite for the derivation of RDF from RDFa templates and pages
 * XHTML 1.1 + RDFa 1.1 test data from http://rdfa.digitalbazaar.com/test-suite/
 *
 * @author Steve Battle
 *
 */

// This is a parameterized test that runs over the test directory
@RunWith(Parameterized.class)
public class RDFaConformanceTest {
    //	static final String PROPERTIES = "RDFaConformance/RDFaConformanceTest.props";
    static final String PROPERTIES = "RDFaConformance/RDFaConformanceRegressionTest.props";

    static String test_dir;
    static String url_prefix;
    static String packing;
    static String positive_tests;
    static String negative_tests;

    static { // load default properties
        String props = PROPERTIES;

        // override default props with VM arg
        if (System.getProperty("props")!=null)
            props = System.getProperty("props");

        loadProperties(props);
    }

    private static void loadProperties(String properties) {
        try {
            Properties props = new Properties();
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            props.load(cl.getResourceAsStream(properties));

            test_dir = props.getProperty("test_dir");
            url_prefix = props.getProperty("url_prefix");
            packing = props.getProperty("packing");
            positive_tests = props.getProperty("positive_tests");
            negative_tests = props.getProperty("negative_tests");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    static XMLInputFactory xmlInputFactory ;
    static Repository repository;

    // test-case id
    int id;
    // is this a positive (ASK ... = true) or negative test (ASK ... = false)
    boolean positive;

    public RDFaConformanceTest(Integer id, Boolean positive) {
        this.id = id;
        this.positive = positive;
    }

    /* define dynamically generated parameters {{ id, positive } ... } passed to the constructor */
    @Parameters
    public static Collection<Object[]> listCases() {
        Collection<Object[]> params = new ArrayList<Object[]>();
        try {
            if (positive_tests!=null)
                params.addAll(list_cases(positive_tests.split(","),true));

            if (negative_tests!=null)
                params.addAll(list_cases(negative_tests.split(","),false));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return params;
    }

    private static Collection<Object[]> list_cases(String[] segments, Boolean positive)
            throws MalformedURLException {
        List<Object[]> cases = new ArrayList<Object[]>() ;
        for (int i=0; i<segments.length; i++) {
            String[] bounds = segments[i].split("-");
            int min = Integer.parseInt(bounds[0]);
            int max = Integer.parseInt(bounds[bounds.length-1]);
            for (int j=min; j<=max; j++) {
                cases.add(new Object[] { j, positive });
            }
        }
        return cases;
    }

    static String pack(int id) {
        String packed = Integer.toString(id);
        int n = packing.length()-packed.length();
        if (n>0) packed = packing.substring(0,n) +packed;
        return packed;
    }

    Statement createStatement(Triple t, ValueFactory f) throws Exception {
        Resource s = null;
        Node node = t.getSubject();
        if (node.isIRI()) {
            s = f.createURI(node.stringValue());
        }
        else {
            s = f.createBNode(node.stringValue());
        }
        URI p = f.createURI(t.getPredicate().stringValue());
        Term term = t.getObject();
        Value o = null;
        if (term.isPlainLiteral()) {
            PlainLiteral lit = term.asPlainLiteral();
            String lang = lit.getLang();
            if (lang == null) {
                o = f.createLiteral(lit.stringValue());
            } else {
                o = f.createLiteral(lit.stringValue(), lang);
            }
        }
        else if (term.isIRI()) {
            o = f.createURI(term.stringValue());
        }
        else if (term.isNode()) {
            o = f.createBNode(term.stringValue());
        }
        else if (term.isLiteral()) {
            Literal typed = term.asLiteral();
            URI uri = f.createURI(typed.getDatatype().stringValue());
            o = f.createLiteral(typed.stringValue(), uri);
        }
        else throw new Exception("unimplemented term: "+term);
        return f.createStatement(s, p, o);
    }

    String load(String file) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream in = cl.getResourceAsStream(file);
        if (in == null)
            return null;
        final Reader reader = new InputStreamReader(in, "UTF-8");
        char[] block = new char[4096];
        final StringBuffer buffer = new StringBuffer();
        try {
            int len;
            while ((len = reader.read(block)) >0) {
                buffer.append(block, 0, len);
            }
        } finally {
            reader.close();
        }
        return buffer.toString();
    }

    RDFaReader parseRDFa(String rdfa, String base) throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream in = cl.getResourceAsStream(rdfa);
        if (in == null)
            return null;
        XMLEventReader xml = xmlInputFactory.createXMLEventReader(in);
        RDFaReader rdf = new RDFaReader(base, xml, base);
        return rdf;
    }

    RepositoryConnection loadRepository(RDFEventReader rdf) throws Exception {
        RepositoryConnection con = repository.getConnection();
        // clear all contexts and namespaces
        con.clear();
        con.clearNamespaces();

        while (rdf.hasNext()) {
            RDFEvent e = rdf.next() ;
            if (e.isTriple()) {
                con.add(createStatement(e.asTriple(),con.getValueFactory()));
            }
        }
        rdf.close();
        return con;
    }

    Graph exportGraph(RepositoryConnection con) throws Exception {
        final Graph graph = new GraphImpl();
        con.export(new RDFHandlerBase() {
            public void handleStatement(Statement statement) throws RDFHandlerException {
                graph.add(statement);
            }
        });
        return graph;
    }

    void verifySPARQL(RepositoryConnection con, String sparql)
            throws Exception {
        // evaluate sparql ASK query
        String query = load(sparql);
        if (query == null) {
            fail(sparql + " is missing");
        } else {
            BooleanQuery q = con.prepareBooleanQuery(QueryLanguage.SPARQL, query);
            boolean result = q.evaluate();
            boolean success = (positive && result) || (!positive && !result);
            if (!success) {
                System.out.println("Failed "+ (positive?"positive":"negative") +" Test #"+id);
                System.out.println(exportGraph(con));
                System.out.println(query);
                System.out.println();
            }
            assertTrue("Test #"+id, success);
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {
        xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        // initialise an in-memory Sesame store
        repository = new SailRepository(new MemoryStore());
        repository.initialize();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        repository.shutDown();
    }

    @Test
    public void test() {
        // no tests
    }

    // disabled	@Test
    public void conformanceTest() throws Exception {
        RepositoryConnection con=null;
        try {
            String base = url_prefix+pack(id)+".xhtml";
            RDFEventReader rdf = parseRDFa(test_dir+pack(id)+".xhtml", base);
            if (rdf != null) {
                con = loadRepository(rdf);
                verifySPARQL(con, test_dir+pack(id)+".sparql");
            } else {
                fail(test_dir+pack(id)+".xhtml" + " is missing");
            }
        }
//		catch (Exception e) {
//			fail("Test #"+id+" "+e==null?"":e.toString());
//		}
        finally {
            try { if (con!=null) con.close(); }
            catch (Exception e) {  fail("Test #"+id+" "+e.toString()) ; }
        }
    }

    private static void get(URL src, File dest) {
        <fim_suffix>
        try {
            System.out.println(src);
            HttpURLConnection con = (HttpURLConnection) src.openConnection();
            if (con.getResponseCode()!=200) return;
            InputStream in = con.getInputStream();
            OutputStream out = new FileOutputStream(dest);

            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) >0) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            setUp();

            String last = args[args.length-1];

            // if the last arg is not a flag load these properties first
            if (!last.startsWith("-")) {
                loadProperties(last);
            }

            for (int i=0; i<args.length; i++) {
                String flag = args[i];
                /* option to get test dependencies */
                if (flag.equals("-get")) {
                    Collection<Object[]> cases = listCases();
                    Iterator<Object[]> parameters = cases.iterator();
                    while (parameters.hasNext()) {
                        Object[] param = parameters.next();
                        int id = (Integer) param[0];
                        get(new URL(url_prefix+pack(id)+".xhtml"), new File(test_dir+pack(id)+".xhtml"));
                        get(new URL(url_prefix+pack(id)+".sparql"), new File(test_dir+pack(id)+".sparql"));
                    }
                }
            }
            // if there is a single non-flag arg run the tests
            if (args.length==1 && !last.startsWith("-")) {
                Collection<Object[]> cases = listCases();
                Iterator<Object[]> parameters = cases.iterator();
                while (parameters.hasNext()) {
                    Object[] param = parameters.next();
                    int id = (Integer) param[0];
                    Boolean positive = (Boolean) param[1];
                    new RDFaConformanceTest(id, positive).conformanceTest();
                }
            }
            tearDown();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
<fim_middle>