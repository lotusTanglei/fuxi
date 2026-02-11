package com.fuxi.script.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fuxi.script.entity.ExecutionPlan;
import com.fuxi.script.entity.ExecutionPlanItem;

import java.util.List;

public interface PlanService extends IService<ExecutionPlan> {
    
    /**
     * Create a new execution plan with items
     */
    void createPlan(ExecutionPlan plan, List<Long> scriptVersionIds);
    
    /**
     * Update an existing plan
     */
    void updatePlan(ExecutionPlan plan, List<Long> scriptVersionIds);
    
    /**
     * Execute the plan
     */
    void executePlan(Long planId);
    
    /**
     * Get items for a plan
     */
    List<ExecutionPlanItem> getPlanItems(Long planId);
    
    /**
     * Get items for a plan with script details (Title, Version)
     */
    List<java.util.Map<String, Object>> getPlanItemsWithDetails(Long planId);
    
    /**
     * Verify a plan item execution result
     */
    void verifyItem(Long itemId, boolean pass, String remark);
    
    /**
     * Submit execution receipt (OPS)
     */
    void submitReceipt(Long planId, String receipt);
    
    /**
     * Verify plan (TEST)
     */
    void verifyPlanTest(Long planId, boolean pass);
    
    /**
     * Finalize plan (LEADER)
     */
    void finalizePlan(Long planId);
}
