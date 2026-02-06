package com.fuxi.script.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fuxi.script.entity.ExecutionPlan;
import com.fuxi.script.entity.ExecutionPlanItem;
import com.fuxi.script.entity.ScriptInfo;
import com.fuxi.script.entity.ScriptVersion;
import com.fuxi.script.mapper.ExecutionPlanItemMapper;
import com.fuxi.script.mapper.ExecutionPlanMapper;
import com.fuxi.script.mapper.ScriptInfoMapper;
import com.fuxi.script.mapper.ScriptVersionMapper;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPlan(ExecutionPlan plan, List<Long> scriptVersionIds) {
        plan.setStatus("DRAFT");
        this.save(plan);
        
        savePlanItems(plan.getId(), scriptVersionIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlan(ExecutionPlan plan, List<Long> scriptVersionIds) {
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
        
        plan.setStatus("RUNNING");
        this.updateById(plan);
        
        List<ExecutionPlanItem> items = planItemMapper.selectList(new LambdaQueryWrapper<ExecutionPlanItem>()
                .eq(ExecutionPlanItem::getPlanId, planId)
                .orderByAsc(ExecutionPlanItem::getSortOrder));
        
        boolean allSuccess = true;
        
        for (ExecutionPlanItem item : items) {
            try {
                // Update item status
                item.setStatus("RUNNING");
                item.setStartedAt(LocalDateTime.now());
                planItemMapper.updateById(item);
                
                // Simulate Execution
                // In real world, this would connect to targetSite and run the script
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
                allSuccess = false;
                // Depending on policy, we might stop here or continue
            }
        }
        
        plan.setStatus(allSuccess ? "COMPLETED" : "FAILED");
        this.updateById(plan);
    }

    @Override
    public List<ExecutionPlanItem> getPlanItems(Long planId) {
        return planItemMapper.selectList(new LambdaQueryWrapper<ExecutionPlanItem>()
                .eq(ExecutionPlanItem::getPlanId, planId)
                .orderByAsc(ExecutionPlanItem::getSortOrder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void verifyItem(Long itemId, boolean pass, String remark) {
        ExecutionPlanItem item = planItemMapper.selectById(itemId);
        if (item != null) {
            item.setVerifyStatus(pass ? "PASS" : "FAIL");
            item.setVerifyRemark(remark);
            item.setVerifiedBy(SecurityContextHolder.getContext().getAuthentication().getName());
            item.setVerifiedAt(LocalDateTime.now());
            planItemMapper.updateById(item);
        }
    }
}
