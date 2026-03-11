package com.thoughtworks.agents.infr.repository.devtask;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("task_phase")
public class TaskPhasePO {

    @TableId
    private String id;

    @TableField("dev_task_id")
    private String devTaskId;

    @TableField("phase_type")
    private String phaseType;

    @TableField("cc_session_id")
    private String ccSessionId;

    private String output;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("finished_at")
    private LocalDateTime finishedAt;

    @TableField("failure_reason")
    private String failureReason;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDevTaskId() {
        return devTaskId;
    }

    public void setDevTaskId(String devTaskId) {
        this.devTaskId = devTaskId;
    }

    public String getPhaseType() {
        return phaseType;
    }

    public void setPhaseType(String phaseType) {
        this.phaseType = phaseType;
    }

    public String getCcSessionId() {
        return ccSessionId;
    }

    public void setCcSessionId(String ccSessionId) {
        this.ccSessionId = ccSessionId;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
