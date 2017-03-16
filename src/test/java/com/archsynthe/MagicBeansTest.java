package com.archsynthe;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author James Adams (jamesa@slalom.com)
 * @version ${VERSION}
 * @since ${VERSION}
 */
public class MagicBeansTest {

    @Test
    public void testKeyGen() {
        MagicBeansConfig config = new MagicBeansConfig();
        config.setHomePath(System.getenv("MB_HOME"));
        config.setDevopsUserName("us-east-2-magicbeans-devops-user");
        config.setDevopsPrivateKeyfilePath(config.getHomePath() + "/keys/" + config.getDevopsUserName());
        config.setDevopsPublicKeyfilePath(config.getDevopsPrivateKeyfilePath() + ".pub");
        String keyFingerprint = MagicBeans.createSSHKey(config);
        assertNotNull(keyFingerprint);
        assertEquals(47, keyFingerprint.length());
        assertTrue(Files.exists(Paths.get(config.getDevopsPrivateKeyfilePath())));
        assertTrue(Files.exists(Paths.get(config.getDevopsPublicKeyfilePath())));
    }

}
