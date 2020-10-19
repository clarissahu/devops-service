package io.choerodon.devops.api.controller.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.devops.api.vo.deploy.HostDeployConfigVO;
import io.choerodon.devops.app.service.DevopsDeployService;
import io.choerodon.swagger.annotation.Permission;

/**
 * Created by Sheep on 2019/7/30.
 */
@RestController
@RequestMapping("/v1/projects/{project_id}/deploy")
public class DevopsDeployController {

    @Autowired
    private DevopsDeployService devopsDeployService;


    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "主机部署")
    @PostMapping("/host")
    public ResponseEntity<Void> hostDeploy(
            @ApiParam(value = "项目Id", required = true)
            @PathVariable(value = "project_id") Long projectId,
            @RequestBody HostDeployConfigVO hostDeployConfigVO) {
        devopsDeployService.hostDeploy(projectId, hostDeployConfigVO);
        return ResponseEntity.noContent().build();
    }


}
