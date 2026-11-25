import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import model.Book;
import model.User;
import service.LibraryService;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LibraryWebServer {
    private static LibraryService service = new LibraryService();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", exchange -> sendResponse(exchange, buildLoginHtml("")));

        server.createContext("/login", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> p = parseForm(exchange);
                String role = p.get("role");
                if ("admin".equals(role)) {
                    if ("admin".equals(p.get("username")) && "admin123".equals(p.get("password"))) {
                        redirect(exchange, "/admin");
                        return;
                    }
                    sendResponse(exchange, buildLoginHtml("Invalid Admin credentials! Use admin / admin123"));
                    return;
                } else {
                    int id = Integer.parseInt(p.get("studentId"));
                    User u = service.findUserById(id);
                    if (u != null) {
                        redirect(exchange, "/student?uid=" + id);
                        return;
                    }
                    sendResponse(exchange,
                            buildLoginHtml("Student ID not found! Register below or generate 500 students in Admin."));
                    return;
                }
            }
            redirect(exchange, "/");
        });

        server.createContext("/register-student", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> p = parseForm(exchange);
                int id = Integer.parseInt(p.get("userId"));
                service.registerUser(new User(id, p.get("name").replace(",", " "), p.get("email").replace(",", " ")));
                redirect(exchange, "/student?uid=" + id);
                return;
            }
            redirect(exchange, "/");
        });

        server.createContext("/student", exchange -> {
            Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery());
            int uid = Integer.parseInt(q.getOrDefault("uid", "5001"));
            String search = q.getOrDefault("q", "").toLowerCase();
            sendResponse(exchange, buildStudentHtml(uid, search));
        });

        server.createContext("/request-book", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> p = parseForm(exchange);
                int uid = Integer.parseInt(p.get("uid"));
                service.requestBook(Integer.parseInt(p.get("bookId")), uid);
                redirect(exchange, "/student?uid=" + uid);
                return;
            }
            redirect(exchange, "/");
        });

        server.createContext("/admin", exchange -> {
            Map<String, String> q = parseQuery(exchange.getRequestURI().getQuery());
            String search = q.getOrDefault("q", "").toLowerCase();
            sendResponse(exchange, buildAdminHtml(search));
        });

        server.createContext("/add-book", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> p = parseForm(exchange);
                service.addBook(new Book(Integer.parseInt(p.get("id")), p.get("title").replace(",", " "),
                        p.get("author").replace(",", " ")));
            }
            redirect(exchange, "/admin");
        });

        server.createContext("/delete-book", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> p = parseForm(exchange);
                service.deleteBook(Integer.parseInt(p.get("bookId")));
            }
            redirect(exchange, "/admin");
        });

        server.createContext("/approve-request", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                service.approveRequest(Integer.parseInt(parseForm(exchange).get("bookId")));
            }
            redirect(exchange, "/admin");
        });

        server.createContext("/reject-request", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                service.rejectRequest(Integer.parseInt(parseForm(exchange).get("bookId")));
            }
            redirect(exchange, "/admin");
        });

        server.createContext("/return-book", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                service.returnBook(Integer.parseInt(parseForm(exchange).get("bookId")));
            }
            redirect(exchange, "/admin");
        });

        server.createContext("/seed-data", exchange -> {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                service.generateBulkData();
            }
            redirect(exchange, "/admin");
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Smart Library Portal Started! Open: http://localhost:8080");
    }

    private static String buildLoginHtml(String error) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><title>Smart Library Login</title><style>");
        sb.append(
                "body{font-family:'Segoe UI',sans-serif;background:linear-gradient(135deg,#1e3c72,#2a5298);margin:0;display:flex;justify-content:center;align-items:center;min-height:100vh}");
        sb.append(".wrap{display:flex;gap:25px;flex-wrap:wrap;justify-content:center;padding:20px}");
        sb.append(
                ".box{background:white;padding:30px;border-radius:12px;width:300px;box-shadow:0 10px 25px rgba(0,0,0,0.2)}");
        sb.append(
                "h2{margin-top:0;color:#1e3c72}input{width:92%;padding:10px;margin:8px 0;border:1px solid #ccc;border-radius:6px}");
        sb.append(
                "button{width:100%;padding:11px;background:#2a5298;color:white;border:none;border-radius:6px;cursor:pointer;font-weight:bold;margin-top:8px}");
        sb.append(
                ".err{background:#ffebee;color:#c62828;padding:10px;border-radius:6px;margin-bottom:15px;text-align:center}");
        sb.append("</style></head><body><div>");
        if (!error.isEmpty())
            sb.append("<div class='err'>").append(error).append("</div>");
        sb.append("<div class='wrap'>");
        sb.append(
                "<div class='box'><h2>Student Login</h2><form method='POST' action='/login'><input type='hidden' name='role' value='student'><input type='number' name='studentId' placeholder='Enter Student ID (e.g. 5001)' required><button type='submit'>Login as Student</button></form>");
        sb.append(
                "<hr style='margin:20px 0'><h3>New Student?</h3><form method='POST' action='/register-student'><input type='number' name='userId' placeholder='Choose Student ID' required><input type='text' name='name' placeholder='Full Name' required><input type='email' name='email' placeholder='College Email' required><button type='submit' style='background:#28a745'>Register & Enter</button></form></div>");
        sb.append(
                "<div class='box'><h2>Admin / Librarian</h2><p style='color:#666;font-size:13px'>Default: admin / admin123</p><form method='POST' action='/login'><input type='hidden' name='role' value='admin'><input type='text' name='username' placeholder='Admin Username' required><input type='password' name='password' placeholder='Password' required><button type='submit' style='background:#e67e22'>Login as Admin</button></form></div>");
        sb.append("</div></div></body></html>");
        return sb.toString();
    }

    private static String buildStudentHtml(int uid, String search) {
        User student = service.findUserById(uid);
        String sName = student != null ? student.getName() : "Student #" + uid;
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><title>Student Portal</title><style>");
        sb.append("body{font-family:'Segoe UI',sans-serif;margin:0;background:#f5f7fb}");
        sb.append(
                ".nav{background:#1e3c72;color:white;padding:15px 30px;display:flex;justify-content:space-between;align-items:center}");
        sb.append(
                ".container{padding:25px 35px}.cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:20px;margin-top:20px}");
        sb.append(
                ".book-card{background:white;border-radius:10px;padding:16px;box-shadow:0 3px 10px rgba(0,0,0,0.08);display:flex;flex-direction:column;justify-content:space-between}");
        sb.append(
                ".cover{height:110px;border-radius:8px;background:linear-gradient(135deg,#667eea,#764ba2);color:white;display:flex;align-items:center;justify-content:center;font-weight:bold;text-align:center;padding:10px;margin-bottom:12px}");
        sb.append(".badge{display:inline-block;padding:4px 8px;border-radius:12px;font-size:12px;font-weight:bold}");
        sb.append(
                ".avail{background:#e8f5e9;color:#2e7d32}.pend{background:#fff3e0;color:#ef6c00}.iss{background:#ffebee;color:#c62828}");
        sb.append(
                "button{width:100%;padding:9px;background:#2a5298;color:white;border:none;border-radius:6px;cursor:pointer;margin-top:10px;font-weight:bold}");
        sb.append(
                "table{width:100%;border-collapse:collapse;background:white;margin-top:15px;border-radius:8px;overflow:hidden}th,td{padding:12px;border-bottom:1px solid #eee;text-align:left}th{background:#1e3c72;color:white}");
        sb.append("</style></head><body>");
        sb.append("<div class='nav'><h2>Smart Library | Student Portal</h2><div>Welcome, <b>").append(sName)
                .append("</b> (ID: ").append(uid)
                .append(") &nbsp; <a href='/' style='color:#ffd54f'>Logout</a></div></div>");
        sb.append(
                "<div class='container'><h3>My Borrowed & Requested Books</h3><table><tr><th>Book ID</th><th>Title</th><th>Status</th><th>Due Date</th><th>Pending Fine</th></tr>");
        for (Book b : service.getAllBooks()) {
            if (b.getIssuedToUserId() == uid) {
                sb.append("<tr><td>").append(b.getId()).append("</td><td>").append(b.getTitle())
                        .append("</td><td><span class='badge iss'>Issued</span></td><td>").append(b.getDueDate())
                        .append("</td><td>Rs. ").append(service.calculateFine(b)).append("</td></tr>");
            } else if (b.getRequestedByUserId() == uid) {
                sb.append("<tr><td>").append(b.getId()).append("</td><td>").append(b.getTitle()).append(
                        "</td><td><span class='badge pend'>Pending Admin Approval</span></td><td>-</td><td>Rs. 0.0</td></tr>");
            }
        }
        sb.append("</table><h3 style='margin-top:30px'>Browse Library Catalog (Top 40 Matches)</h3>");
        sb.append("<form method='GET' action='/student'><input type='hidden' name='uid' value='").append(uid)
                .append("'><input type='text' name='q' value='").append(search)
                .append("' placeholder='Search title, author, or ID...' style='padding:10px;width:320px;border-radius:6px;border:1px solid #ccc'> <button type='submit' style='width:100px'>Search</button></form>");
        sb.append("<div class='cards'>");
        int shown = 0;
        for (Book b : service.getAllBooks()) {
            if ((search.isEmpty() || b.getTitle().toLowerCase().contains(search)
                    || b.getAuthor().toLowerCase().contains(search) || String.valueOf(b.getId()).contains(search))
                    && shown < 40) {
                sb.append("<div class='book-card'><div><div class='cover'>").append(b.getTitle()).append("</div><b>")
                        .append(b.getTitle()).append("</b><div style='color:#666;font-size:13px;margin:4px 0'>by ")
                        .append(b.getAuthor()).append(" (ID: ").append(b.getId()).append(")</div></div><div>");
                if (b.isIssued())
                    sb.append("<span class='badge iss'>Issued</span>");
                else if (b.getRequestedByUserId() != -1)
                    sb.append("<span class='badge pend'>Requested</span>");
                else
                    sb.append(
                            "<span class='badge avail'>Available</span><form method='POST' action='/request-book'><input type='hidden' name='uid' value='")
                            .append(uid).append("'><input type='hidden' name='bookId' value='").append(b.getId())
                            .append("'><button type='submit'>Request to Borrow</button></form>");
                sb.append("</div></div>");
                shown++;
            }
        }
        sb.append("</div></div></body></html>");
        return sb.toString();
    }

    private static String buildAdminHtml(String search) {
        List<Book> books = service.getAllBooks();
        List<User> users = service.getAllUsers();
        long issued = books.stream().filter(Book::isIssued).count();
        long pending = books.stream().filter(b -> b.getRequestedByUserId() != -1).count();
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><title>Admin Dashboard</title><style>");
        sb.append("body{font-family:'Segoe UI',sans-serif;margin:0;display:flex;background:#f4f6f9}");
        sb.append(
                ".sidebar{width:220px;background:#1a252f;color:white;min-height:100vh;padding:20px}.sidebar a{display:block;color:#bdc3c7;text-decoration:none;padding:10px 0}");
        sb.append(
                ".main{flex:1;padding:25px 35px}.stats{display:flex;gap:15px;margin-bottom:25px}.stat{background:white;padding:18px;border-radius:10px;flex:1;box-shadow:0 2px 6px rgba(0,0,0,0.08);border-left:4px solid #2a5298}");
        sb.append(
                ".grid{display:flex;gap:15px;margin-bottom:25px}.card{background:white;padding:18px;border-radius:10px;flex:1;box-shadow:0 2px 6px rgba(0,0,0,0.08)}");
        sb.append(
                "input{width:92%;padding:8px;margin:5px 0;border:1px solid #ccc;border-radius:5px}button{padding:9px 14px;background:#2a5298;color:white;border:none;border-radius:5px;cursor:pointer}");
        sb.append(
                "table{width:100%;border-collapse:collapse;background:white;margin-bottom:25px}th,td{padding:10px;border:1px solid #eee;text-align:left}th{background:#1a252f;color:white}");
        sb.append("</style></head><body>");
        sb.append(
                "<div class='sidebar'><h2>Smart Library</h2><p style='color:#7f8c8d'>Admin Panel</p><hr style='border-color:#2c3e50'><a href='/admin'>Dashboard</a><a href='/admin#requests'>Borrow Requests (")
                .append(pending)
                .append(")</a><a href='/admin#catalog'>Book Catalog</a><a href='/' style='color:#e74c3c;margin-top:20px'>Logout</a></div>");
        sb.append(
                "<div class='main'><div style='display:flex;justify-content:space-between;align-items:center'><h1>Librarian Dashboard</h1><form method='POST' action='/seed-data'><button type='submit' style='background:#27ae60'>+ Generate 2,000 Books & 500 Students</button></form></div>");
        sb.append("<div class='stats'><div class='stat'>Total Books<h2>").append(books.size())
                .append("</h2></div><div class='stat'>Issued Books<h2>").append(issued)
                .append("</h2></div><div class='stat'>Pending Requests<h2>").append(pending)
                .append("</h2></div><div class='stat'>Total Students<h2>").append(users.size())
                .append("</h2></div></div>");
        sb.append(
                "<div class='grid'><div class='card'><h3>Add New Book</h3><form method='POST' action='/add-book'><input type='number' name='id' placeholder='Book ID' required><input type='text' name='title' placeholder='Title' required><input type='text' name='author' placeholder='Author' required><button type='submit'>Add Book</button></form></div>");
        sb.append(
                "<div class='card'><h3>Return Book</h3><form method='POST' action='/return-book'><input type='number' name='bookId' placeholder='Book ID' required><button type='submit' style='background:#e67e22'>Mark Returned</button></form></div></div>");
        sb.append(
                "<h2 id='requests'>Pending Student Borrow Requests</h2><table><tr><th>Book ID</th><th>Book Title</th><th>Requested By (Student ID)</th><th>Action</th></tr>");
        for (Book b : books) {
            if (b.getRequestedByUserId() != -1) {
                sb.append("<tr><td>").append(b.getId()).append("</td><td>").append(b.getTitle())
                        .append("</td><td>Student #").append(b.getRequestedByUserId())
                        .append("</td><td><form method='POST' action='/approve-request' style='display:inline'><input type='hidden' name='bookId' value='")
                        .append(b.getId())
                        .append("'><button type='submit' style='background:#27ae60'>Approve</button></form> <form method='POST' action='/reject-request' style='display:inline'><input type='hidden' name='bookId' value='")
                        .append(b.getId())
                        .append("'><button type='submit' style='background:#c0392b'>Reject</button></form></td></tr>");
            }
        }
        sb.append(
                "</table><h2 id='catalog'>Book Inventory (Top 50 Matches)</h2><form method='GET' action='/admin' style='margin-bottom:10px'><input type='text' name='q' value='")
                .append(search)
                .append("' placeholder='Search books...' style='width:300px'> <button type='submit'>Search</button></form>");
        sb.append(
                "<table><tr><th>ID</th><th>Title</th><th>Author</th><th>Status</th><th>Issued To</th><th>Due Date</th><th>Delete</th></tr>");
        int count = 0;
        for (Book b : books) {
            if ((search.isEmpty() || b.getTitle().toLowerCase().contains(search)
                    || String.valueOf(b.getId()).contains(search)) && count < 50) {
                sb.append("<tr><td>").append(b.getId()).append("</td><td>").append(b.getTitle()).append("</td><td>")
                        .append(b.getAuthor()).append("</td><td>").append(b.isIssued() ? "Issued" : "Available")
                        .append("</td><td>").append(b.isIssued() ? b.getIssuedToUserId() : "-").append("</td><td>")
                        .append(b.getDueDate() != null ? b.getDueDate() : "-")
                        .append("</td><td><form method='POST' action='/delete-book'><input type='hidden' name='bookId' value='")
                        .append(b.getId())
                        .append("'><button type='submit' style='background:#c0392b;padding:5px 10px'>X</button></form></td></tr>");
                count++;
            }
        }
        sb.append("</table></div></body></html>");
        return sb.toString();
    }

    private static Map<String, String> parseForm(HttpExchange exchange) throws IOException {
        BufferedReader br = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
        return parseQuery(br.readLine());
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw != null) {
            for (String pair : raw.split("&")) {
                int idx = pair.indexOf("=");
                if (idx > 0)
                    map.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8),
                            URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private static void sendResponse(HttpExchange exchange, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void redirect(HttpExchange exchange, String loc) throws IOException {
        exchange.getResponseHeaders().set("Location", loc);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }
}
