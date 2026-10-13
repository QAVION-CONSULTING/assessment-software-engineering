<script setup lang="ts">
import type { Project } from '@/types/project'
import { formatEur, formatHours } from '@/utils/format'
import BudgetStatusBadge from './BudgetStatusBadge.vue'
import UtilizationBar from './UtilizationBar.vue'

defineProps<{ project: Project }>()
defineEmits<{ edit: [project: Project] }>()
</script>

<template>
  <article class="card" :aria-labelledby="`project-${project.id}`">
    <div class="card-head">
      <h3 :id="`project-${project.id}`">{{ project.name }}</h3>
      <BudgetStatusBadge :status="project.budget.overallStatus" data-testid="overall-status" />
    </div>
    <dl class="facts">
      <div><dt>Time budget</dt><dd>{{ formatHours(project.timeBudgetHours) }}</dd></div>
      <div><dt>Cost budget</dt><dd>{{ formatEur(project.costBudgetEur) }}</dd></div>
      <div><dt>Hourly rate</dt><dd>{{ formatEur(project.hourlyRateEur) }}</dd></div>
      <div><dt>Recorded hours</dt><dd data-testid="recorded-hours">{{ formatHours(project.budget.totalHours) }}</dd></div>
      <div><dt>Recorded cost</dt><dd data-testid="recorded-cost">{{ formatEur(project.budget.totalCostEur) }}</dd></div>
    </dl>
    <UtilizationBar label="Time" :percent="project.budget.timeUtilizationPercent" :status="project.budget.timeStatus" />
    <UtilizationBar label="Cost" :percent="project.budget.costUtilizationPercent" :status="project.budget.costStatus" />
    <div class="actions">
      <button type="button" class="btn" :aria-label="`Edit ${project.name}`" @click="$emit('edit', project)">Edit</button>
    </div>
  </article>
</template>
