package com.shiznatix.lightclubs;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import com.shiznatix.lightclubs.entities.ScriptFrame;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

class ScriptFileParser {
    private static final String LOG_TAG = "JL_" + ScriptFileParser.class.getName();

    private InputStream mInputStream;

    ScriptFileParser(ContentResolver contentResolver, Uri uri) {
        try {
            mInputStream = contentResolver.openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    ScriptFileParser(InputStream inputStream) {
        mInputStream = inputStream;
    }

    Map<String, ArrayList<ScriptFrame>> getPlaylists() {
        Map<String, TreeMap<Integer, String>> playlistsMap = new HashMap<>();

        try {
            XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
            XmlPullParser parser = xmlFactoryObject.newPullParser();
            parser.setInput(mInputStream, null);

            int event = parser.getEventType();

            while (event != XmlPullParser.END_DOCUMENT) {
                String name = parser.getName();

                switch (event) {
                    case XmlPullParser.START_TAG:
                        if (name.equals("track")) {
                            Map<String, TreeMap<Integer, String>> playlist = parseTagTrack(parser);

                            if (null != playlist) {
                                playlistsMap.putAll(playlist);
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }

                event = parser.next();
            }
        } catch (XmlPullParserException | IOException | NullPointerException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, e.getMessage());
        }

        Map<String, ArrayList<ScriptFrame>> playlists = new HashMap<>();

        // start gross code

        // clubs
        for (int i = 1; i < 4; i++) {
            ArrayList<ScriptFrame> clubFrames = new ArrayList<>();

            // get the last item from each led set
            NavigableMap<Integer, String> ledSet1Map;
            NavigableMap<Integer, String> ledSet2Map;
            NavigableMap<Integer, String> ledSet3Map;
            NavigableMap<Integer, String> ledSet4Map;

            ledSet1Map = playlistsMap.get("C" + i + "-1");
            ledSet2Map = playlistsMap.get("C" + i + "-2");
            ledSet3Map = playlistsMap.get("C" + i + "-3");
            ledSet4Map = playlistsMap.get("C" + i + "-4");

            Integer club1LastTime = ledSet1Map.lastEntry().getKey();
            Integer club2LastTime = ledSet2Map.lastEntry().getKey();
            Integer club3LastTime = ledSet3Map.lastEntry().getKey();
            Integer club4LastTime = ledSet4Map.lastEntry().getKey();

            // get the largest number of these - this is the last frame number
            int lastTime = Collections.max(Arrays.asList(club1LastTime, club2LastTime, club3LastTime, club4LastTime));

            // ensure the first item of each map is key 0
            if (0 != ledSet1Map.firstEntry().getKey()) {
                ledSet1Map.put(0, "0,0,0");
            }
            if (0 != ledSet2Map.firstEntry().getKey()) {
                ledSet2Map.put(0, "0,0,0");
            }
            if (0 != ledSet3Map.firstEntry().getKey()) {
                ledSet3Map.put(0, "0,0,0");
            }
            if (0 != ledSet4Map.firstEntry().getKey()) {
                ledSet4Map.put(0, "0,0,0");
            }

            // hold the current and next values for each led set
            Iterator itLedSet1 = playlistsMap.get("C" + i + "-1").entrySet().iterator();
            Iterator itLedSet2 = playlistsMap.get("C" + i + "-2").entrySet().iterator();
            Iterator itLedSet3 = playlistsMap.get("C" + i + "-3").entrySet().iterator();
            Iterator itLedSet4 = playlistsMap.get("C" + i + "-4").entrySet().iterator();

            Map.Entry ledSet1Current = (Map.Entry)itLedSet1.next();
            Map.Entry ledSet2Current = (Map.Entry)itLedSet2.next();
            Map.Entry ledSet3Current = (Map.Entry)itLedSet3.next();
            Map.Entry ledSet4Current = (Map.Entry)itLedSet4.next();

            Map.Entry ledSet1Next = itLedSet1.hasNext() ? (Map.Entry)itLedSet1.next() : ledSet1Current;
            Map.Entry ledSet2Next = itLedSet2.hasNext() ? (Map.Entry)itLedSet2.next() : ledSet2Current;
            Map.Entry ledSet3Next = itLedSet3.hasNext() ? (Map.Entry)itLedSet3.next() : ledSet3Current;
            Map.Entry ledSet4Next = itLedSet4.hasNext() ? (Map.Entry)itLedSet4.next() : ledSet4Current;

            // loop from 0 to this last time number
            for (int y = 0; y <= lastTime; y++) {
                // if next value of led set is equal to the y, shift our current/next to the right 1
                if (y == (Integer)ledSet1Next.getKey()) {
                    ledSet1Current = ledSet1Next;
                    ledSet1Next = itLedSet1.hasNext() ? (Map.Entry)itLedSet1.next() : ledSet1Current;
                }
                if (y == (Integer)ledSet2Next.getKey()) {
                    ledSet2Current = ledSet2Next;
                    ledSet2Next = itLedSet2.hasNext() ? (Map.Entry)itLedSet2.next() : ledSet2Current;
                }
                if (y == (Integer)ledSet3Next.getKey()) {
                    ledSet3Current = ledSet3Next;
                    ledSet3Next = itLedSet3.hasNext() ? (Map.Entry)itLedSet3.next() : ledSet3Current;
                }
                if (y == (Integer)ledSet4Next.getKey()) {
                    ledSet4Current = ledSet4Next;
                    ledSet4Next = itLedSet4.hasNext() ? (Map.Entry)itLedSet4.next() : ledSet4Current;
                }

                String message = (String)ledSet1Current.getValue();
                message = message + ":" + ledSet2Current.getValue();
                message = message + ":" + ledSet3Current.getValue();
                message = message + ":" + ledSet4Current.getValue();

                clubFrames.add(new ScriptFrame(y, message));
            }

            playlists.put("C" + i, clubFrames);
        }

        // goggles
        ArrayList<ScriptFrame> goggleFrames = new ArrayList<>();

        // get the last item from each led set
        NavigableMap<Integer, String> goggles1Map;
        NavigableMap<Integer, String> goggles2Map;

        goggles1Map = playlistsMap.get("G-1");
        goggles2Map = playlistsMap.get("G-2");

        Integer goggles1LastTime = goggles1Map.lastEntry().getKey();
        Integer goggles2LastTime = goggles2Map.lastEntry().getKey();

        // get the largest number of these - this is the last frame number
        int lastTime = Collections.max(Arrays.asList(goggles1LastTime, goggles2LastTime));

        // ensure the first item of each map is key 0
        if (0 != goggles1Map.firstEntry().getKey()) {
            goggles1Map.put(0, "0,0,0");
        }
        if (0 != goggles2Map.firstEntry().getKey()) {
            goggles2Map.put(0, "0,0,0");
        }

        // hold the current and next values for each led set
        Iterator itGoggles1 = playlistsMap.get("G-1").entrySet().iterator();
        Iterator itGoggles2 = playlistsMap.get("G-2").entrySet().iterator();

        Map.Entry goggles1Current = (Map.Entry)itGoggles1.next();
        Map.Entry goggles2Current = (Map.Entry)itGoggles2.next();

        Map.Entry goggles1Next = itGoggles1.hasNext() ? (Map.Entry)itGoggles1.next() : goggles1Current;
        Map.Entry goggles2Next = itGoggles2.hasNext() ? (Map.Entry)itGoggles2.next() : goggles2Current;

        // loop from 0 to this last time number
        for (int y = 0; y <= lastTime; y++) {
            // if next value of led set is equal to the y, shift our current/next to the right 1
            if (y == (Integer)goggles1Next.getKey()) {
                goggles1Current = goggles1Next;
                goggles1Next = itGoggles1.hasNext() ? (Map.Entry)itGoggles1.next() : goggles1Current;
            }
            if (y == (Integer)goggles2Next.getKey()) {
                goggles2Current = goggles2Next;
                goggles2Next = itGoggles2.hasNext() ? (Map.Entry)itGoggles2.next() : goggles2Current;
            }

            String message = (String)goggles1Current.getValue();
            message = message + ":" + goggles2Current.getValue();

            goggleFrames.add(new ScriptFrame(y, message));
        }

        playlists.put("G", goggleFrames);

        return playlists;
    }

    private Map<String, TreeMap<Integer, String>> parseTagTrack(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "track");

        TreeMap<Integer, String> frames = new TreeMap<>();
        Map<String, TreeMap<Integer, String>> deviceFrames = new HashMap<>();
        String key = "";

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String name = parser.getName();

            switch (name) {
                case "name":
                    key = readTagText(parser);
                    break;
                case "process":
                    frames = parseTagProcess(parser);
                    break;
                default:
                    skipTag(parser);
            }
        }

        if (key.equals("")) {
            return null;
        }

        deviceFrames.put(key, frames);

        return deviceFrames;
    }

    private TreeMap<Integer, String> parseTagProcess(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "process");

        TreeMap<Integer, String> frames = new TreeMap<>();

        while (true) {
            parser.next();

            int eventType = parser.getEventType();
            String name = parser.getName();

            if (null == name) {
                continue;
            }

            if (XmlPullParser.END_TAG == eventType && name.equals("process")) {
                break;
            }

            if (!name.startsWith("f")) {
                continue;
            }

            Integer frameTime = Integer.parseInt(name.substring(1));

            if (frameTime < 0) {
                continue;
            }

            String[] colorParts = readTagText(parser).split(",");

            frames.put(frameTime, colorParts[0] + "," + colorParts[1] + "," + colorParts[2]);
        }

        return frames;
    }

    private String readTagText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";

        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }

        return result;
    }

    private void skipTag(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }

        int depth = 1;

        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    void closeStream() {
        // close our stream
        try {
            mInputStream.close();
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }
}
