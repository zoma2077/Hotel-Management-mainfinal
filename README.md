# Hotel Management System (JavaFX + Maven)

## Requirements
- Java 17+
- Maven 3.9+

## Run
From the project root:

```bash
mvn clean javafx:run
```

## Test
```bash
mvn test
```

## Default seeded accounts
The application seeds in-memory demo data at startup:
- Admin: `admin` / `Admin1234`
- Receptionist: `reception` / `Reception1`

Guests and rooms are also pre-seeded for demonstration.

## Notes
- Data storage is **in-memory** (see `com.cse241.hotel.db.HotelDatabase`).
- The UI is built with JavaFX FXML screens under `src/main/resources/fxml/`.

## Reservation workflow
Reservations follow **PENDING → CONFIRMED → CHECKED_IN → COMPLETED**, with **CANCELLED** as a terminal state. Business rules and role permissions are centralized in `ReservationWorkflow` and enforced by `ReservationService` and the payment layer. For the full state diagram, guest vs staff actions, checkout guards, staff dashboard entry points, and chat return paths, see [`docs/REPORT.md`](docs/REPORT.md#reservation-state-machine-and-permissions).

