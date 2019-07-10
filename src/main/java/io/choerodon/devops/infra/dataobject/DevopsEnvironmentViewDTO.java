package io.choerodon.devops.infra.dataobject;

import java.util.List;

/**
 * @author zmf
 */
public class DevopsEnvironmentViewDTO {
    private Long id;
    private String name;
    private Long clusterId;
    private Boolean isFailed;
    private List<DevopsApplicationViewDTO> apps;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }

    public Boolean getFailed() {
        return isFailed;
    }

    public void setFailed(Boolean failed) {
        isFailed = failed;
    }

    public List<DevopsApplicationViewDTO> getApps() {
        return apps;
    }

    public void setApps(List<DevopsApplicationViewDTO> apps) {
        this.apps = apps;
    }
}
