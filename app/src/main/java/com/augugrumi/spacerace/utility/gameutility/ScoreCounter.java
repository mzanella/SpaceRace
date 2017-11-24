package com.augugrumi.spacerace.utility.gameutility;

import android.support.annotation.NonNull;
import android.util.SparseArray;
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

        void setAnswer(int answer) {

            // TODO get the right answer, check if it's correct and in that case set isCorrect
        }

        boolean isCorrect () {
            return isCorrect;
        }

        int getQuestionId () {
            return questionId;
        }

        int getScore() {

            // 1 point for correct answer
            // 0 point for wrong answer
            return isCorrect ? 1 : 0;
        }
    }

    public static class Builder {

        private Map<LatLng, List<Score>> pointMap;
        private int score;

        public Builder() {
            pointMap = new HashMap<>();
        }

        public Builder appendPOIQuestions(LatLng POIId, @NonNull List<Integer> questions) {
            List<Score> questionsWithScore = new ArrayList<>();

            for (Integer i : questions) {
                questionsWithScore.add(
                        new Score(i)
                );
            }

            pointMap.put(POIId, questionsWithScore);
            return this;
        }

        public Builder appendAnswer(LatLng POIId, int questionId, int answer) {
            pointMap.get(POIId).get(questionId).setAnswer(answer);
            score += pointMap.get(POIId).get(questionId).getScore();

            return this;
        }

        public ScoreCounter build () {
            return new ScoreCounter(pointMap, score);
        }
    }
}
