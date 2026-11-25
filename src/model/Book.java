package model;

import java.time.LocalDate;

public class Book {
    private int id;
    private String title;
    private String author;
    private boolean isIssued;
    private int issuedToUserId;
    private LocalDate dueDate;
    private int requestedByUserId;

    public Book(int id, String title, String author) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isIssued = false;
        this.issuedToUserId = -1;
        this.dueDate = null;
        this.requestedByUserId = -1;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public boolean isIssued() {
        return isIssued;
    }

    public int getIssuedToUserId() {
        return issuedToUserId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public int getRequestedByUserId() {
        return requestedByUserId;
    }

    public void setRequestedByUserId(int userId) {
        this.requestedByUserId = userId;
    }

    public void issueTo(int userId, LocalDate dueDate) {
        this.isIssued = true;
        this.issuedToUserId = userId;
        this.dueDate = dueDate;
        this.requestedByUserId = -1;
    }

    public void returned() {
        this.isIssued = false;
        this.issuedToUserId = -1;
        this.dueDate = null;
        this.requestedByUserId = -1;
    }

    @Override
    public String toString() {
        if (isIssued) {
            return String.format("ID: %d | %s by %s | Issued to %d (Due: %s)", id, title, author, issuedToUserId,
                    dueDate);
        }
        return String.format("ID: %d | %s by %s | Available", id, title, author);
    }
}
