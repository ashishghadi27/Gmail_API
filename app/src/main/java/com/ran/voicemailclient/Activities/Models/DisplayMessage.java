package com.ran.voicemailclient.Activities.Models;

public class DisplayMessage {
    private String sender, time, snippet;

    public DisplayMessage(String sender, String time, String snippet) {
        this.sender = sender;
        this.time = time;
        this.snippet = snippet;
    }

    public String getSender() {
        return sender;
    }

    public String getTime() {
        return time;
    }

    public String getSnippet() {
        return snippet;
    }
}
