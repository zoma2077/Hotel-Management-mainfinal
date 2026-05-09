# Hotel Management System — Report

## Overview
This project is a Maven-based Java 17 application with a JavaFX (FXML) user interface that implements a hotel management / reservation workflow. The application uses an **in-memory database** for data storage and demonstrates role-based flows for **Guests**, **Receptionists**, and an **Admin**, plus a **socket-based chat** feature.

## Architecture (MVC)
The codebase follows an MVC-style organization:

- **Model**: Domain objects under `com.cse241.hotel.model.*` (e.g., `Room`, `Reservation`, `Guest`, `Invoice`) plus supporting `enums/`, `exceptions/`, and `interfaces/`.
- **View**: JavaFX FXML screens under `src/main/resources/fxml/` and a shared stylesheet `src/main/resources/style.css` (applied globally).
- **Controller**: JavaFX controllers under `com.cse241.hotel.ui.controller` (one controller per FXML screen).
- **Services**: Stateless service utilities under `com.cse241.hotel.services` that implement business rules on top of the in-memory database.
- **Persistence (in-memory)**: `com.cse241.hotel.db.HotelDatabase` stores collections for guests, staff, rooms, reservations, and invoices and provides seeding + lookup helpers.

Screen navigation is centralized in `com.cse241.hotel.ui.Navigator`, which owns the primary JavaFX `Stage` and swaps the scene root when changing screens.

## Domain model (key entities)
- **Users**
  - `Guest`: end-user who browses rooms and creates reservations.
  - `Staff`: base staff type; concrete roles include `Admin` and `Receptionist`.
  - `Role` enum: distinguishes roles for authentication/authorization logic.
- **Property**
  - `Room`: room number + type + amenities.
  - `RoomType`: name, price per night, capacity.
  - `Amenity`: add-on with optional cost.
- **Transactions**
  - `Reservation`: guest + room + date range + `ReservationStatus` (see [Reservation state machine](#reservation-state-machine-and-permissions) below).
  - `Invoice`: generated for checkout/payment workflows.
- **Business rules**
  - Overlap/availability checks prevent booking a room for overlapping dates in blocking statuses (`PENDING`, `CONFIRMED`, `CHECKED_IN`).

## Reservation state machine and permissions

**Authoritative rule source**: `com.cse241.hotel.services.ReservationWorkflow` defines who may perform which transition and when checkout is allowed. `com.cse241.hotel.services.ReservationService` applies those rules when confirming, checking in, or cancelling (with `ReservationWorkflow.Actor` for guest vs staff cancel). UI controllers (`ReservationManagementController` for guests, `ReceptionistReservationsController` for staff) disable buttons and show the same messages via the workflow helpers.

### States and typical flow

```
PENDING → CONFIRMED → CHECKED_IN → COMPLETED
                    ↘ CANCELLED ↙
```

- **PENDING**: new booking from `ReservationService.createReservation` (or seed data).
- **CONFIRMED**: staff has acknowledged the booking (`ReservationService.staffConfirmBooking`).
- **CHECKED_IN**: guest on-site (`ReservationService.staffCheckIn`), allowed from **CONFIRMED** or directly from **PENDING** (express check-in).
- **COMPLETED**: payment settled via `PaymentService.checkout` (sets status after successful payment).
- **CANCELLED**: terminal; may be reached while still **PENDING**, **CONFIRMED**, or **CHECKED_IN** (staff), or **PENDING** only (guest).

### Guest vs Receptionist / Admin

| Action | Guest | Receptionist / Admin (Manage reservations) |
|--------|-------|---------------------------------------------|
| **Confirm booking** | No | Yes, only while **PENDING** (`staffMayConfirm` / `staffConfirmBooking`) |
| **Check-in** | No | Yes, while **PENDING** or **CONFIRMED** (`staffMayCheckIn` / `staffCheckIn`) |
| **Checkout / Pay** | Yes, only **CONFIRMED** or **CHECKED_IN** | Same (`mayOpenCheckout`) |
| **Cancel** | Yes, only while **PENDING** (`guestMayCancel`; `cancelReservation(..., Actor.GUEST)`) | Yes while **PENDING**, **CONFIRMED**, or **CHECKED_IN** (`staffMayCancel`; not after **COMPLETED** / **CANCELLED**) |

New bookings start as **PENDING**; guests cannot confirm their own reservation—staff must confirm (or staff may check in from **PENDING**).

### Staff dashboard: Manage reservations

From `staff-dashboard.fxml` / `StaffDashboardController`, **Admin** and **Receptionist** see **Manage reservations** (navigates to `receptionist-reservations.fxml`). **Admin** additionally sees **Manage rooms**. Staff without those roles are redirected if they open the receptionist screen directly.

### Payment and checkout guards

- **Opening checkout UI**: `ReservationWorkflow.mayOpenCheckout` — only **CONFIRMED** or **CHECKED_IN**. Tooltips use `reasonCheckoutBlocked` when disabled.
- **Processing payment**: `PaymentService.checkout` calls `ReservationWorkflow.reasonCheckoutBlocked` first; on success it creates an invoice, debits the guest balance, and sets status to **COMPLETED**. Additional validation: guest must have sufficient balance (`InvalidPaymentException` if not).

### Chat session and return paths (high level)

`Navigator.goToChat(returnFxmlPath)` stores a return screen in `Session` before showing chat. **Back** from chat uses `Session.consumeChatReturnPathOrDefault()` (clears the stored path): typically **Guest dashboard** (`DASHBOARD`) or **Staff dashboard** (`STAFF_DASHBOARD`), or the path passed when opening chat (e.g. `RECEPTIONIST_RESERVATIONS`, `ADMIN_ROOMS`). This keeps chat navigation separate from which user is logged in.

## Services (business logic layer)
Services are implemented as stateless utility classes:

- **Authentication / registration**: `com.cse241.hotel.services.AuthService`
  - Guest registration with uniqueness checks via `HotelDatabase.requireUniqueGuestUsername`.
  - Guest and staff login checks against the in-memory collections.
- **Reservations**: `com.cse241.hotel.services.ReservationService` + `ReservationWorkflow`
  - Creates reservations and enforces date validation and room availability (overlap detection).
  - Staff/guest cancel, confirm, and check-in paths enforce `ReservationWorkflow` rules.
- **Rooms / payments**: `RoomService` and `PaymentService`
  - Manage room list operations; checkout uses `ReservationWorkflow` before completing payment and setting **COMPLETED**.

## Database and seeding
All data is stored in-memory via static collections in `com.cse241.hotel.db.HotelDatabase`.

- **Seeding**: `HotelDatabase.seedDummyData()` populates:
  - Staff accounts (Admin, Receptionist)
  - Room types, amenities, and rooms
  - Example guests and initial reservations
- **Bootstrap**: `com.cse241.hotel.ui.MainApp` calls `seedDummyData()` at application startup before showing the login screen.
- **Testing support**: `HotelDatabase.resetForTests()` clears all collections and reseeds, enabling repeatable unit tests.

## JavaFX UI (screens)
FXML screens are located under `src/main/resources/fxml/` and loaded by `Navigator`:

- `login.fxml` / `LoginController`
- `register.fxml` / `RegisterController`
- `dashboard.fxml` / `DashboardController`
- `rooms.fxml` / `RoomBrowseController`
- `reservations.fxml` / `ReservationManagementController`
- `checkout.fxml` / `CheckoutController`
- `admin-rooms.fxml` / `AdminRoomsController`
- `receptionist-reservations.fxml` / `ReceptionistReservationsController`
- `staff-dashboard.fxml` / `StaffDashboardController`
- `chat.fxml` / `ChatController`

Navigation is done via `Navigator.goTo(<FXML_PATH>)`, which loads the FXML, swaps the `Scene` root, and reapplies the global stylesheet.

## Concurrency (Phase 3)
The UI layer uses standard JavaFX concurrency patterns:

- **Background threads for blocking work**: e.g., network/server startup and client connection in `ChatController` are started on daemon threads to keep the UI responsive.
- **UI-thread confinement**: UI updates from background threads use `Platform.runLater(...)`.
- **Reusable executors**: `com.cse241.hotel.ui.concurrent.FxExecutors` provides a daemon-threaded single executor via `DaemonThreadFactory` for safe background task execution when needed.

## Socket chat (Phase 4)
The chat feature uses plain TCP sockets with a simple line-based protocol:

- **Server**: `com.cse241.hotel.net.ChatServer`
  - Uses `ServerSocket` to accept clients on a background accept loop.
  - Maintains a thread-safe client set (concurrent set) and broadcasts messages.
- **Per-client handler**: `com.cse241.hotel.net.ClientHandler`
  - Each client runs in a dedicated daemon thread.
  - Expects an initial join line: `JOIN|username|role`
  - Subsequent messages: `MSG|text`
- **Client**: `com.cse241.hotel.net.client.ChatClient`
  - Connects to server and listens on a background thread.
  - Parses:
    - `SYS|text` for system messages
    - `FROM|username|role|text` for chat messages
- **UI**: `com.cse241.hotel.ui.controller.ChatController`
  - Can start a local server instance.
  - Connects/disconnects and sends messages, updating UI via `Platform.runLater`.

## How to run
From the project root:

```bash
mvn clean javafx:run
```

### Seeded credentials (demo)
- Admin: `admin` / `Admin1234`
- Receptionist: `reception` / `Reception1`

## How to test
Run unit tests from the project root:

```bash
mvn test
```

## Project structure (high level)
- `pom.xml`: Java 17 + JavaFX dependencies and `javafx-maven-plugin` configuration (`mainClass`: `com.cse241.hotel.ui.MainApp`)
- `src/main/java`: application code (models, services, UI, networking)
- `src/main/resources`: FXML + CSS
- `src/test/java`: JUnit 5 tests (reservation overlap, password validation, payment tests)

