/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import org.geoserver.security.RESTfulDefinitionSource;
import org.geoserver.security.impl.RESTAccessRuleDAO;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.access.ConfigAttribute;

@TestSetup(run=TestSetupFrequency.REPEAT)
public class RESTSecurityRulesTest extends GeoServerSystemTestSupport {

    RESTfulDefinitionSource defSource = null;
    RESTAccessRuleDAO dao = null;
    
    @Before
    public void setUpInternal() throws Exception {
        defSource = (RESTfulDefinitionSource) applicationContext.getBean("restFilterDefinitionMap");
        dao = (RESTAccessRuleDAO) applicationContext.getBean("restRulesDao");
    }

    @Test
    public void testDefault() throws Exception {
        Collection<ConfigAttribute> atts = defSource.lookupAttributes("/foo", "GET");
        assertEquals(1, atts.size());
        assertEquals("ADMIN", atts.iterator().next().getAttribute());
    }

    @Test
    public void testException() throws Exception {
        FileWriter fw = writer();
        fw.write("/foo;GET=IS_AUTHENTICATED_ANONYMOUSLY\n");
        fw.write("/**;GET=ROLE_ADMINISTRATOR\n");
        fw.flush();
        fw.close();

        //seems to be a delay of updating the timestamp on file or something, causing written 
        // fules to not be reloaded, so just do it manually
        dao.reload();
        defSource.reload();

        Collection<ConfigAttribute> atts = defSource.lookupAttributes("/foo", "GET");
        assertEquals(1, atts.size());
        assertEquals("IS_AUTHENTICATED_ANONYMOUSLY", atts.iterator().next().getAttribute());
    }

    @Test
    public void testExceptionAfter() throws Exception {
        FileWriter fw = writer();
        fw.write("/**;GET=ROLE_ADMINISTRATOR\n");
        fw.write("/foo;GET=IS_AUTHENTICATED_ANONYMOUSLY\n");
        fw.flush();
        fw.close();

        dao.reload();
        defSource.reload();

        Collection<ConfigAttribute> atts = defSource.lookupAttributes("/foo", "GET");
        assertEquals(1, atts.size());
        assertEquals("ROLE_ADMINISTRATOR", atts.iterator().next().getAttribute());
    }

    FileWriter writer() throws IOException {
        return new FileWriter(new File(getDataDirectory().findSecurityRoot(), "rest.properties"));
    }
}
