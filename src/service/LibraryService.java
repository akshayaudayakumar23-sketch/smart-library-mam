package service;

import database.FileStorage;
import model.Book;
import model.User;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class LibraryService {
    private List<Book> books;
    private List<User> users;
    private static final int LOAN_DAYS = 14;
    private static final double FINE_PER_DAY = 10.0;

    public LibraryService() {
        this.books = FileStorage.loadBooks();
        this.users = FileStorage.loadUsers();
    }

    public List<Book> getAllBooks() {
        return books;
    }

    public List<User> getAllUsers() {
        return users;
    }

    public User findUserById(int userId) {
        return users.stream().filter(u -> u.getUserId() == userId).findFirst().orElse(null);
    }

    public void addBook(Book book) {
        books.add(book);
        FileStorage.saveBooks(books);
    }

    public void deleteBook(int bookId) {
        books.removeIf(b -> b.getId() == bookId);
        FileStorage.saveBooks(books);
    }

    public void registerUser(User user) {
        if (findUserById(user.getUserId()) == null) {
            users.add(user);
            FileStorage.saveUsers(users);
        }
    }

    public void requestBook(int bookId, int userId) {
        for (Book book : books) {
            if (book.getId() == bookId && !book.isIssued() && book.getRequestedByUserId() == -1) {
                book.setRequestedByUserId(userId);
                FileStorage.saveBooks(books);
                return;
            }
        }
    }

    public void approveRequest(int bookId) {
        for (Book book : books) {
            if (book.getId() == bookId && book.getRequestedByUserId() != -1) {
                LocalDate dueDate = LocalDate.now().plusDays(LOAN_DAYS);
                book.issueTo(book.getRequestedByUserId(), dueDate);
                FileStorage.saveBooks(books);
                return;
            }
        }
    }

    public void rejectRequest(int bookId) {
        for (Book book : books) {
            if (book.getId() == bookId) {
                book.setRequestedByUserId(-1);
                FileStorage.saveBooks(books);
                return;
            }
        }
    }

    public void issueBook(int bookId, int userId) {
        if (findUserById(userId) == null)
            return;
        for (Book book : books) {
            if (book.getId() == bookId && !book.isIssued()) {
                book.issueTo(userId, LocalDate.now().plusDays(LOAN_DAYS));
                FileStorage.saveBooks(books);
                return;
            }
        }
    }

    public void returnBook(int bookId) {
        for (Book book : books) {
            if (book.getId() == bookId && book.isIssued()) {
                book.returned();
                FileStorage.saveBooks(books);
                return;
            }
        }
    }

    public double calculateFine(Book book) {
        if (!book.isIssued() || book.getDueDate() == null)
            return 0.0;
        long overdueDays = ChronoUnit.DAYS.between(book.getDueDate(), LocalDate.now());
        return overdueDays > 0 ? overdueDays * FINE_PER_DAY : 0.0;
    }

    public void generateBulkData() {
        String[] subjects = { "Java Programming", "Data Structures", "Database Systems", "Operating Systems",
                "Computer Networks", "Artificial Intelligence", "Machine Learning", "Cloud Computing" };
        String[] authors = { "James Gosling", "Robert Martin", "Thomas Cormen", "Andrew Tanenbaum",
                "Stuart Russell", "Ian Goodfellow", "Abraham Silberschatz", "Martin Fowler" };

        for (int i = 0; i < 2000; i++) {
            String title = subjects[i % subjects.length] + " Vol " + ((i / subjects.length) + 1);
            books.add(new Book(1000 + i, title, authors[i % authors.length]));
        }
        for (int i = 1; i <= 500; i++) {
            users.add(new User(5000 + i, "Student " + i, "student" + i + "@college.edu"));
        }
        FileStorage.saveBooks(books);
        FileStorage.saveUsers(users);
    }
}
