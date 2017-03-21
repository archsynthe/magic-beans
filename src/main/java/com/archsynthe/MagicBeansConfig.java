package com.archsynthe;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * @author James Adams (jamesa@slalom.com)
 * @version ${VERSION}
 * @since ${VERSION}
 */
@lombok.Data
class MagicBeansConfig {

    private static final String AWS_REGION_CONFIG_PROPERTY = "awsRegion";
    private static final String AWS_PROFILE_CONFIG_PROPERTY = "awsProfile";
    private static final String COMPANY_CONFIG_PROPERTY = "company";
    private static final String SSH_CONFIG_FILE_CONFIG_PROPERTY = "sshConfigFilePath";

    private String region;
    private String profile;
    private String homePath;
    private String company;
    private String sshConfigFilePath;
    private String devopsEnvironmentTemplatePath;
    private String devopsStackName;
    private String devopsPolicyName;
    private String devopsGroupName;
    private String devopsUserName;
    private String devopsRepositoryName;
    private String devopsRepositoryCloneUrl;
    private String devopsRepositoryDescription;
    private String devopsBucketName;
    private String devopsPrivateKeyfilePath;
    private String devopsPublicKeyfilePath;
    private String devopsAccessKeyId;
    private String devopsSecretAccessKey;
    private String devopsSSHPublicKeyId;

    static MagicBeansConfig load() {

        // Load path from environment
        String homePath = System.getenv("MB_HOME");

        MagicBeansConfig config = new MagicBeansConfig();
        config.setHomePath(homePath);
        try (InputStream is = new FileInputStream(homePath + "/conf/config.yaml")) {

            // Load configuration data from classpath resource
            Yaml yaml = new Yaml();
            Map map = (Map) yaml.load(is);

            // Map data loaded from config file to configuration object
            config.setRegion((String) map.get(AWS_REGION_CONFIG_PROPERTY));
            config.setProfile((String) map.get(AWS_PROFILE_CONFIG_PROPERTY));
            config.setCompany((String) map.get(COMPANY_CONFIG_PROPERTY));
            config.setSshConfigFilePath((String) map.get(SSH_CONFIG_FILE_CONFIG_PROPERTY));

            // Map generated names back to config object
            config.setDevopsEnvironmentTemplatePath(config.getHomePath() + "/templates/devops-environment-template.yaml");
            config.setDevopsStackName(config.getRegion() + "-" + config.getCompany() + "-devops-environment");
            config.setDevopsRepositoryName(config.getRegion() + "-" + config.getCompany() + "-cloudformation");
            config.setDevopsRepositoryDescription("CloudFormation Templates: " + config.getCompany());
            config.setDevopsBucketName(config.getRegion() + "-" + config.getCompany() + "-devops-artifacts");
            config.setDevopsPolicyName(config.getRegion() + "-" + config.getCompany() + "-devops-policy");
            config.setDevopsGroupName(config.getRegion() + "-" + config.getCompany() + "-devops-group");
            config.setDevopsUserName(config.getRegion() + "-" + config.getCompany() + "-devops-user");
            config.setDevopsPrivateKeyfilePath(config.getHomePath() + "/keys/" + config.getDevopsUserName());
            config.setDevopsPublicKeyfilePath(config.getDevopsPrivateKeyfilePath() + ".pub");

        } catch (IOException e) {
            e.printStackTrace();
        }

        return config;
    }

}
