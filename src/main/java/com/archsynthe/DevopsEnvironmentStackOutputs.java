package com.archsynthe;

/**
 * @author James Adams (jamesa@slalom.com)
 * @version ${VERSION}
 * @since ${VERSION}
 */
@lombok.Data
public class DevopsEnvironmentStackOutputs {

    private String sshPublicKeyId;
    private String userAccessKeyId;
    private String userSecretAccessKey;
    private String repositoryCloneUrl;
    private String s3BucketUrl;

}
