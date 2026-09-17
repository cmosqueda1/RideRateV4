package com.chris.uberratebanner;

import android.content.Context;
import android.graphics.Rect;
import java.util.*;
import java.util.regex.*;

public final class OfferParser {
    private OfferParser(){}
    private static final Pattern MONEY = Pattern.compile("\\$(\\d{1,6}(?:,\\d{3})*(?:\\.\\d{1,2})?)");
    private static final Pattern MINUTES = Pattern.compile("(?i)(\\d{1,3})\\s*(?:minutes?|mins?)\\b");
    private static final Pattern HOURS = Pattern.compile("(?i)(\\d{1,2})\\s*(?:hours?|hrs?|h)\\b(?:\\s*(\\d{1,3})\\s*(?:minutes?|mins?|m)\\b)?");
    private static final Pattern COMPACT = Pattern.compile("(?i)(\\d{1,2})h\\s*(\\d{1,3})m\\b");
    private static final Pattern STOPS = Pattern.compile("(?i)\\b(\\d{1,2})\\s*stops?\\b");
    private static final int RIDE_STOP_BUFFER_MINUTES = 2;

    public static class Line {
        public final String text;
        public final Rect bounds;
        public Line(String t,Rect b){text=t==null?"":t.trim();bounds=b==null?null:new Rect(b);}
    }

    public static class ParsedOffer {
        public double payout,hourly;
        public int pickupMinutes,tripMinutes,totalMinutes,baseTotalMinutes,deliveryBufferMinutes;
        public int stopCount,stopBufferMinutes;
        public String type="Ride", rawText;
        public Rect cropRect;
    }

    public static ParsedOffer parseOffer(Context context,List<Line> input,int imageWidth,int imageHeight){
        if(input==null||input.isEmpty()) return null;
        List<Line> lines=new ArrayList<>();
        for(Line l:input) if(l!=null&&!l.text.isBlank()) lines.add(l);
        lines.sort(Comparator.comparingInt(a->a.bounds==null?Integer.MAX_VALUE:a.bounds.top));

        StringBuilder raw=new StringBuilder();
        for(Line l:lines){if(raw.length()>0)raw.append('\n');raw.append(l.text);}
        String all=raw.toString();
        String lower=all.toLowerCase(Locale.US);

        boolean hasTotalDuration=false;
        for(Line l:lines){
            if(l.text.toLowerCase(Locale.US).contains("total") && duration(l.text)!=null){hasTotalDuration=true;break;}
        }
        boolean delivery=lower.contains("delivery") || lower.contains("includes expected tip") || hasTotalDuration;
        boolean uberSignature=lower.contains("uber") || lower.contains("accept") || lower.contains("match") || delivery || lower.contains("exclusive") || lower.contains("share");
        if(!uberSignature) return null;

        Line payoutLine=null; Double payout=null; double best=-1;
        for(Line l:lines){
            String ll=l.text.toLowerCase(Locale.US);
            if(ll.contains("/hr")||ll.contains("per hour")||ll.contains("included")||ll.contains("perk")) continue;
            Matcher m=MONEY.matcher(l.text);
            while(m.find()){
                double v=parseMoney(m.group(1));
                if(v<1||v>1000)continue;
                double score=(l.bounds==null?1:Math.max(1,l.bounds.height()))*4.0;
                if(score>best){best=score;payout=v;payoutLine=l;}
            }
        }
        if(payout==null) return null;

        List<DurationHit> hits=new ArrayList<>();
        for(Line l:lines){
            if(payoutLine!=null&&l.bounds!=null&&payoutLine.bounds!=null&&l.bounds.centerY()<=payoutLine.bounds.centerY()) continue;
            Integer d=duration(l.text);
            if(d==null||d<=0||d>420) continue;
            String ll=l.text.toLowerCase(Locale.US);
            if(ll.contains("rating")||ll.contains("limit")||ll.contains("until")) continue;
            hits.add(new DurationHit(d,l,ll));
        }

        ParsedOffer out=new ParsedOffer();
        out.payout=payout; out.rawText=all; out.type=delivery?"Delivery":"Ride";
        Rect crop=payoutLine==null?null:payoutLine.bounds==null?null:new Rect(payoutLine.bounds);

        if(delivery){
            DurationHit totalHit=null;
            for(DurationHit h:hits) if(h.lower.contains("total")){totalHit=h;break;}
            if(totalHit==null && !hits.isEmpty()) totalHit=hits.get(0);
            if(totalHit==null) return null;
            out.baseTotalMinutes=totalHit.minutes;
            out.deliveryBufferMinutes=context==null?2:AppSettings.deliveryWaitBuffer(context);
            out.totalMinutes=out.baseTotalMinutes+out.deliveryBufferMinutes;
            out.pickupMinutes=0; out.tripMinutes=0;
            out.stopCount=0; out.stopBufferMinutes=0;
            crop=union(crop,totalHit.line.bounds);
        } else {
            if(hits.size()<2) return null;
            DurationHit first=hits.get(0), last=hits.get(hits.size()-1);
            // Prefer explicit pickup/away and trip/drop-off labels when Uber exposes them.
            for(DurationHit h:hits){
                if(h.lower.contains("away")||h.lower.contains("pickup")||h.lower.contains("pick up")){first=h;break;}
            }
            for(DurationHit h:hits){
                if(h==first)continue;
                if(h.lower.contains("trip")||h.lower.contains("dropoff")||h.lower.contains("drop off")||h.lower.contains("ride")){last=h;break;}
            }
            if(last==first){for(int i=hits.size()-1;i>=0;i--){if(hits.get(i)!=first){last=hits.get(i);break;}}}

            out.pickupMinutes=first.minutes;
            out.tripMinutes=last.minutes;
            out.stopCount=stopCount(all);
            out.stopBufferMinutes=out.stopCount*RIDE_STOP_BUFFER_MINUTES;
            out.baseTotalMinutes=out.pickupMinutes+out.tripMinutes;
            out.totalMinutes=out.baseTotalMinutes+out.stopBufferMinutes;
            crop=union(crop,first.line.bounds,last.line.bounds);
        }

        if(out.totalMinutes<=0||out.totalMinutes>480) return null;
        out.hourly=out.payout*60.0/out.totalMinutes;
        out.cropRect=crop;
        return out;
    }

    public static ParsedOffer parseTextOnly(Context context,String text){
        if(text==null)return null;
        List<Line> ls=new ArrayList<>();
        for(String s:text.split("\\n|\\|")) if(!s.trim().isEmpty())ls.add(new Line(s.trim(),null));
        return parseOffer(context,ls,0,0);
    }

    private static int stopCount(String s){
        Matcher m=STOPS.matcher(s==null?"":s);
        int max=0;
        while(m.find()) max=Math.max(max,safe(m.group(1)));
        return max;
    }
    private static double parseMoney(String s){
        try{String clean=s.replace(",",""); double v=Double.parseDouble(clean); if(!clean.contains(".")&&v>=250&&v<=99999) v/=100.0; return v;}catch(Exception e){return -1;}
    }
    private static Integer duration(String s){
        Matcher c=COMPACT.matcher(s); if(c.find())return safe(c.group(1))*60+safe(c.group(2));
        Matcher h=HOURS.matcher(s); if(h.find())return safe(h.group(1))*60+(h.group(2)==null?0:safe(h.group(2)));
        Matcher m=MINUTES.matcher(s); if(m.find())return safe(m.group(1));
        return null;
    }
    private static int safe(String s){try{return Integer.parseInt(s);}catch(Exception e){return 0;}}
    private static Rect union(Rect...rs){Rect out=null;for(Rect r:rs){if(r==null)continue;if(out==null)out=new Rect(r);else out.union(r);}return out;}
    private static class DurationHit{final int minutes;final Line line;final String lower;DurationHit(int m,Line l,String s){minutes=m;line=l;lower=s;}}
}
