package com.kahootp46.live.auth;

public enum AccessRole {
    /** Quiz owner: manages quizzes and hosts games. */
    ADMIN,
    /** Anonymous read-only viewer bound to a single game PIN. */
    VIEWER
}
