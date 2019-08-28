package io.choerodon.devops.infra.dto;

import javax.persistence.Table;

/**
 * @author lizongwei
 * @date 2019/7/3
 */
@Table(name = "devops_app_service_resource")
public class DevopsAppServiceResourceDTO {

    private Long appServiceId;
    private String resourceType;
    private Long resourceId;


    public DevopsAppServiceResourceDTO(){}


    public DevopsAppServiceResourceDTO(Long appServiceId,String resourceType,Long resourceId){
        this.appServiceId = appServiceId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public Long getAppServiceId() {
        return appServiceId;
    }

    public void setAppServiceId(Long appServiceId) {
        this.appServiceId = appServiceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }
}
