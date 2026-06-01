<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2 class="section-title">教学考试管理</h2>
        <div class="muted">维护题库、发布试卷、查看学生成绩</div>
      </div>
      <el-select v-model="courseFilter" clearable placeholder="按课程筛选" class="filter">
        <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
    </div>

    <el-tabs v-model="active" class="route-tabs">
      <el-tab-pane label="题库管理" name="questions">
        <div class="toolbar">
          <el-input v-model="keyword" clearable placeholder="搜索题干/答案" class="filter" />
          <div>
            <el-button @click="triggerImportQuestions">导入xls</el-button>
            <input ref="questionFileInput" type="file" accept=".xlsx" class="hidden-file" @change="handleQuestionImport" />
            <el-button @click="loadDuplicateQuestions">查重</el-button>
            <el-button @click="exportQuestions">导出xls</el-button>
            <el-button @click="aiQuestionDialog=true">AI出题</el-button>
            <el-button type="primary" @click="openQuestion()">新增题目</el-button>
          </div>
        </div>
        <el-table :data="pagedQuestions" class="panel" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column label="课程" width="150"><template #default="{ row }">{{ courseName(row.courseId) }}</template></el-table-column>
          <el-table-column prop="type" label="题型" width="100"><template #default="{ row }"><el-tag>{{ typeName(row.type) }}</el-tag></template></el-table-column>
          <el-table-column label="题干" min-width="320" show-overflow-tooltip>
            <template #default="{ row }">{{ cleanGeneratedSuffix(row.stem) }}</template>
          </el-table-column>
          <el-table-column prop="correctAnswer" label="答案" width="120" />
          <el-table-column prop="knowledgePoint" label="知识点" width="160" show-overflow-tooltip />
          <el-table-column prop="reviewStatus" label="审核" width="100"><template #default="{ row }"><el-tag :type="row.reviewStatus === 'APPROVED' ? 'success' : row.reviewStatus === 'REJECTED' ? 'danger' : 'info'">{{ reviewStatusName(row.reviewStatus) }}</el-tag></template></el-table-column>
          <el-table-column prop="difficulty" label="难度" width="140"><template #default="{ row }"><el-rate :model-value="row.difficulty" disabled /></template></el-table-column>
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <el-button link type="warning" @click="reviewQuestion(row, row.reviewStatus === 'APPROVED' ? 'DRAFT' : 'APPROVED')">切换审核</el-button>
              <el-button link type="primary" @click="openQuestion(row)">编辑</el-button>
              <el-button link type="danger" @click="removeQuestion(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-actions"><el-pagination v-model:current-page="pagers.questions.page" v-model:page-size="pagers.questions.size" :total="filteredQuestions.length" :page-sizes="[5,8,12,20]" layout="total, sizes, prev, pager, next" /></div>
      </el-tab-pane>

      <el-tab-pane label="试卷管理" name="papers">
        <div class="toolbar">
          <span class="muted">勾选题目并设置分值后发布试卷</span>
          <div>
            <el-button @click="exportPapers">导出xls</el-button>
            <el-button @click="aiPaperDialog=true">AI出卷</el-button>
            <el-button type="primary" @click="openPaper()">新增试卷</el-button>
          </div>
        </div>
        <el-table :data="pagedPapers" class="panel" stripe>
          <el-table-column label="试卷" min-width="220">
            <template #default="{ row }">{{ cleanGeneratedSuffix(row.title) }}</template>
          </el-table-column>
          <el-table-column label="课程" width="150"><template #default="{ row }">{{ courseName(row.courseId) }}</template></el-table-column>
          <el-table-column prop="durationMinutes" label="时长" width="100" />
          <el-table-column prop="totalScore" label="总分" width="100" />
          <el-table-column prop="allowRetake" label="重考" width="90"><template #default="{ row }">{{ row.allowRetake ? '允许' : '禁止' }}</template></el-table-column>
          <el-table-column prop="published" label="状态" width="100"><template #default="{ row }"><el-tag :type="row.published ? 'success' : 'info'">{{ row.published ? '已发布' : '草稿' }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="170">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPaper(row)">编辑</el-button>
              <el-button link type="danger" @click="removePaper(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-actions"><el-pagination v-model:current-page="pagers.papers.page" v-model:page-size="pagers.papers.size" :total="papers.length" :page-sizes="[5,8,12,20]" layout="total, sizes, prev, pager, next" /></div>
      </el-tab-pane>

      <el-tab-pane label="成绩管理" name="scores">
        <div class="toolbar">
          <el-input v-model="scoreKeyword" clearable placeholder="搜索学生/试卷" class="filter" />
          <div><el-button @click="exportScores">导出xls</el-button><el-button @click="load">刷新</el-button></div>
        </div>
        <el-table :data="pagedScores" class="panel" stripe>
          <el-table-column label="试卷" min-width="220">
            <template #default="{ row }">{{ cleanGeneratedSuffix(row.paperTitle) }}</template>
          </el-table-column>
          <el-table-column prop="studentName" label="学生" width="140" />
          <el-table-column prop="score" label="成绩" width="100">
            <template #default="{ row }"><strong>{{ row.score }}</strong></template>
          </el-table-column>
          <el-table-column prop="objectiveScore" label="客观题" width="100" />
          <el-table-column prop="subjectiveScore" label="主观题" width="100" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">{{ examStatusName(row.status) }}</template>
          </el-table-column>
          <el-table-column label="复核" width="100">
            <template #default="{ row }">{{ formatReviewStatusName(row.reviewStatus) }}</template>
          </el-table-column>
          <el-table-column label="提交时间" width="190">
            <template #default="{ row }">{{ formatDateTime(row.submittedAt, '') }}</template>
          </el-table-column>
        </el-table>
        <div class="table-actions"><el-pagination v-model:current-page="pagers.scores.page" v-model:page-size="pagers.scores.size" :total="filteredScores.length" :page-sizes="[5,8,12,20]" layout="total, sizes, prev, pager, next" /></div>
      </el-tab-pane>

      <el-tab-pane label="人工阅卷" name="manual">
        <div class="toolbar">
          <span class="muted">处理简答题、编程题等需要复核的答案</span>
          <el-button @click="load">刷新</el-button>
        </div>
        <el-table :data="manualRows" class="panel" stripe>
          <el-table-column prop="paperId" label="试卷ID" width="90" />
          <el-table-column label="题目"><template #default="{ row }">{{ cleanGeneratedSuffix(row.question.stem) }}</template></el-table-column>
          <el-table-column label="学生答案" width="220"><template #default="{ row }">{{ row.studentAnswer || '未作答' }}</template></el-table-column>
          <el-table-column label="得分" width="120">
            <template #default="{ row }">
              <el-input-number v-model="manualGrades[row.answerId].score" :min="0" :max="row.maxScore || 100" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="批注" width="240">
            <template #default="{ row }">
              <el-input v-model="manualGrades[row.answerId].comment" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="primary" @click="submitManual(row)">提交</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="成绩分析" name="analysis">
        <div class="toolbar">
          <span class="muted">按试卷、班级和知识点查看薄弱项</span>
          <el-button @click="loadAnalysis">刷新</el-button>
        </div>
        <el-table :data="analysisRows" class="panel" stripe>
          <el-table-column label="试卷" min-width="220">
            <template #default="{ row }">{{ cleanGeneratedSuffix(row.paperTitle) }}</template>
          </el-table-column>
          <el-table-column prop="count" label="人数" width="90" />
          <el-table-column prop="averageScore" label="均分" width="90" />
          <el-table-column prop="maxScore" label="最高" width="90" />
          <el-table-column prop="minScore" label="最低" width="90" />
          <el-table-column prop="passRate" label="及格率" width="100">
            <template #default="{ row }">{{ row.passRate }}%</template>
          </el-table-column>
          <el-table-column label="薄弱知识点">
            <template #default="{ row }">
              <span v-for="kp in row.weakKnowledgePoints" :key="kp.knowledgePoint" class="chip">{{ kp.knowledgePoint }}({{ kp.wrongCount }})</span>
            </template>
          </el-table-column>
          <el-table-column label="题目正确率" min-width="220">
            <template #default="{ row }">
              <span v-for="item in row.questionAccuracy" :key="item.questionId" class="chip">Q{{ item.questionId }} {{ item.accuracy }}%</span>
            </template>
          </el-table-column>
          <el-table-column label="班级统计" min-width="260">
            <template #default="{ row }">
              <span v-for="item in row.classStats" :key="item.classId || item.className" class="chip">{{ item.className }} 均{{ item.averageScore }} / 及格{{ item.passRate }}%</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="考试监控" name="monitor">
        <div class="toolbar">
          <span class="muted">记录切屏、全屏退出、异常行为等事件</span>
          <el-button @click="loadMonitor">刷新</el-button>
        </div>
        <el-table :data="monitorRows" class="panel" stripe>
          <el-table-column prop="paperId" label="试卷ID" width="90" />
          <el-table-column prop="studentId" label="学生ID" width="90" />
          <el-table-column prop="eventType" label="事件" width="140" />
          <el-table-column prop="detail" label="详情" />
          <el-table-column prop="ip" label="IP" width="140" />
          <el-table-column prop="userAgent" label="设备" min-width="220" show-overflow-tooltip />
          <el-table-column label="时间" width="180">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="成绩复查" name="appeals">
        <div class="toolbar">
          <span class="muted">学生申诉与教师处理记录</span>
          <el-button @click="loadAppeals">刷新</el-button>
        </div>
        <el-table :data="appealRows" class="panel" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="attemptId" label="考试记录" width="100" />
          <el-table-column prop="studentId" label="学生ID" width="90" />
          <el-table-column prop="reason" label="原因" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">{{ appealStatusName(row.status) }}</template>
          </el-table-column>
          <el-table-column prop="reply" label="回复" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" @click="reviewAppeal(row, 'APPROVED')">同意</el-button>
              <el-button link type="danger" @click="reviewAppeal(row, 'REJECTED')">拒绝</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="questionDialog" :title="questionForm.question.id ? '编辑题目' : '新增题目'" width="760px">
      <el-form :model="questionForm.question" label-width="86px">
        <el-form-item label="课程"><el-select v-model="questionForm.question.courseId"><el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-form-item>
        <el-form-item label="题型">
            <el-radio-group v-model="questionForm.question.type" @change="syncOptionsForType">
              <el-radio-button value="SINGLE">单选</el-radio-button>
              <el-radio-button value="MULTIPLE">多选</el-radio-button>
              <el-radio-button value="JUDGE">判断</el-radio-button>
              <el-radio-button value="SHORT">简答</el-radio-button>
              <el-radio-button value="PROGRAM">编程</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="题干"><el-input v-model="questionForm.question.stem" type="textarea" :rows="3" /></el-form-item>
        <el-form-item v-if="isObjective(questionForm.question.type)" label="选项">
          <div class="option-list">
            <div v-for="(option, index) in questionForm.options" :key="index" class="option-row">
              <el-input v-model="option.optionKey" class="option-key" />
              <el-input v-model="option.optionText" />
              <el-button v-if="questionForm.question.type !== 'JUDGE'" @click="questionForm.options.splice(index, 1)">删除</el-button>
            </div>
            <el-button v-if="questionForm.question.type !== 'JUDGE'" @click="addOption">添加选项</el-button>
          </div>
        </el-form-item>
        <el-form-item label="正确答案">
          <el-input v-model="questionForm.question.correctAnswer" placeholder="多选用逗号分隔，如 A,B,D" />
        </el-form-item>
        <el-form-item label="知识点"><el-input v-model="questionForm.question.knowledgePoint" /></el-form-item>
        <el-form-item label="审核"><el-select v-model="questionForm.question.reviewStatus"><el-option label="草稿" value="DRAFT" /><el-option label="通过" value="APPROVED" /><el-option label="驳回" value="REJECTED" /></el-select></el-form-item>
        <el-form-item label="难度"><el-rate v-model="questionForm.question.difficulty" /></el-form-item>
        <el-form-item label="解析"><el-input v-model="questionForm.question.analysis" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="questionDialog=false">取消</el-button><el-button type="primary" @click="saveQuestion">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="aiQuestionDialog" title="AI出题" width="980px">
      <div class="ai-layout">
        <div>
          <el-form :model="aiQuestionForm" label-width="90px">
            <el-form-item label="课程"><el-select v-model="aiQuestionForm.courseId"><el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-form-item>
            <el-form-item label="知识点"><el-input v-model="aiQuestionForm.topic" placeholder="例如：Java 集合、SQL 查询、Spring Boot 自动配置" /></el-form-item>
            <el-form-item label="题型">
              <el-radio-group v-model="aiQuestionForm.type">
                <el-radio-button value="SINGLE">单选</el-radio-button>
                <el-radio-button value="MULTIPLE">多选</el-radio-button>
                <el-radio-button value="JUDGE">判断</el-radio-button>
                <el-radio-button value="SHORT">简答</el-radio-button>
                <el-radio-button value="PROGRAM">编程</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="数量"><el-input-number v-model="aiQuestionForm.count" :min="1" :max="10" /></el-form-item>
            <el-form-item label="难度"><el-rate v-model="aiQuestionForm.difficulty" /></el-form-item>
            <el-form-item><el-button type="primary" :loading="aiQuestionLoading" @click="generateQuestions">生成题目草稿</el-button></el-form-item>
          </el-form>
        </div>
        <div>
          <div class="toolbar">
            <h3>题目草稿</h3>
            <el-button :disabled="aiQuestionDrafts.length === 0" type="success" @click="saveGeneratedQuestions">保存到题库</el-button>
          </div>
          <el-empty v-if="aiQuestionDrafts.length === 0" description="暂无草稿" />
          <div v-for="(draft, index) in aiQuestionDrafts" :key="index" class="draft">
            <div class="toolbar">
              <strong>{{ index + 1 }}. {{ typeName(draft.question.type) }}</strong>
              <el-rate v-model="draft.question.difficulty" />
            </div>
            <el-input v-model="draft.question.stem" type="textarea" :rows="2" />
            <div class="option-list compact">
              <div v-for="option in draft.options" :key="option.optionKey" class="option-row">
                <el-input v-model="option.optionKey" class="option-key" />
                <el-input v-model="option.optionText" />
              </div>
            </div>
            <el-input v-model="draft.question.correctAnswer" class="draft-input" placeholder="正确答案" />
            <el-input v-model="draft.question.analysis" type="textarea" :rows="2" placeholder="解析" />
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="paperDialog" :title="paperForm.paper.id ? '编辑试卷' : '新增试卷'" width="900px">
      <el-form :model="paperForm.paper" label-width="86px">
        <el-form-item label="试卷名称"><el-input v-model="paperForm.paper.title" /></el-form-item>
        <el-form-item label="课程"><el-select v-model="paperForm.paper.courseId"><el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-form-item>
        <el-form-item label="时长"><el-input-number v-model="paperForm.paper.durationMinutes" :min="5" :max="240" /></el-form-item>
            <el-form-item label="发布"><el-switch v-model="paperForm.paper.published" :active-value="1" :inactive-value="0" /></el-form-item>
            <el-form-item label="重考"><el-switch v-model="paperForm.paper.allowRetake" :active-value="1" :inactive-value="0" /></el-form-item>
            <el-form-item label="开始"><el-date-picker v-model="paperForm.paper.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
            <el-form-item label="结束"><el-date-picker v-model="paperForm.paper.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
          </el-form>
      <el-table :data="pagedCandidateQuestions" class="question-picker" stripe>
        <el-table-column width="58">
          <template #default="{ row }"><el-checkbox :model-value="selectedQuestionIds.includes(row.id)" @change="toggleQuestion(row)" /></template>
        </el-table-column>
        <el-table-column prop="stem" label="题干" show-overflow-tooltip />
        <el-table-column prop="type" label="题型" width="90"><template #default="{ row }">{{ typeName(row.type) }}</template></el-table-column>
        <el-table-column label="分值" width="130">
          <template #default="{ row }"><el-input-number v-model="paperScores[row.id]" :min="1" :max="100" size="small" /></template>
        </el-table-column>
      </el-table>
      <div class="table-actions"><el-pagination v-model:current-page="pagers.candidates.page" v-model:page-size="pagers.candidates.size" :total="paperCandidateQuestions.length" :page-sizes="[5,8,12,20]" layout="total, sizes, prev, pager, next" /></div>
      <template #footer><span class="muted">已选 {{ selectedQuestionIds.length }} 题，总分 {{ paperTotal }}</span><el-button @click="paperDialog=false">取消</el-button><el-button type="primary" @click="savePaper">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="aiPaperDialog" title="AI出卷" width="980px">
      <div class="ai-layout">
        <div>
          <el-form :model="aiPaperForm" label-width="90px">
            <el-form-item label="课程"><el-select v-model="aiPaperForm.courseId"><el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-form-item>
            <el-form-item label="试卷名"><el-input v-model="aiPaperForm.title" /></el-form-item>
            <el-form-item label="考查重点"><el-input v-model="aiPaperForm.topic" placeholder="例如：面向对象基础 + 集合框架" /></el-form-item>
            <el-form-item label="题目数"><el-input-number v-model="aiPaperForm.questionCount" :min="1" :max="50" /></el-form-item>
            <el-form-item label="总分"><el-input-number v-model="aiPaperForm.totalScore" :min="10" :max="200" /></el-form-item>
            <el-form-item label="时长"><el-input-number v-model="aiPaperForm.durationMinutes" :min="5" :max="240" /></el-form-item>
            <el-form-item label="知识点"><el-input v-model="aiPaperForm.knowledgePointsText" placeholder="用逗号分隔多个知识点" /></el-form-item>
            <el-form-item label="题型比例">
              <div class="ratio-grid">
                <el-input-number v-model="aiPaperForm.singleCount" :min="0" :max="50" /><span>单选</span>
                <el-input-number v-model="aiPaperForm.multipleCount" :min="0" :max="50" /><span>多选</span>
                <el-input-number v-model="aiPaperForm.judgeCount" :min="0" :max="50" /><span>判断</span>
                <el-input-number v-model="aiPaperForm.shortCount" :min="0" :max="50" /><span>简答</span>
                <el-input-number v-model="aiPaperForm.programCount" :min="0" :max="50" /><span>编程</span>
              </div>
            </el-form-item>
            <el-form-item label="难度比例">
              <div class="ratio-grid difficulty-grid">
                <el-input-number v-model="aiPaperForm.easyCount" :min="0" :max="50" /><span>简单</span>
                <el-input-number v-model="aiPaperForm.mediumCount" :min="0" :max="50" /><span>中等</span>
                <el-input-number v-model="aiPaperForm.hardCount" :min="0" :max="50" /><span>困难</span>
              </div>
            </el-form-item>
            <el-form-item><el-button type="primary" :loading="aiPaperLoading" @click="generatePaper">生成试卷草稿</el-button></el-form-item>
          </el-form>
        </div>
        <div>
          <div class="toolbar">
            <h3>试卷草稿</h3>
            <el-button :disabled="!aiPaperDraft" type="success" @click="saveGeneratedPaper">保存为试卷</el-button>
          </div>
          <el-empty v-if="!aiPaperDraft" description="暂无草稿" />
          <template v-else>
            <el-form :model="aiPaperDraft.paper" label-width="80px">
              <el-form-item label="名称"><el-input v-model="aiPaperDraft.paper.title" /></el-form-item>
              <el-form-item label="时长"><el-input-number v-model="aiPaperDraft.paper.durationMinutes" :min="5" /></el-form-item>
              <el-form-item label="发布"><el-switch v-model="aiPaperDraft.paper.published" :active-value="1" :inactive-value="0" /></el-form-item>
              <el-form-item label="重考"><el-switch v-model="aiPaperDraft.paper.allowRetake" :active-value="1" :inactive-value="0" /></el-form-item>
            </el-form>
            <el-table :data="aiPaperDraft.questions" stripe>
              <el-table-column prop="sortNo" label="#" width="60" />
              <el-table-column label="题目"><template #default="{ row }">{{ questionStem(row.questionId) }}</template></el-table-column>
              <el-table-column label="分值" width="130"><template #default="{ row }"><el-input-number v-model="row.score" :min="1" size="small" /></template></el-table-column>
            </el-table>
          </template>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import {
  appealStatusName,
  cleanGeneratedSuffix,
  createPager,
  examStatusName,
  exportXls,
  formatDateTime,
  reviewStatusName as formatReviewStatusName,
  usePagedRows
} from '../utils/tableTools'

const route = useRoute()
const active = ref(route.params.section || 'questions')
const keyword = ref('')
const scoreKeyword = ref('')
const courseFilter = ref(null)
const courses = ref([])
const questions = ref([])
const papers = ref([])
const scores = ref([])
const manualRows = ref([])
const analysisRows = ref([])
const monitorRows = ref([])
const appealRows = ref([])
const questionDuplicates = ref([])
const questionDialog = ref(false)
const paperDialog = ref(false)
const aiQuestionDialog = ref(false)
const aiPaperDialog = ref(false)
const questionFileInput = ref(null)
const questionForm = reactive({ question: {}, options: [] })
const paperForm = reactive({ paper: {}, selected: [] })
const paperScores = reactive({})
const manualGrades = reactive({})
const aiQuestionLoading = ref(false)
const aiPaperLoading = ref(false)
const aiQuestionDrafts = ref([])
const aiPaperDraft = ref(null)
const aiQuestionForm = reactive({ courseId: null, topic: '', type: 'SINGLE', count: 3, difficulty: 2 })
const aiPaperForm = reactive({
  courseId: null,
  title: 'AI智能测试卷',
  topic: '',
  knowledgePointsText: '',
  questionCount: 5,
  totalScore: 100,
  durationMinutes: 45,
  singleCount: 2,
  multipleCount: 1,
  judgeCount: 1,
  shortCount: 1,
  programCount: 0,
  easyCount: 2,
  mediumCount: 2,
  hardCount: 1
})
const pagers = {
  questions: createPager(),
  papers: createPager(),
  scores: createPager(),
  candidates: createPager()
}

const selectedQuestionIds = computed(() => paperForm.selected.map((item) => item.questionId))
const paperTotal = computed(() => paperForm.selected.reduce((sum, item) => sum + Number(paperScores[item.questionId] || item.score || 0), 0))
const paperCandidateQuestions = computed(() => questions.value.filter((q) => !paperForm.paper.courseId || q.courseId === paperForm.paper.courseId))
const filteredQuestions = computed(() => questions.value.filter((q) => (!courseFilter.value || q.courseId === courseFilter.value) && (!keyword.value || [q.stem, q.correctAnswer].some((v) => String(v || '').includes(keyword.value)))))
const filteredScores = computed(() => scores.value.filter((s) => !scoreKeyword.value || [s.paperTitle, s.studentName].some((v) => String(v || '').includes(scoreKeyword.value))))
const paperRows = computed(() => papers.value)
const pagedQuestions = usePagedRows(filteredQuestions, pagers.questions)
const pagedPapers = usePagedRows(paperRows, pagers.papers)
const pagedScores = usePagedRows(filteredScores, pagers.scores)
const pagedCandidateQuestions = usePagedRows(paperCandidateQuestions, pagers.candidates)

async function load() {
  const [courseData, questionData, paperData, scoreData, manualData, analysisData, monitorData, appealData] = await Promise.all([
    http.get('/common/courses'),
    http.get('/teacher/questions'),
    http.get('/teacher/papers'),
    http.get('/teacher/scores'),
    http.get('/teacher/manual'),
    http.get('/teacher/analysis'),
    http.get('/teacher/monitor'),
    http.get('/teacher/appeals')
  ])
  courses.value = courseData
  questions.value = questionData
  papers.value = paperData
  scores.value = scoreData
  manualRows.value = manualData
  analysisRows.value = analysisData
  monitorRows.value = monitorData
  appealRows.value = appealData
  manualRows.value.forEach((row) => {
    if (!manualGrades[row.answerId]) {
      manualGrades[row.answerId] = { score: row.manualScore ?? row.score ?? 0, comment: row.comment || '' }
    }
  })
  aiQuestionForm.courseId = aiQuestionForm.courseId || courseData[0]?.id
  aiPaperForm.courseId = aiPaperForm.courseId || courseData[0]?.id
}

async function generateQuestions() {
  aiQuestionLoading.value = true
  try {
    aiQuestionDrafts.value = await http.post('/teacher/ai/questions', aiQuestionForm)
    ElMessage.success('AI题目草稿已生成')
  } finally {
    aiQuestionLoading.value = false
  }
}

async function saveGeneratedQuestions() {
  for (const draft of aiQuestionDrafts.value) {
    await http.post('/teacher/questions', draft)
  }
  ElMessage.success(`已保存 ${aiQuestionDrafts.value.length} 道题目`)
  aiQuestionDrafts.value = []
  aiQuestionDialog.value = false
  await load()
}

async function generatePaper() {
  aiPaperLoading.value = true
  try {
    const payload = {
      ...aiPaperForm,
      knowledgePoints: String(aiPaperForm.knowledgePointsText || '')
        .split(/[,\n，]/)
        .map((s) => s.trim())
        .filter(Boolean)
    }
    aiPaperDraft.value = await http.post('/teacher/ai/paper', payload)
    ElMessage.success('AI试卷草稿已生成')
  } finally {
    aiPaperLoading.value = false
  }
}

async function saveGeneratedPaper() {
  await http.post('/teacher/papers', aiPaperDraft.value)
  ElMessage.success('试卷已保存')
  aiPaperDraft.value = null
  aiPaperDialog.value = false
  await load()
}

async function openQuestion(row = null) {
  if (row) {
    const detail = await http.get(`/teacher/questions/${row.id}`)
    questionForm.question = { ...detail.question }
    questionForm.options = detail.options.map((o) => ({ ...o }))
  } else {
    questionForm.question = { courseId: courses.value[0]?.id, type: 'SINGLE', difficulty: 1, correctAnswer: 'A', reviewStatus: 'APPROVED' }
    questionForm.options = ['A', 'B', 'C', 'D'].map((key) => ({ optionKey: key, optionText: '' }))
  }
  if (!isObjective(questionForm.question.type)) {
    questionForm.options = []
  }
  questionDialog.value = true
}

function syncOptionsForType() {
  if (questionForm.question.type === 'JUDGE') {
    questionForm.options = [
      { optionKey: 'A', optionText: '正确' },
      { optionKey: 'B', optionText: '错误' }
    ]
    questionForm.question.correctAnswer = 'A'
  } else if (isObjective(questionForm.question.type)) {
    if (!questionForm.options.length) {
      questionForm.options = ['A', 'B', 'C', 'D'].map((key) => ({ optionKey: key, optionText: '' }))
    }
  } else {
    questionForm.options = []
    questionForm.question.correctAnswer = ''
  }
}

function addOption() {
  const key = String.fromCharCode(65 + questionForm.options.length)
  questionForm.options.push({ optionKey: key, optionText: '' })
}

async function saveQuestion() {
  await http.post('/teacher/questions', questionForm)
  ElMessage.success('题目已保存')
  questionDialog.value = false
  load()
}

async function reviewQuestion(row, status) {
  await http.post(`/teacher/questions/${row.id}/review`, { status })
  ElMessage.success('题目审核状态已更新')
  load()
}

async function removeQuestion(id) {
  await ElMessageBox.confirm('确认删除该题目吗？', '删除确认', { type: 'warning' })
  await http.delete(`/teacher/questions/${id}`)
  ElMessage.success('删除成功')
  load()
}

async function openPaper(row = null) {
  paperForm.selected = []
  Object.keys(paperScores).forEach((key) => delete paperScores[key])
  if (row) {
    paperForm.paper = { ...row }
    const links = await http.get(`/teacher/papers/${row.id}/questions`)
    paperForm.selected = links.map((q) => ({ questionId: q.questionId, score: q.score, sortNo: q.sortNo }))
    links.forEach((q) => (paperScores[q.questionId] = q.score))
  } else {
    paperForm.paper = { courseId: courses.value[0]?.id, title: '', durationMinutes: 45, published: 0, allowRetake: 0 }
  }
  paperDialog.value = true
}

function toggleQuestion(row) {
  const index = paperForm.selected.findIndex((item) => item.questionId === row.id)
  if (index >= 0) {
    paperForm.selected.splice(index, 1)
  } else {
    paperForm.selected.push({ questionId: row.id, score: paperScores[row.id] || 10, sortNo: paperForm.selected.length + 1 })
    paperScores[row.id] = paperScores[row.id] || 10
  }
}

async function savePaper() {
  await http.post('/teacher/papers', {
    paper: { ...paperForm.paper },
    questions: paperForm.selected.map((item, index) => ({
      questionId: item.questionId,
      score: paperScores[item.questionId] || item.score || 10,
      sortNo: index + 1
    }))
  })
  ElMessage.success('试卷已保存')
  paperDialog.value = false
  load()
}

async function submitManual(row) {
  const payload = manualGrades[row.answerId] || { score: row.manualScore ?? row.score ?? 0, comment: '' }
  await http.post(`/teacher/manual/${row.answerId}`, payload)
  ElMessage.success('已提交人工阅卷')
  load()
}

async function loadAnalysis() {
  analysisRows.value = await http.get('/teacher/analysis')
}

async function loadMonitor() {
  monitorRows.value = await http.get('/teacher/monitor')
}

async function loadAppeals() {
  appealRows.value = await http.get('/teacher/appeals')
}

async function reviewAppeal(row, status) {
  await http.post(`/teacher/appeals/${row.id}`, { status, reply: status === 'APPROVED' ? '同意复查申请' : '暂不支持复查' })
  ElMessage.success('申诉已处理')
  loadAppeals()
}

function triggerImportQuestions() {
  questionFileInput.value?.click()
}

async function handleQuestionImport(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  await http.post('/teacher/questions/import', formData)
  ElMessage.success('题库已导入')
  load()
}

async function loadDuplicateQuestions() {
  questionDuplicates.value = await http.get('/teacher/questions/duplicates')
  if (questionDuplicates.value.length === 0) {
    ElMessage.success('未发现重复题目')
  } else {
    ElMessage.warning(`发现 ${questionDuplicates.value.length} 组重复题目`)
  }
}

async function removePaper(id) {
  await ElMessageBox.confirm('确认删除该试卷吗？', '删除确认', { type: 'warning' })
  await http.delete(`/teacher/papers/${id}`)
  ElMessage.success('删除成功')
  load()
}

function courseName(id) {
  return courses.value.find((c) => c.id === id)?.name || `课程${id}`
}

function typeName(type) {
  return { SINGLE: '单选', MULTIPLE: '多选', JUDGE: '判断', SHORT: '简答', PROGRAM: '编程' }[type] || type
}

function questionStem(id) {
  return cleanGeneratedSuffix(questions.value.find((q) => q.id === id)?.stem || `题目 ${id}`)
}

function isObjective(type) {
  return ['SINGLE', 'MULTIPLE', 'JUDGE'].includes(type)
}

function reviewStatusName(status) {
  return formatReviewStatusName(status)
}

function exportQuestions() {
  exportXls('题库管理', filteredQuestions.value, [
    { label: 'ID', prop: 'id' },
    { label: '课程', formatter: (row) => courseName(row.courseId) },
    { label: '题型', formatter: (row) => typeName(row.type) },
    { label: '题干', formatter: (row) => cleanGeneratedSuffix(row.stem) },
    { label: '答案', prop: 'correctAnswer' },
    { label: '难度', prop: 'difficulty' }
  ])
}

function exportPapers() {
  exportXls('试卷管理', papers.value, [
    { label: '试卷', formatter: (row) => cleanGeneratedSuffix(row.title) },
    { label: '课程', formatter: (row) => courseName(row.courseId) },
    { label: '时长', prop: 'durationMinutes' },
    { label: '总分', prop: 'totalScore' },
    { label: '状态', formatter: (row) => row.published ? '已发布' : '草稿' }
  ])
}

function exportScores() {
  exportXls('成绩管理', filteredScores.value, [
    { label: '试卷', formatter: (row) => cleanGeneratedSuffix(row.paperTitle) },
    { label: '学生', prop: 'studentName' },
    { label: '成绩', prop: 'score' },
    { label: '状态', formatter: (row) => examStatusName(row.status) },
    { label: '提交时间', formatter: (row) => formatDateTime(row.submittedAt, '') }
  ])
}

onMounted(load)
watch(() => route.params.section, (section) => {
  active.value = section || 'questions'
})
</script>

<style scoped>
.route-tabs :deep(.el-tabs__header) {
  display: none;
}

.hidden-file {
  display: none;
}

.filter {
  max-width: 300px;
}

.el-select {
  width: 100%;
}

.option-list {
  width: 100%;
  display: grid;
  gap: 8px;
}

.option-row {
  display: grid;
  grid-template-columns: 70px 1fr 80px;
  gap: 8px;
}

.question-picker {
  margin-top: 12px;
}

.ai-layout {
  display: grid;
  grid-template-columns: minmax(320px, 420px) 1fr;
  gap: 14px;
  align-items: start;
}

.draft {
  border-top: 1px solid var(--line);
  padding: 14px 0;
}

.draft-input {
  margin: 8px 0;
}

.compact {
  margin-top: 8px;
}

.compact .option-row {
  grid-template-columns: 70px 1fr;
}

.ratio-grid {
  display: grid;
  grid-template-columns: minmax(140px, 1fr) 56px;
  gap: 8px;
  align-items: center;
  max-width: 460px;
}

.difficulty-grid {
  max-width: 360px;
}

.chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  margin-right: 6px;
  margin-bottom: 6px;
  border-radius: 999px;
  background: #eef8ff;
  color: #2563eb;
}

@media (max-width: 980px) {
  .ai-layout {
    grid-template-columns: 1fr;
  }

  .ratio-grid {
    grid-template-columns: repeat(2, minmax(90px, 1fr));
  }
}
</style>
