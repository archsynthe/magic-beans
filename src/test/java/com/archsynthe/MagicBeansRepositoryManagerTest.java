package com.archsynthe;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author James Adams (jamesa@slalom.com)
 * @version ${VERSION}
 * @since ${VERSION}
 */
public class MagicBeansRepositoryManagerTest {

    private String mbHome = System.getenv("MB_HOME");
    private MagicBeansConfig config;

    @Before
    public void verifyHomeDirectorySet() {

        // Verify MB_HOME environment variable is set
        assertNotNull(mbHome);
        assertNotEquals("",mbHome);

        // Load configuration file
        config = MagicBeansConfig.load();
        assertNotNull(config);
        assertNotNull(config.getCompany());
        assertNotNull(config.getDevopsBucketName());
        assertNotNull(config.getDevopsEnvironmentTemplatePath());
        assertNotNull(config.getDevopsGroupName());
        assertNotNull(config.getDevopsPolicyName());
        assertNotNull(config.getDevopsPrivateKeyfilePath());
        assertNotNull(config.getDevopsPublicKeyfilePath());
        assertNotNull(config.getDevopsRepositoryDescription());
        assertNotNull(config.getDevopsRepositoryName());
        assertNotNull(config.getDevopsStackName());
        assertNotNull(config.getDevopsUserName());
        assertNotNull(config.getHomePath());
        assertNotNull(config.getProfile());
        assertNotNull(config.getRegion());

    }

    @Test
    public void testCloneRepository() {
        config.setDevopsRepositoryCloneUrl("ssh://APKAITXRLXDYL3CEFUQQ@git-codecommit." + config.getRegion() + ".amazonaws.com/v1/repos/" + config.getDevopsRepositoryName());
        MagicBeansRepositoryManager.cloneRepository(config);
        MagicBeansRepositoryManager.copySeedFiles(config);
    }

}
