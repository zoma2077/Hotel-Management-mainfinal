package com.cse241.hotel.ui;

import com.cse241.hotel.model.user.Guest;
import com.cse241.hotel.model.user.Staff;

import java.util.Optional;


public final class Session {

    //  account currently authenticated.

    public enum AccountKind {
        GUEST,
        STAFF
    }

    private static Guest currentGuest;
    private static Staff currentStaff;
    /** FXML path to return to after leaving Chat; cleared when consumed. */
    private static String chatReturnFxml;

    private Session() {
    }

    public static Guest getCurrentGuest() {
        return currentGuest;
    }

    public static void setCurrentGuest(Guest guest) {
        currentGuest = guest;
        if (guest != null) {
            currentStaff = null;
        }
    }

    public static Staff getCurrentStaff() {
        return currentStaff;
    }

    public static void setCurrentStaff(Staff staff) {
        currentStaff = staff;
        if (staff != null) {
            currentGuest = null;
        }
    }

    public static boolean isLoggedIn() {
        return currentGuest != null || currentStaff != null;
    }

    public static Optional<AccountKind> getActiveAccountKind() {
        if (currentGuest != null) {
            return Optional.of(AccountKind.GUEST);
        }
        if (currentStaff != null) {
            return Optional.of(AccountKind.STAFF);
        }
        return Optional.empty();
    }

    public static boolean isGuestSession() {
        return currentGuest != null;
    }

    public static boolean isStaffSession() {
        return currentStaff != null;
    }

    /**
     * Sets the screen to open when the user leaves Chat (back). Call immediately before {@link Navigator#goTo(String)} to chat.
     */
    public static void setChatReturnPath(String fxmlPath) {
        chatReturnFxml = fxmlPath;
    }

    /**
     * Returns the configured chat return path, or a role-appropriate default, and clears the stored path.
     */
    public static String consumeChatReturnPathOrDefault() {
        if (chatReturnFxml != null) {
            String path = chatReturnFxml;
            chatReturnFxml = null;
            return path;
        }
        if (currentStaff != null) {
            return Navigator.STAFF_DASHBOARD;
        }
        return Navigator.DASHBOARD;
    }

    public static void clear() {
        currentGuest = null;
        currentStaff = null;
        chatReturnFxml = null;
    }
}
