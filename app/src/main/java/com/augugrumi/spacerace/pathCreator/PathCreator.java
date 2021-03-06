/**
* Copyright 2017 Davide Polonio <poloniodavide@gmail.com>, Federico Tavella
* <fede.fox16@gmail.com> and Marco Zanella <zanna0150@gmail.com>
* 
* This file is part of SpaceRace.
* 
* SpaceRace is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* SpaceRace is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with SpaceRace.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.augugrumi.spacerace.pathCreator;

import android.support.annotation.NonNull;
import android.util.Log;

import com.augugrumi.spacerace.utility.CoordinatesUtility;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonSyntaxException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import retrofit2.Call;
import retrofit2.Response;

import static com.augugrumi.spacerace.utility.Costants.HOP_MIN_NUM;
import static com.augugrumi.spacerace.utility.Costants.MAX_DISTANCE_FIRST_HOP;
import static com.augugrumi.spacerace.utility.Costants.MIN_DISTANCE_FIRST_HOP;
import static com.augugrumi.spacerace.utility.Costants.MODE;
import static com.augugrumi.spacerace.utility.Costants.MODE_DEBUG;

/**
 * Created by dpolonio on 15/11/17.
 */

public class PathCreator {

    public static class DistanceFrom {

        private LatLng start;
        private LatLng end;
        private double distance;

        DistanceFrom(@NonNull LatLng start, @NonNull LatLng end, double distance) {

            this.start = start;
            this.end = end;
            this.distance = distance;
        }

        public LatLng getStart() {
            return start;
        }
        public LatLng getEnd() {
            return end;
        }
        public double getDistance() {
            return distance;
        }

        @Override
        public String toString() {

            return "{" +
                    "start: [" +
                    start.latitude + "," +
                    start.longitude +
                    "]," +
                    "end: [" +
                    end.latitude + "," +
                    end.longitude +
                    "]," +
                    "distance: " + distance +
                    "}";
        }

        @NonNull
        String toJson() throws JSONException {

            JSONObject res = new JSONObject();

            JSONArray a1 = new JSONArray();

            a1.put(start.latitude);
            a1.put(start.longitude);

            JSONArray a2 = new JSONArray();

            a2.put(end.latitude);
            a2.put(end.longitude);



            res.put("start", a1);
            res.put("end", a2);
            res.put("distance", distance);

            return res.toString();
        }
    }

    /**
     * Max distance in meters
     */
    private double maxDistance = 5;
    /**
     * Min distance in meters
     */
    private double minDistance = 0.5;
    private LatLng initialPosition = null;

    public PathCreator(@NonNull LatLng initialPosition, double minDistance, double maxDistance) {

        this.initialPosition = initialPosition;

        if ((maxDistance >= 0 && minDistance >= 0) && maxDistance > minDistance) {
            this.minDistance = minDistance;
            this.maxDistance = maxDistance;
        }
    }

    public LatLng getInitialPosition() {
        return initialPosition;
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public double getMinDistance() {
        return minDistance;
    }

    private List<FutureTask<DistanceFrom>> calculateDistanceFromStart(@NonNull final LatLng start, @NonNull List<LatLng> points) {

        final String toMatch = "legs=[{distance={text=";
        List<FutureTask<DistanceFrom>> res = new ArrayList<>();
        final ExecutorService threadManager = Executors.newCachedThreadPool();
        final String init = start.latitude + "," + start.longitude;

        for (final LatLng destination : points) {

            final String dest = destination.latitude + "," + destination.longitude;
            final Call<Object> path = new PathRetrieval().getDirections(init, dest);

            FutureTask<DistanceFrom> task;
            if (MODE != MODE_DEBUG) {
                task = createTaskWithDistanceApi(start, destination, path, toMatch);
            } else {
                task = createTaskWithoutDistanceApi(start, destination);
            }
            res.add(task);
            threadManager.execute(task);
        }
        threadManager.shutdown();
        return res;
    }

    public Deque<DistanceFrom> generatePath() {

        List<FutureTask<DistanceFrom>> effectiveDistanceFromStart = calculateDistanceFromStart(
                initialPosition,
                getInRange(
                        initialPosition,
                        MIN_DISTANCE_FIRST_HOP,
                        MAX_DISTANCE_FIRST_HOP
                ));
        Collections.shuffle(effectiveDistanceFromStart); // Randomizing the points...

        for (FutureTask<DistanceFrom> distanceFromFutureTask : effectiveDistanceFromStart) {
            Deque<DistanceFrom> path = new ArrayDeque<>();
            try {
                DistanceFrom distance = distanceFromFutureTask.get();
                Map<LatLng, Boolean> visitedTable = new HashMap<>();

                visitedTable.put(distance.end, true);

                path.addLast(distance);
                path.addAll(pathChooser(distance,
                        maxDistance - (distance.distance/1000),
                        visitedTable,
                        5));
                if (path.size() >= HOP_MIN_NUM) {
                    return path;
                }
            } catch (InterruptedException | ExecutionException  e) {
                e.printStackTrace();
            }
        }
        return new ArrayDeque<>();
    }

    private List<LatLng> getInRange(@NonNull LatLng pos, double min, double max) {
        List<LatLng> buildingsPositions = new ArrayList<>();
        for (LatLng position : PositionsLoader.getPositions()) {
            double distance = CoordinatesUtility.distance(
                    position.latitude,
                    position.longitude,
                    pos.latitude,
                    pos.longitude);

            Log.d("DISTANCE", "distance between " + position.toString() + " and " + pos.toString() + "= " + distance);

            if (distance <= max && distance >= min) {

                Log.d("POS_FINDER_MATCH", "ADDING CANDIDATE: " + position.toString());
                buildingsPositions.add(position);
            }
        }
        return buildingsPositions;
    }

    private Deque<DistanceFrom> pathChooser(@NonNull DistanceFrom d,
                                            double remainingDistance,
                                            @NonNull Map<LatLng, Boolean> visitedTable,
                                            int maxNodes)
            throws ExecutionException, InterruptedException {
        Deque<DistanceFrom> res = new ArrayDeque<>();

        if (maxNodes == 0) {
            return res;
        }
        if (remainingDistance >= minDistance) {

            List<FutureTask<DistanceFrom>> effectiveDistance = calculateDistanceFromStart(
                    d.end,
                    getInRange(d.end, MIN_DISTANCE_FIRST_HOP, MAX_DISTANCE_FIRST_HOP));
            Collections.shuffle(effectiveDistance); // Randomizing the points...

            Log.d("PATH_CHOOSER", "effectiveDistance size: " + effectiveDistance.size());

            for (FutureTask<DistanceFrom> d2 : effectiveDistance) {
                DistanceFrom calculation = d2.get();
                if (calculation.start == d.end &&
                        calculation.start != d.start &&
                        calculation.distance >= 0 &&
                        (calculation.distance/1000) <= remainingDistance &&
                        visitedTable.get(calculation.end) == null) {

                    Log.d("PATH_CHOOSER", "adding new node into the list");

                    visitedTable.put(calculation.end, true);

                    res.addLast(calculation);
                    res.addAll(pathChooser(calculation,
                            remainingDistance - (calculation.distance/1000),
                            visitedTable,
                            maxNodes - 1));
                    return res;
                }
            }
        }
        return res;
    }

    private FutureTask<DistanceFrom> createTaskWithoutDistanceApi
                                            (final LatLng start, final LatLng destination){
        return new FutureTask<>(new Callable<DistanceFrom>() {
            @Override
            public DistanceFrom call() throws Exception {
                return new DistanceFrom(start, destination,
                        CoordinatesUtility.distance(start.latitude, start.longitude,
                                destination.latitude, destination.longitude));
            }
        });
    }

    private FutureTask<DistanceFrom> createTaskWithDistanceApi
            (final LatLng start, final LatLng destination, final Call<Object> path, final String toMatch){
        return new FutureTask<>(new Callable<DistanceFrom>() {
            @Override
            public DistanceFrom call() throws Exception {
                Response<Object> response = path.execute();
                DistanceFrom toReturn;
                try {

                    Log.d("POS_FINDER", "ORIGINAL JSON: " + response.body().toString());

                    int pos = response.body().toString().indexOf(toMatch);
                    if (pos >= 0) {

                        pos += toMatch.length();

                        String semiSanitized = response.body().toString().substring(pos, pos + 10);
                        pos = semiSanitized.indexOf('m');
                        String sanitized = semiSanitized.substring(0, pos + 1);
                        sanitized = sanitized.trim();
                        sanitized = sanitized.replaceAll(" ", "");

                        double distance = -1;
                        if (sanitized.indexOf("km") != 0) {
                            Log.d("POS_FINDER", "DISTANCE IN KM");

                            distance = Double.parseDouble(sanitized.substring(0, sanitized.length() - 2));

                            Log.d("POS_FINDER", "Res in kilometers is: " + distance);
                            distance *= 1000; // We need the distance in meters
                        } else if (sanitized.indexOf('m') != 0) {
                            Log.d("POS_FINDER", "DISTANCE IN METERS");

                            distance = Double.parseDouble(sanitized.substring(0, sanitized.length() - 1));

                            Log.d("POS_FINDER", "Res in meters is: " + distance);
                        }

                        return new DistanceFrom(start, destination, distance);

                    }
                    toReturn = new DistanceFrom(start, destination, -1);
                } catch (Exception e) {
                    e.printStackTrace();
                    toReturn = new DistanceFrom(start, destination,
                                CoordinatesUtility.distance(start.latitude, start.longitude,
                                destination.latitude, destination.longitude));
                }
                return toReturn;
            }
        });
    }
}
