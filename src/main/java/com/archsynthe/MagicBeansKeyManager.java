package com.archsynthe;

import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.model.*;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

/**
 * @author James Adams (jamesa@slalom.com)
 * @version ${VERSION}
 * @since ${VERSION}
 */
public class MagicBeansKeyManager {

    static UploadSSHPublicKeyResult uploadSSHKeyToUser(MagicBeansConfig config, AmazonIdentityManagement iam) {

        UploadSSHPublicKeyRequest uploadRequest = new UploadSSHPublicKeyRequest();
        UploadSSHPublicKeyResult uploadResult = null;
        try {
            uploadRequest.setUserName(config.getDevopsUserName());
            String publicKey = new String(Files.readAllBytes(Paths.get(config.getDevopsPublicKeyfilePath())));
            uploadRequest.setSSHPublicKeyBody(publicKey);
            uploadResult = iam.uploadSSHPublicKey(uploadRequest);
            config.setDevopsSSHPublicKeyId(uploadResult.getSSHPublicKey().getSSHPublicKeyId());
            config.setDevopsRepositoryCloneUrl("ssh://" + config.getDevopsSSHPublicKeyId() + "@git-codecommit." + config.getRegion()+ ".amazonaws.com/v1/repos/" + config.getDevopsRepositoryName());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return uploadResult;

    }

    static void removeSSHKeysFromUser(MagicBeansConfig config, AmazonIdentityManagement iam) {

        ListSSHPublicKeysRequest listRequest = new ListSSHPublicKeysRequest();
        listRequest.setUserName(config.getDevopsUserName());
        ListSSHPublicKeysResult listResult = iam.listSSHPublicKeys(listRequest);
        for (SSHPublicKeyMetadata metadata : listResult.getSSHPublicKeys()) {
            String publicKeyId = metadata.getSSHPublicKeyId();
            DeleteSSHPublicKeyRequest deleteRequest = new DeleteSSHPublicKeyRequest();
            deleteRequest.setUserName(config.getDevopsUserName());
            deleteRequest.setSSHPublicKeyId(publicKeyId);
            iam.deleteSSHPublicKey(deleteRequest);
        }

    }

    static String createSSHKey(MagicBeansConfig config) {
        JSch jsch=new JSch();

        String keyFileName = config.getHomePath() + "/keys/" + config.getDevopsUserName();
        String keyFingerprint = null;
        KeyPair keyPair= null;
        try {
            keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA, 2048);
            keyPair.writePrivateKey(keyFileName);
            Set<PosixFilePermission> perms = new HashSet<>(3);
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(Paths.get(keyFileName),perms);
            keyPair.writePublicKey(keyFileName + ".pub", config.getDevopsUserName());
            keyFingerprint = keyPair.getFingerPrint();
            keyPair.dispose();
        } catch (JSchException | IOException e) {
            e.printStackTrace();
        }

        return keyFingerprint;

    }


}
