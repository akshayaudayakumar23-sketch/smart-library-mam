# Smart Library Management System (Java Web Portal)

A full-stack, role-based web application built from scratch in Core Java (`com.sun.net.httpserver.HttpServer`) with zero external framework dependencies. Designed to handle large-scale catalogs (**2,000+ books** and **500+ student accounts**) with automated loan approvals and overdue fine calculation.

## Key Features
- **Role-Based Authentication (`/`):** Separate login and self-registration portals for **Students** and **Librarians (Admin)**.
- **Student Catalog Portal (`/student`):** Visual book cover cards, instant keyword search, one-click *Request to Borrow*, and live tracking of return due dates and pending fines.
- **Admin Dashboard (`/admin`):** Real-time inventory metrics, borrow request approval/rejection queue, book addition/deletion, return processing, and a 1-click **2,000 Books + 500 Students** dataset generator.
- **Automated Fine Engine:** Assigns a 14-day return window on approval and calculates overdue penalties (`Rs. 10/day`) using Java's `java.time.LocalDate` API.
- **Persistent Storage:** Automatically serializes all transactions to `books.csv` and `users.csv`.

## Architecture
```text
src/
 ├── model/          # Book.java, User.java (Data Models)
 ├── database/       # FileStorage.java (CSV Disk Persistence)
 ├── service/        # LibraryService.java (Business Logic, Approvals & Fine Engine)
 └── LibraryWebServer.java  # Built-in HTTP Server & Dynamic HTML/CSS Views
```

## How to Run Locally
1. Open the project folder in **VS Code** (requires JDK 17+).
2. Open `src/LibraryWebServer.java` and click **Run** above `main(String[] args)`.
3. Open your browser and navigate to `http://localhost:8080`.
   - **Admin Login:** `admin` / `admin123`
   - **Student Login:** Enter `5001` (after generating sample data) or register a new ID.
