package com.augugrumi.spacerace.utility.gameutility;

import android.support.annotation.NonNull;
import android.util.SparseBooleanArray;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dpolonio on 24/11/17.
 */

public class ScoreCounter {

    private int score;
    private Map<LatLng, List<Score>> pointMap;

    private ScoreCounter(@NonNull Map<LatLng, List<Score>> pointMap, int score) {
        this.score = score;
        this.pointMap = pointMap;
    }

    public int getScore() {
        return score;
    }

    @NonNull
    public Map<LatLng, SparseBooleanArray> getAnswerCorrectnessMap () {
        Map<LatLng, SparseBooleanArray> res = new HashMap<>();

        for (LatLng id : pointMap.keySet()) {
            SparseBooleanArray isCorrectList = new SparseBooleanArray();

            for (Score score : pointMap.get(id)) {
                isCorrectList.put(score.getQuestionId(), score.isCorrect());
            }
        }
        return res;
    }

    private static class Score {

        private boolean isCorrect;
        private int questionId;

        private Score (int questionId) {
            this.questionId = questionId;
        }

        void setAnswer(String answer) {

            // TODO get the right answer, check if it's correct and in that case set isCorrect
        }

        boolean isCorrect () {
            return isCorrect;
        }

        int getQuestionId () {
            return questionId;
        }

        int getScore() {

            // 1 point for a correct answer
            // 0 points for a wrong answer
            return isCorrect ? 1 : 0;
        }
    }

    public static class Builder {

        private Map<LatLng, List<Score>> pointMap;
        private int score;

        public Builder() {
            pointMap = new HashMap<>();
        }

        @NonNull
        public Builder appendPOIQuestions(@NonNull LatLng POIId, @NonNull List<Integer> questions) {
            List<Score> questionsWithScore = new ArrayList<>();

            for (Integer i : questions) {
                questionsWithScore.add(
                        new Score(i)
                );
            }

            pointMap.put(POIId, questionsWithScore);
            return this;
        }

        @NonNull
        public Builder appendAnswer(@NonNull LatLng POIId, int questionId, @NonNull String answer) {
            pointMap.get(POIId).get(questionId).setAnswer(answer);
            score += pointMap.get(POIId).get(questionId).getScore();

            return this;
        }

        @NonNull
        public ScoreCounter build () {
            return new ScoreCounter(pointMap, score);
        }
    }
}
