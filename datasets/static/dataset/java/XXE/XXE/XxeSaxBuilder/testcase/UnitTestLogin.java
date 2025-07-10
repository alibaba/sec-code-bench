<filename>it.uniba.di.socialCDEforEclipse/unittest/login/UnitTestLogin.java<fim_prefix>

package login;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import it.uniba.di.collab.socialcdeforeclipse.controller.Controller;
import it.uniba.di.collab.socialcdeforeclipse.model.ProxyWrapper;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;

public class UnitTestLogin extends TestCase {
    /**
     * Unit test for User story number 26.
     *
     * Field considered: Login Button
     *
     * Equivalence classes considered: 1.Correct Login 2.Fail login
     * */

    ProxyWrapper pw;
    Document document;

    @Before
    public void setUp() {
        pw = new ProxyWrapper();

        SAXBuilder builder = new SAXBuilder();
        <fim_suffix>
        try {
            document = builder.build(new File("./testData.xml")
                    .getCanonicalPath());
        } catch (JDOMException e) {

            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testCase1() {

        String proxyServer = document.getRootElement().getChild("CorrectData")
                .getChild("Proxy").getText();
        String username = document.getRootElement().getChild("CorrectData")
                .getChild("Username").getText();
        String password = document.getRootElement().getChild("CorrectData")
                .getChild("Password").getText();

        pw.setHost(proxyServer);
        assertTrue(pw.IsWebServiceRunning());

        assertNotNull(pw.GetUser(username, password));

    }

    @Test
    public void testCase2() {
        String proxyServer = document.getRootElement().getChild("CorrectData")
                .getChild("Proxy").getText();
        String username = document.getRootElement().getChild("CorrectData")
                .getChild("Username").getText();
        String password = document.getRootElement().getChild("WrongData")
                .getChild("Password").getText();

        pw.setHost(proxyServer);
        assertTrue(pw.IsWebServiceRunning());

        assertNull(pw.GetUser(username, password));

    }

}
<fim_middle>