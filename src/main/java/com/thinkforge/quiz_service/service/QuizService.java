package com.thinkforge.quiz_service.service;

import com.thinkforge.quiz_service.dto.*;
import com.thinkforge.quiz_service.entity.*;
import com.thinkforge.quiz_service.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class QuizService {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuizStudentEvaluationRepository quizStudentEvaluationRepository;

    @Autowired
    private QuizSubmissionRepository quizSubmissionRepository;

    @Autowired
    private QuestionService questionService;

    public List<QuizDTO> getAllQuiz() {

        List<Quiz> quizList = quizRepository.findAll();

        return quizList.stream()
                .map(QuizService::QuiztoQuizDTO)
                .toList();

    }

    public QuizDTO getQuizByQuizId(UUID quizId) {

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Cannot find Quiz with quiz id: " + quizId));

        return QuiztoQuizDTO(quiz);

    }

    public List<QuizDTO> getQuizByTeacherId(UUID teacherId) {

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found with teacher id: " + teacherId));

        List<Quiz> quizList = quizRepository.findByCreatedBy(teacher);

        return quizList.stream()
                .map(QuizService::QuiztoQuizDTO)
                .toList();

    }

    public QuizDTO updateQuiz(UUID quizId, UpdateQuizRequestDTO request) {

        System.out.println(request.getGrade());

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Cannot find Quiz with quiz id: " + quizId));

        quiz.setUpdatedAt(Timestamp.from(Instant.now()));
        quiz.setGrade(request.getGrade());
        quiz.setSubject(request.getSubject());
        quiz.setTopic(request.getTopic());
        quiz.setDeadline(request.getDeadline());

        return QuiztoQuizDTO(quizRepository.save(quiz));

    }

    public void deleteQuiz(UUID quizId) {

        quizRepository.deleteById(quizId);

    }

    public void submitQuiz(UUID quizId, QuizSubmissionRequest request) {

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Cannot find Quiz with quiz id: " + quizId));

        UUID studentId = request.getStudentId();

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Cannot find student with student id: " +  studentId));

        int maxScore = 0, score = 0;

        for(AnswerDTO answer: request.getAnswers()) {

            Question question = questionRepository.findById(answer.getQuestionId())
                    .orElseThrow(() -> new RuntimeException("Cannot find Question with question id: " + answer.getQuestionId()));

            maxScore++;
            if(Objects.equals(answer.getSelectedOption(), question.getCorrectOption())) score++;

            QuizStudentEvaluation evaluation = new QuizStudentEvaluation();
            evaluation.setStudentId(studentId);
            evaluation.setQuiz(quiz);
            evaluation.setQuestion(question);
            evaluation.setSelectedOption(answer.getSelectedOption());
            evaluation.setSubmittedAt(Timestamp.from(Instant.now()));

            quizStudentEvaluationRepository.save(evaluation);

        }

        QuizSubmission quizSubmission = new QuizSubmission();
        quizSubmission.setQuiz(quiz);
        quizSubmission.setStudent(student);
        quizSubmission.setScore(score);
        quizSubmission.setMaxScore(maxScore);
        quizSubmission.setSubmittedAt(Timestamp.from(Instant.now()));

        quizSubmissionRepository.save(quizSubmission);

    }

    public static QuizDTO QuiztoQuizDTO(Quiz quiz) {
        QuizDTO dto = new QuizDTO();
        dto.setQuizId(quiz.getQuizId());
        dto.setCreatedBy(quiz.getCreatedBy().getTeacherId());
        dto.setCreatedAt(quiz.getCreatedAt());
        dto.setUpdatedAt(quiz.getUpdatedAt());
        dto.setSubject(quiz.getSubject());
        dto.setTopic(quiz.getTopic());
        dto.setGrade(quiz.getGrade());
        dto.setDeadline(quiz.getDeadline());
        return dto;
    }


    public List<GetQuestionsDTO> generateQuiz(CreateQuizRequestDTO request) {

        UUID teacherId = request.getTeacherId();
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found with teacher id: " + teacherId));

        Quiz quiz = new Quiz();
        quiz.setCreatedBy(teacher);
        quiz.setCreatedAt(Timestamp.from(Instant.now()));
        quiz.setUpdatedAt(Timestamp.from(Instant.now()));
        quiz.setSubject(request.getSubject());
        quiz.setTopic(request.getTopic());
        quiz.setGrade(request.getGrade());
        quiz.setDeadline(request.getDeadline());

        Quiz response = quizRepository.save(quiz);

        List<GetQuestionsDTO> questions = questionService.generateQuizQuestion(request.getGrade().toString(), request.getSubject(), request.getTopic(), request.getNumOfQuestions());

        for(GetQuestionsDTO question: questions) {

            Question q = new Question();
            q.setQuiz(quiz);
            q.setQuestionText(question.getQuestionText());
            q.setMarks(question.getMarks());
            q.setNegativeMarks(question.getNegativeMarks());
            q.setOptionA(question.getOptionA());
            q.setOptionB(question.getOptionB());
            q.setOptionC(question.getOptionC());
            q.setOptionD(question.getOptionD());
            q.setCorrectOption(question.getCorrectOption());
            q.setHint(question.getHint());

            questionRepository.save(q);
        }

        return questions;

    }

    public QuizAnalysisByQuizIdResponseDTO getQuizAnalysisByQuiz(UUID quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found with quizId: " + quizId));

        List<QuizSubmission> submissions = quizSubmissionRepository.findAllByQuiz(quiz);

        QuizAnalysisByQuizIdResponseDTO response = new QuizAnalysisByQuizIdResponseDTO();
        response.setQuizId(quizId);

        List<QuizAnalysisByQuizIdDTO> studentData = new ArrayList<>();

        for (QuizSubmission submission : submissions) {
            QuizAnalysisByQuizIdDTO dto = new QuizAnalysisByQuizIdDTO();
            dto.setStudentId(submission.getStudent().getStudentId());
            dto.setObtainedScore(submission.getScore());
            dto.setMaxScore(submission.getMaxScore());

            List<QuizAnalysisByQuizIdQuestionDTO> questionData = new ArrayList<>();
            List<QuizStudentEvaluation> evaluations = quizStudentEvaluationRepository
                    .findAllByStudentIdAndQuiz(submission.getStudent().getStudentId(), quiz);

            for (QuizStudentEvaluation evaluation : evaluations) {
                QuizAnalysisByQuizIdQuestionDTO questionDTO = new QuizAnalysisByQuizIdQuestionDTO();
                questionDTO.setQuestionId(evaluation.getQuestion().getQuestionId());
                questionDTO.setOptionA(evaluation.getQuestion().getOptionA());
                questionDTO.setOptionB(evaluation.getQuestion().getOptionB());
                questionDTO.setOptionC(evaluation.getQuestion().getOptionC());
                questionDTO.setOptionD(evaluation.getQuestion().getOptionD());
                questionDTO.setSelectedOption(evaluation.getSelectedOption());
                questionDTO.setCorrectOption(evaluation.getQuestion().getCorrectOption());

                questionData.add(questionDTO);
            }

            dto.setQuestionData(questionData);
            studentData.add(dto);
        }

        response.setStudentData(studentData);
        return response;
    }

}
