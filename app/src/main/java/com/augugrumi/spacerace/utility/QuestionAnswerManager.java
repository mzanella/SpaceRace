package com.augugrumi.spacerace.utility;

import android.content.res.Resources;
import android.util.Log;
import android.widget.ListView;

import com.augugrumi.spacerace.R;
import com.augugrumi.spacerace.SpaceRace;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Marco Zanella
 * @version 0.01
 *          date 24/11/17
 */

public class QuestionAnswerManager {

    private static final String LAT = "lat";
    private static final String LNG = "lng";
    private static final String TITLE = "title";
    private static final String NAME = "name";
    private static final String CARD = "card";
    private static final String Q1 = "Question1";
    private static final String Q2 = "Question2";
    private static final String Q3 = "Question3";
    private static final String A11 = "Answer11";
    private static final String A12 = "Answer12";
    private static final String A13 = "Answer13";
    private static final String A21 = "Answer21";
    private static final String A22 = "Answer22";
    private static final String A23 = "Answer23";
    private static final String A31 = "Answer31";
    private static final String A32 = "Answer32";
    private static final String A33 = "Answer33";

    private static Map<LatLng, JSONObject> qa;

    static {
        qa = new HashMap<>();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(SpaceRace.
                getAppContext().getResources().
                openRawResource(R.raw.qa)));
        String line = null;
        StringBuilder builder = new StringBuilder();
        try {
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        line = builder.toString();
        if (!line.isEmpty()) {
            try {
                JSONArray array = new JSONArray(line);
                JSONObject obj;
                double lat;
                double lng;
                for (int i = 0; i < array.length(); i++) {
                    obj = array.getJSONObject(i);
                    lat = obj.getDouble(LAT);
                    lng = obj.getDouble(LNG);
                    qa.put(new LatLng(lat, lng), obj);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private QuestionAnswerManager(){}

    public static List<String> getQuestions(LatLng latLng) {
        List<String> questions = new ArrayList<>();

        JSONObject obj = qa.get(latLng);

        try {
            questions.add(obj.getString(Q1));
            questions.add(obj.getString(Q2));
            questions.add(obj.getString(Q3));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return questions;
    }

    public static String getRightAnswer(LatLng latLng, int questionId) {

        JSONObject obj = qa.get(latLng);
        String key1 = "Answer" + questionId + "1";
        String answer = "";

        try {
            answer = obj.getString(key1);
            } catch (JSONException e) {
            e.printStackTrace();
        }

        return answer;
    }

    public static QuestionAnswerManager.QuestionAnswers getQuestionAnswers(LatLng latLng, int qaId) {

        JSONObject obj = qa.get(latLng);
        String questionKey = "Question" + qaId;

        String question = "";
        List<String> answers = new ArrayList<>();
        String answerKey = "Answer" + qaId;

        try {
            question = obj.getString(questionKey);
            for (int i = 1; i <= 3; i++) {
                answers.add(obj.getString(answerKey + i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return new QuestionAnswers(question, answers);
    }

    public static String getTitle(LatLng latLng) {
        String toReturn = "";
        try {
            JSONObject obj = qa.get(latLng);
            toReturn = obj.getString(TITLE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return toReturn;
    }

    public static String getCard(LatLng latLng) {
        String toReturn = "";
        try {
            JSONObject obj = qa.get(latLng);
            toReturn = obj.getString(CARD);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return toReturn;
    }

    public static class QuestionAnswers {
        private String question;
        private List<String> answers;

        private QuestionAnswers(String question, List<String> answers) {
            this.question = question;
            this.answers = new ArrayList<String>(answers);
        }

        public String getQuestion(){
            return question;
        }

        public List<String> getAnswers() {
            return new ArrayList<>(answers);
        }
    }
}