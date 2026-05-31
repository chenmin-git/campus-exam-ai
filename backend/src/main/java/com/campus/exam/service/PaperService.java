package com.campus.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.exam.dto.PaperDto;
import com.campus.exam.dto.PaperQuestionRequest;
import com.campus.exam.entity.Paper;
import com.campus.exam.entity.PaperQuestion;
import com.campus.exam.entity.User;
import com.campus.exam.mapper.PaperMapper;
import com.campus.exam.mapper.PaperQuestionMapper;
import com.campus.exam.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaperService {
    private final PaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;

    public List<Paper> list(Boolean published, User current) {
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<Paper>().orderByDesc(Paper::getId);
        if (published != null) {
            wrapper.eq(Paper::getPublished, published ? 1 : 0);
        }
        if (current != null && "TEACHER".equals(current.getRole())) {
            wrapper.eq(Paper::getCreatorId, current.getId());
        }
        return paperMapper.selectList(wrapper);
    }

    public List<PaperQuestion> questions(Long paperId) {
        return paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paperId)
                .orderByAsc(PaperQuestion::getSortNo));
    }

    @Transactional
    public PaperDto save(PaperDto dto, Long creatorId) {
        Paper paper = dto.paper();
        if (paper.getTitle() == null || paper.getTitle().isBlank() || paper.getCourseId() == null) {
            throw new IllegalArgumentException("试卷名称和课程不能为空");
        }
        paper.setCreatorId(creatorId);
        paper.setCreatedAt(paper.getCreatedAt() == null ? LocalDateTime.now() : paper.getCreatedAt());
        int total = dto.questions() == null ? 0 : dto.questions().stream().mapToInt(q -> q.score() == null ? 0 : q.score()).sum();
        paper.setTotalScore(total);
        if (paper.getPublished() == null) {
            paper.setPublished(0);
        }
        if (paper.getAllowRetake() == null) {
            paper.setAllowRetake(0);
        }
        if (paper.getStartTime() != null && paper.getEndTime() != null && paper.getEndTime().isBefore(paper.getStartTime())) {
            throw new IllegalArgumentException("考试结束时间不能早于开始时间");
        }
        if (paper.getId() == null) {
            paperMapper.insert(paper);
        } else {
            paperMapper.updateById(paper);
            paperQuestionMapper.delete(new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getPaperId, paper.getId()));
        }
        if (dto.questions() != null) {
            int index = 1;
            for (PaperQuestionRequest item : dto.questions()) {
                PaperQuestion pq = new PaperQuestion();
                pq.setPaperId(paper.getId());
                pq.setQuestionId(item.questionId());
                pq.setScore(item.score() == null ? 0 : item.score());
                pq.setSortNo(item.sortNo() == null ? index : item.sortNo());
                paperQuestionMapper.insert(pq);
                index++;
            }
        }
        if (Integer.valueOf(1).equals(paper.getPublished())) {
            notificationService.create(null, "STUDENT", "考试发布通知", "《" + paper.getTitle() + "》已发布，请在规定时间内参加考试。");
        }
        operationLogService.log("保存试卷", "paper:" + paper.getId(), paper.getTitle());
        return new PaperDto(paper, dto.questions());
    }

    public void delete(Long id) {
        paperQuestionMapper.delete(new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getPaperId, id));
        paperMapper.deleteById(id);
        operationLogService.log("删除试卷", "paper:" + id, "删除试卷及题目关系");
    }
}
