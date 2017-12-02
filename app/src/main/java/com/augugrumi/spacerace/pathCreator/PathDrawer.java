package com.augugrumi.spacerace.pathCreator;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;

import com.augugrumi.spacerace.R;
import com.augugrumi.spacerace.SpaceRace;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Deque;

import static com.augugrumi.spacerace.utility.Costants.DEFAULT_PIECE_DISPLAYED;
import static com.augugrumi.spacerace.utility.Costants.ICON_DIMENSION;

/**
 * Created by davide on 19/11/17.
 */

public class PathDrawer {

    private GoogleMap map;
    private Deque<PathCreator.DistanceFrom> path;
    private BitmapDescriptor firstNodeIcon;
    private BitmapDescriptor middleNodeIcon;
    private BitmapDescriptor lastNodeIcon;
    private PathCreator.DistanceFrom last;

    private boolean isFirstPop;

    private PathDrawer(GoogleMap map,
                       Deque<PathCreator.DistanceFrom> path,
                       BitmapDescriptor firstNodeIcon,
                       BitmapDescriptor middleNodeIcon,
                       BitmapDescriptor lastNodeIcon) {

        this.map = map;
        this.path = path;
        last = path.getFirst();
        this.firstNodeIcon = firstNodeIcon;
        this.middleNodeIcon = middleNodeIcon;
        this.lastNodeIcon = lastNodeIcon;
        this.isFirstPop = true;
    }

    public boolean hasNext() {

        return path.size() != 0;
    }

    public Marker drawNext() {

        MarkerOptions options = new MarkerOptions();

        if (isFirstPop) {

            options.position(path.getFirst().getStart());
            options.icon(firstNodeIcon);
            isFirstPop = false;
        } else {

            if (path.size() == 0) {
                options.position(last.getEnd());
                options.icon(lastNodeIcon);
            } else {
                options.position(path.getFirst().getEnd());
                options.icon(middleNodeIcon);
            }
        }

        if (path.size() > 0)
            last = path.getFirst();

        return map.addMarker(options);
    }

    public static class Builder {

        private GoogleMap map;
        private Deque<PathCreator.DistanceFrom> path;
        private BitmapDescriptor firstNodeIcon;
        private BitmapDescriptor middleNodeIcon;
        private BitmapDescriptor lastNodeIcon;

        public Builder() {

            Bitmap toScale = BitmapFactory.decodeResource(
                    SpaceRace.getAppContext().getResources(),
                    DEFAULT_PIECE_DISPLAYED);
            toScale = Bitmap.createScaledBitmap(toScale,
                    ICON_DIMENSION,
                    ICON_DIMENSION,
                    false);

            BitmapDescriptor defaultIcon = BitmapDescriptorFactory.fromBitmap(toScale);

            this.firstNodeIcon = defaultIcon;
            this.middleNodeIcon = defaultIcon;
            this.lastNodeIcon = defaultIcon;
            this.map = null;
            this.path = null;
        }

        public Builder setMap(@NonNull GoogleMap map) {

            this.map = map;

            return this;
        }

        public Builder setPath(@NonNull Deque<PathCreator.DistanceFrom> path) {

            this.path = path;

            return this;
        }

        public Builder setStartIcon(@NonNull BitmapDescriptor icon) {

            this.firstNodeIcon = icon;

            return this;
        }

        public Builder setMiddleIcon(@NonNull BitmapDescriptor icon) {

            this.middleNodeIcon = icon;

            return this;
        }

        public Builder setEndIcon(@NonNull BitmapDescriptor icon) {

            this.lastNodeIcon = icon;

            return this;
        }

        public PathDrawer build() {

            if (map != null && path != null){

                return new PathDrawer(
                        map,
                        path,
                        firstNodeIcon,
                        middleNodeIcon,
                        lastNodeIcon
                );
            }

            return null;
        }
    }
}
