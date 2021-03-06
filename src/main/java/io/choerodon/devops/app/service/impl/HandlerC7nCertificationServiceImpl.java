package io.choerodon.devops.app.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.models.V1Endpoints;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.choerodon.devops.api.vo.kubernetes.C7nCertification;
import io.choerodon.devops.api.vo.kubernetes.certification.CertificationExistCert;
import io.choerodon.devops.api.vo.kubernetes.certification.CertificationSpec;
import io.choerodon.devops.app.service.*;
import io.choerodon.devops.infra.dto.*;
import io.choerodon.devops.infra.enums.*;
import io.choerodon.devops.infra.exception.GitOpsExplainException;
import io.choerodon.devops.infra.util.GitUtil;
import io.choerodon.devops.infra.util.TypeUtil;


@Service
public class HandlerC7nCertificationServiceImpl implements HandlerObjectFileRelationsService<C7nCertification> {

    private static final String CERTIFICATE = "Certificate";
    private static final String GIT_SUFFIX = "/.git";
    public static final String LETSENCRYPT_PROD = "letsencrypt-prod";
    public static final String LOCALHOST = "localhost";
    public static final String CLUSTER_ISSUER = "ClusterIssuer";

    @Autowired
    private CertificationService certificationService;
    @Autowired
    private DevopsEnvironmentService devopsEnvironmentService;
    @Autowired
    private DevopsEnvFileResourceService devopsEnvFileResourceService;
    @Autowired
    private DevopsEnvCommandService devopsEnvCommandService;
    private Gson gson = new Gson();

    @Override
    public void handlerRelations(Map<String, String> objectPath, List<DevopsEnvFileResourceDTO> beforeSync,

                                 List<C7nCertification> c7nCertifications, List<V1Endpoints> v1Endpoints, Long envId, Long projectId, String path, Long userId) {
        List<C7nCertification> updateC7nCertification = new ArrayList<>();
        List<String> beforeC7nCertification = beforeSync.stream()
                .filter(devopsEnvFileResourceE -> devopsEnvFileResourceE.getResourceType().equals(CERTIFICATE))
                .map(devopsEnvFileResourceE -> {
                    CertificationDTO certificationDTO = certificationService
                            .baseQueryById(devopsEnvFileResourceE.getResourceId());
                    if (certificationDTO == null) {
                        devopsEnvFileResourceService
                                .baseDeleteByEnvIdAndResourceId(envId, devopsEnvFileResourceE.getResourceId(), ObjectType.CERTIFICATE.getType());
                        return null;
                    }
                    return certificationDTO.getName();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<C7nCertification> addC7nCertification = new ArrayList<>();
        c7nCertifications.forEach(certification -> {
            int index = beforeC7nCertification.indexOf(certification.getMetadata().getName());
            if (index != -1) {
                updateC7nCertification.add(certification);
                beforeC7nCertification.remove(index);
            } else {
                addC7nCertification.add(certification);
            }
        });

        updateC7nCertification.forEach(c7nCertification1 ->
                updateC7nCertificationPath(c7nCertification1, envId, objectPath, path));
        beforeC7nCertification
                .forEach(certName -> {
                    CertificationDTO certificationDTO = certificationService.baseQueryByEnvAndName(envId, certName);
                    if (certificationDTO != null) {
                        certificationService.certDeleteByGitOps(certificationDTO.getId());
                        devopsEnvFileResourceService
                                .baseDeleteByEnvIdAndResourceId(envId, certificationDTO.getId(), ObjectType.CERTIFICATE.getType());
                    }

                });


        addC7nCertification.forEach(c7nCertification -> {
            String filePath = "";
            try {
                filePath = objectPath.get(TypeUtil.objToString(c7nCertification.hashCode()));
                DevopsEnvFileResourceDTO devopsEnvFileResourceDTO = new DevopsEnvFileResourceDTO();
                devopsEnvFileResourceDTO.setEnvId(envId);
                devopsEnvFileResourceDTO.setFilePath(filePath);
                devopsEnvFileResourceDTO.setResourceId(
                        createCertificationAndGetId(
                                envId, c7nCertification, c7nCertification.getMetadata().getName(), filePath, path, userId));
                devopsEnvFileResourceDTO.setResourceType(c7nCertification.getKind());
                devopsEnvFileResourceService.baseCreate(devopsEnvFileResourceDTO);
            } catch (Exception e) {
                throw new GitOpsExplainException(e.getMessage(), filePath, e);
            }
        });
    }

    @Override
    public Class<C7nCertification> getTarget() {
        return C7nCertification.class;
    }

    private void updateC7nCertificationPath(C7nCertification c7nCertification,
                                            Long envId, Map<String, String> objectPath, String path) {
        Long certId = checkC7nCertificationChanges(c7nCertification, envId, objectPath, path);

        String kind = c7nCertification.getKind();
        DevopsEnvFileResourceDTO devopsEnvFileResourceDTO = devopsEnvFileResourceService
                .baseQueryByEnvIdAndResourceId(envId, certId, kind);
        devopsEnvFileResourceService.updateOrCreateFileResource(objectPath, envId,
                devopsEnvFileResourceDTO, c7nCertification.hashCode(), certId, kind);

    }

    private Long checkC7nCertificationChanges(C7nCertification c7nCertification, Long envId,
                                              Map<String, String> objectPath, String path) {
        DevopsEnvironmentDTO devopsEnvironmentDTO = devopsEnvironmentService.baseQueryById(envId);
        String certName = c7nCertification.getMetadata().getName();
        CertificationDTO certificationDTO = certificationService.baseQueryByEnvAndName(envId, certName);
        CertificationFileDTO certificationFileDTO = certificationService.baseQueryCertFile(certificationDTO.getId());
        String type;
        String keyContent = null;
        Map<String, String> issuerRef = new HashMap<>();
        String certContent = null;
        if (certificationFileDTO != null) {
            type = CertificationType.UPLOAD.getType();
            keyContent = certificationFileDTO.getKeyFile();
            certContent = certificationFileDTO.getCertFile();
            issuerRef.put("name", LOCALHOST);
            issuerRef.put("kind", CLUSTER_ISSUER);
        } else {
            type = CertificationType.REQUEST.getType();
            issuerRef.put("name", LETSENCRYPT_PROD);
            issuerRef.put("kind", CLUSTER_ISSUER);
        }
        c7nCertification.getSpec().setIssuerRef(issuerRef);


        String filePath = objectPath.get(TypeUtil.objToString(c7nCertification.hashCode()));
        C7nCertification oldC7nCertification = certificationService.getC7nCertification(
                certName, type, gson.fromJson(certificationDTO.getDomains(), new TypeToken<List<String>>() {
                }.getType()), keyContent, certContent, devopsEnvironmentDTO.getCode());
        if (!c7nCertification.equals(oldC7nCertification)) {
            throw new GitOpsExplainException(GitOpsObjectError.CERT_CHANGED.getError(), filePath);
        }
        updateCommandSha(filePath, path, certificationDTO.getCommandId());
        return certificationDTO.getId();
    }

    private Long createCertificationAndGetId(Long envId, C7nCertification c7nCertification, String certName,
                                             String filePath, String path, Long userId) {
        CertificationDTO certificationDTO = certificationService
                .baseQueryByEnvAndName(envId, certName);
        if (certificationDTO == null) {
            certificationDTO = new CertificationDTO();

            CertificationSpec certificationSpec = c7nCertification.getSpec();
            String domain = certificationSpec.getCommonName();
            List<String> dnsDomain = certificationSpec.getDnsNames();
            List<String> domains = new ArrayList<>();
            domains.add(domain);
            if (dnsDomain != null && !dnsDomain.isEmpty()) {
                domains.addAll(dnsDomain);
            }
            certificationDTO.setDomains(gson.toJson(domains));
            certificationDTO.setEnvId(envId);
            certificationDTO.setName(certName);
            certificationDTO.setStatus(CertificationStatus.OPERATING.getStatus());
            certificationDTO = certificationService.baseCreate(certificationDTO);
            CertificationExistCert existCert = c7nCertification.getSpec().getExistCert();
            if (existCert != null) {
                certificationDTO.setCertificationFileId(certificationService.baseStoreCertFile(
                        new CertificationFileDTO(existCert.getCert(), existCert.getKey())));
            }
            Long commandId = certificationService
                    .createCertCommand(CommandType.CREATE.getType(), certificationDTO.getId(), userId);
            certificationDTO.setCommandId(commandId);
            certificationService.baseUpdateCommandId(certificationDTO);
        }
        updateCommandSha(filePath, path, certificationDTO.getCommandId());
        return certificationDTO.getId();
    }

    private void updateCommandSha(String filePath, String path, Long commandId) {
        DevopsEnvCommandDTO devopsEnvCommandDTO = devopsEnvCommandService.baseQuery(commandId);
        devopsEnvCommandDTO.setSha(GitUtil.getFileLatestCommit(path + GIT_SUFFIX, filePath));
        devopsEnvCommandService.baseUpdate(devopsEnvCommandDTO);
    }
}
