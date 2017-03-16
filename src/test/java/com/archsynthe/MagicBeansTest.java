package com.archsynthe;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * These tests rely on the {@code MB_HOME} environment variable being set to the {@code src/main} directory under
 * the project home.
 *
 * @author James Adams (jamesa@slalom.com)
 * @version ${VERSION}
 * @since ${VERSION}
 */
public class MagicBeansTest {

    @Test
    public void testKeyGen() {

        // Verify MB_HOME environment variable is set
        String mbHome = System.getenv("MB_HOME");
        assertNotNull(mbHome);
        assertNotEquals("",mbHome);

        // Create mock configuration file
        MagicBeansConfig config = new MagicBeansConfig();
        config.setHomePath(mbHome);
        config.setDevopsUserName("us-east-2-magicbeans-devops-user");
        config.setDevopsPrivateKeyfilePath(config.getHomePath() + "/keys/" + config.getDevopsUserName());
        config.setDevopsPublicKeyfilePath(config.getDevopsPrivateKeyfilePath() + ".pub");

        // Generate keypair
        String keyFingerprint = MagicBeans.createSSHKey(config);

        // Validate keypair creation
        assertNotNull(keyFingerprint);
        assertEquals(47, keyFingerprint.length());
        assertTrue(Files.exists(Paths.get(config.getDevopsPrivateKeyfilePath())));
        assertTrue(Files.exists(Paths.get(config.getDevopsPublicKeyfilePath())));

    }

}
