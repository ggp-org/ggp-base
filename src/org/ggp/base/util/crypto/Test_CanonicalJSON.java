package org.ggp.base.util.crypto;

import org.ggp.base.util.crypto.CanonicalJSON.CanonicalizationStrategy;

import junit.framework.TestCase;

/**
 * Unit tests for the CanonicalJSON class, which provides a
 * standard way for GGP systems to canonicalize JSON objects.
 * This is an important step in signing JSON objects.
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
        assertEquals(a, "{\"1\":2,\"2\":3,\"3\":{\"2\":5,\"7\":9,\"a\":6,\"c\":4}}");
        
        String b = CanonicalJSON.getCanonicalForm("{'abc':3, \"def\":4, ghi:5}", theStrategy);
        assertEquals(b, CanonicalJSON.getCanonicalForm("{'def':4, abc:3, \"ghi\":5}", theStrategy));
        assertEquals(b, CanonicalJSON.getCanonicalForm("{\"def\":4, ghi:5, 'abc':3}", theStrategy));
        assertEquals(b, CanonicalJSON.getCanonicalForm("{abc:3, def:4, ghi:5}", theStrategy));
        assertEquals(b, CanonicalJSON.getCanonicalForm("{'abc':3, 'def':4, 'ghi':5}", theStrategy));
        assertEquals(b, CanonicalJSON.getCanonicalForm("{\"abc\":3, \"def\":4, \"ghi\":5}", theStrategy));
        assertEquals(b, "{\"abc\":3,\"def\":4,\"ghi\":5}");
    }
}