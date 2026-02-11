package com.fuxi.script.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fuxi.script.entity.*;
import com.fuxi.script.mapper.*;
import com.fuxi.script.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanServiceImpl extends ServiceImpl<ExecutionPlanMapper, ExecutionPlan> implements PlanService {

    private final ExecutionPlanItemMapper planItemMapper;
    private final ScriptVersionMapper scriptVersionMapper;
    private final ScriptInfoMapper scriptInfoMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPlan(ExecutionPlan plan, List<Long> scriptVersionIds) {
        // Initial status is PENDING to be ready for OPS execution
        plan.setStatus("PENDING");
        // Set creator
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        plan.setCreatedBy(currentUsername);
        
        this.save(plan);
        
        savePlanItems(plan.getId(), scriptVersionIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlan(ExecutionPlan plan, List<Long> scriptVersionIds) {
        // Reset to PENDING on update? Or keep status?
        // Usually if updated, it might need re-execution, but let's keep status or ensure it's PENDING if DRAFT
        if ("DRAFT".equals(plan.getStatus())) {
            plan.setStatus("PENDING");
        }
        this.updateById(plan);
        
        // Remove existing items
        planItemMapper.delete(new LambdaQueryWrapper<ExecutionPlanItem>()
                .eq(ExecutionPlanItem::getPlanId, plan.getId()));
        
        // Add new items
        savePlanItems(plan.getId(), scriptVersionIds);
    }

    private void savePlanItems(Long planId, List<Long> scriptVersionIds) {
        if (scriptVersionIds != null) {
            for (int i = 0; i < scriptVersionIds.size(); i++) {
                ExecutionPlanItem item = new ExecutionPlanItem();
                item.setPlanId(planId);
                item.setScriptVersionId(scriptVersionIds.get(i));
                item.setSortOrder(i + 1);
                item.setStatus("PENDING");
                planItemMapper.insert(item);
            }
        }
    }

    @Override
    @Async
    public void executePlan(Long planId) {
        ExecutionPlan plan = this.getById(planId);
        if (plan == null) return;
        
        // Strict Workflow Check: Only allow PENDING or RUNNING
        if (!"PENDING".equals(plan.getStatus()) && !"RUNNING".equals(plan.getStatus())) {
            log.warn("Cannot execute plan {}: Status is {}", planId, plan.getStatus());
            return;
        }
        
        // Update status to RUNNING if it was PENDING
        if ("PENDING".equals(plan.getStatus())) {
            plan.setStatus("RUNNING");
            this.updateById(plan);
        }
        
        List<ExecutionPlanItem> items = planItemMapper.selectList(new LambdaQueryWrapper<ExecutionPlanItem>()
                .eq(ExecutionPlanItem::getPlanId, planId)
                .orderByAsc(ExecutionPlanItem::getSortOrder));
        
        for (ExecutionPlanItem item : items) {
            try {
                // Update item status
                item.setStatus("RUNNING");
                item.setStartedAt(LocalDateTime.now());
                planItemMapper.updateById(item);
                
                // Simulate Execution
                ScriptVersion version = scriptVersionMapper.selectById(item.getScriptVersionId());
                ScriptInfo script = scriptInfoMapper.selectById(version.getScriptId());
                
                log.info("Executing script: {} on site: {}", script.getTitle(), plan.getTargetSite());
                Thread.sleep(2000); // Simulate work
                
                // Update item success
                item.setStatus("SUCCESS");
                item.setExecutionResult("Execution completed successfully.");
                item.setFinishedAt(LocalDateTime.now());
                planItemMapper.updateById(item);
                
            } catch (Exception e) {
                log.error("Execution failed for item: " + item.getId(), e);
                item.setStatus("FAILED");
                item.setExecutionResult("Error: " + e.getMessage());
                item.setFinishedAt(LocalDateTime.now());
                planItemMapper.updateById(item);
            }
        }
        // Note: We do NOT set plan status to COMPLETED here anymore.
        // It stays RUNNING until OPS submits receipt.
    }

    @Override
    public List<ExecutionPlanItem> getPlanItems(Long planId) {
        return planItemMapper.selectList(new LambdaQueryWrapper<ExecutionPlanItem>()
                .eq(ExecutionPlanItem::getPlanId, planId)
                .orderByAsc(ExecutionPlanItem::getSortOrder));
    }

    @Override
    public List<java.util.Map<String, Object>> getPlanItemsWithDetails(Long planId) {
        List<ExecutionPlanItem> items = getPlanItems(planId);
        
        return items.stream().map(item -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", item.getId());
            map.put("planId", item.getPlanId());
            map.put("scriptVersionId", item.getScriptVersionId());
            map.put("sortOrder", item.getSortOrder());
            map.put("status", item.getStatus());
            map.put("executionResult", item.getExecutionResult());
            map.put("startedAt", item.getStartedAt());
            map.put("finishedAt", item.getFinishedAt());
            map.put("verifyStatus", item.getVerifyStatus());
            map.put("verifyRemark", item.getVerifyRemark());
            map.put("verifiedBy", item.getVerifiedBy());
            map.put("verifiedAt", item.getVerifiedAt());
            
            try {
                ScriptVersion version = scriptVersionMapper.selectById(item.getScriptVersionId());
                if (version != null) {
                    map.put("versionNum", version.getVersionNum());
                    ScriptInfo script = scriptInfoMapper.selectById(version.getScriptId());
                    if (script != null) {
                        map.put("scriptTitle", script.getTitle());
                        
                        // Resolve Real Name for Script Creator
                        String createdBy = script.getCreatedBy();
                        if (createdBy != null && !createdBy.isEmpty()) {
                            SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, createdBy));
                            map.put("scriptCreatedBy", user != null ? user.getRealName() : createdBy);
                        } else {
                            map.put("scriptCreatedBy", "");
                        }
                        
                        map.put("scriptType", script.getType());
                        map.put("scriptTargetEnv", script.getTargetEnv());
                    } else {
                         map.put("scriptTitle", "Unknown Script (ID: " + version.getScriptId() + ")");
                         map.put("scriptCreatedBy", "Unknown");
                         map.put("scriptType", "Unknown");
                         map.put("scriptTargetEnv", "Unknown");
                    }
                } else {
                    map.put("versionNum", "Unknown");
                    map.put("scriptTitle", "Unknown Version");
                    map.put("scriptCreatedBy", "Unknown");
                    map.put("scriptType", "Unknown");
                    map.put("scriptTargetEnv", "Unknown");
                }
            } catch (Exception e) {
                map.put("scriptTitle", "Error fetching info");
                map.put("versionNum", "Error");
                map.put("scriptCreatedBy", "Error");
                map.put("scriptType", "Error");
                map.put("scriptTargetEnv", "Error");
            }
            return map;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verifyItem(Long itemId, boolean pass, String remark) {
        // ... (kept for backward compatibility or item level verification if needed)
        // Ideally this should be removed or integrated, but let's keep it for now.
        // The new flow uses Plan level status transitions.
        ExecutionPlanItem item = planItemMapper.selectById(itemId);
        if (item != null) {
             // ... existing logic ...
            item.setVerifyStatus(pass ? "PASS" : "FAIL");
            item.setVerifyRemark(remark);
            item.setVerifiedBy(SecurityContextHolder.getContext().getAuthentication().getName());
            item.setVerifiedAt(LocalDateTime.now());
            planItemMapper.updateById(item);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReceipt(Long planId, String receipt) {
        ExecutionPlan plan = this.getById(planId);
        if (plan != null) {
            // Strict Workflow Check: Only allow RUNNING
            if (!"RUNNING".equals(plan.getStatus())) {
                throw new IllegalStateException("Cannot submit receipt: Plan is not in RUNNING state.");
            }
            plan.setExecutionReceipt(receipt);
            plan.setStatus("VERIFYING_TEST");
            this.updateById(plan);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verifyPlanTest(Long planId, boolean pass) {
        ExecutionPlan plan = this.getById(planId);
        if (plan != null) {
            // Strict Workflow Check: Only allow VERIFYING_TEST
            if (!"VERIFYING_TEST".equals(plan.getStatus())) {
                 throw new IllegalStateException("Cannot verify (TEST): Plan is not in VERIFYING_TEST state.");
            }
            if (pass) {
                plan.setStatus("VERIFYING_LEADER");
            } else {
                // If rejected by TEST, go back to PENDING (OPS needs to re-execute)
                plan.setStatus("PENDING"); 
            }
            this.updateById(plan);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finalizePlan(Long planId) {
        ExecutionPlan plan = this.getById(planId);
        if (plan != null) {
            // Strict Workflow Check: Only allow VERIFYING_LEADER
            if (!"VERIFYING_LEADER".equals(plan.getStatus())) {
                 throw new IllegalStateException("Cannot finalize (LEADER): Plan is not in VERIFYING_LEADER state.");
            }
            plan.setStatus("COMPLETED");
            this.updateById(plan);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlan(Long planId) {
        ExecutionPlan plan = this.getById(planId);
        if (plan != null) {
            // Logical delete plan
            this.removeById(planId);
            // Logical delete items is handled by MyBatis Plus logic delete if configured,
            // otherwise we might need to manually delete them.
            // Assuming execution_plan has logic delete, items might not.
            // Let's delete items physically or logically.
            // Given the schema has is_deleted for execution_plan, but items table doesn't seem to have it in the create script above?
            // Let's check schema.sql again.
            // execution_plan_item does NOT have is_deleted. So we should physically delete items.
            planItemMapper.delete(new LambdaQueryWrapper<ExecutionPlanItem>().eq(ExecutionPlanItem::getPlanId, planId));
        }
    }
}