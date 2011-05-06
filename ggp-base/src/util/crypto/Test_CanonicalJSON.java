package util.crypto;

import util.crypto.CanonicalJSON.CanonicalizationStrategy;
import junit.framework.TestCase;

/**
 * Unit tests for the HttpReader/HttpWriter pair, which are the way that
 * game players and game servers communicate. Please update these tests
 * as needed when bugs are discovered, to prevent regressions. 
 * 
 * @author Sam
 */
public class Test_CanonicalJSON extends TestCase {
    public void testSimpleCanonicalization() {
        CanonicalizationStrategy theStrategy = CanonicalizationStrategy.SIMPLE;
        
        String a = CanonicalJSON.getCanonicalForm("{1:2,2:3,3:{2:5,c:4,7:9,a:6}}", theStrategy);
        assertEquals(a, CanonicalJSON.getCanonicalForm("{2:3,3:{c:4,7:9,2:5,a:6},1:2}", theStrategy));
        assertEquals(a, CanonicalJSON.getCanonicalForm("{3:{c:4,7:9,2:5,a:6},2:3,1:2}", theStrategy));
        assertEquals(a, CanonicalJSON.getCanonicalForm("{3:{7:9,c:4,2:5,a:6},1:2,2:3}", theStrategy));
        assertEquals(a, CanonicalJSON.getCanonicalForm("{2:3,3:{c:4,7:9,a:6,2:5},1:2}", theStrategy));
        
        String b = CanonicalJSON.getCanonicalForm("{'abc':3, \"def\":4, ghi:5}", theStrategy);
        assertEquals(b, CanonicalJSON.getCanonicalForm("{'def':4, abc:3, \"ghi\":5}", theStrategy));
        assertEquals(b, CanonicalJSON.getCanonicalForm("{\"def\":4, ghi:5, 'abc':3}", theStrategy));
        assertEquals(b, CanonicalJSON.getCanonicalForm("{abc:3, def:4, ghi:5}", theStrategy));
        assertEquals(b, CanonicalJSON.getCanonicalForm("{'abc':3, 'def':4, 'ghi':5}", theStrategy));
        assertEquals(b, CanonicalJSON.getCanonicalForm("{\"abc\":3, \"def\":4, \"ghi\":5}", theStrategy));
    }
}