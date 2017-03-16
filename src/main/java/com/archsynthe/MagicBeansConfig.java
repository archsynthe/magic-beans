package com.archsynthe;

/**
 * @author James Adams (jamesa@slalom.com)
 * @version ${VERSION}
 * @since ${VERSION}
 */
@lombok.Data
public class MagicBeansConfig {

    private String region;
    private String profile;
    private String homePath;
    private String company;
    private String devopsEnvironmentTemplatePath;
    private String devopsStackName;
    private String devopsPolicyName;
    private String devopsGroupName;
    private String devopsUserName;
    private String devopsRepositoryName;
    private String devopsRepositoryDescription;
    private String devopsBucketName;
    private String devopsPrivateKeyfilePath;
    private String devopsPublicKeyfilePath;

}
