import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

class Expense {
    int id;
    String category;
    double amount;
    LocalDate date;
    String note;

    public Expense(int id, String category, double amount, LocalDate date, String note) {
        this.id = id;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.note = note;
    }

    @Override
    public String toString() {
        return id + "," + category + "," + amount + "," + date + "," + note;
    }

    public static Expense fromCSV(String line) {
        String[] parts = line.split(",");
        return new Expense(
                Integer.parseInt(parts[0]),
                parts[1],
                Double.parseDouble(parts[2]),
                LocalDate.parse(parts[3]),
                parts[4]
        );
    }

    // Helper method to get the month-year key for this expense
    public String getMonthYear() {
        return this.date.getMonth() + "-" + this.date.getYear();
    }
}

public class ExpenseTrackerGUI {
    private static final String FILE_NAME = "expenses.csv";
    private static final double MONTHLY_BUDGET = 5000.00;
    private static List<Expense> expenses = new ArrayList<>();
    private static int idCounter = 1;
    private static final List<String> VALID_CATEGORIES = Arrays.asList(
        "Groceries", "Transport", "Entertainment", "Food", "Utilities", 
        "Shopping", "Healthcare", "Rent", "Savings", "Subscriptions", "Miscellaneous", "Other"
    );
    private static JFrame frame;
    private static JPanel panel;
    private static JTextArea displayArea;

    public static void main(String[] args) {
        loadExpenses();

        frame = new JFrame("Expense Tracker");
        frame.setSize(600, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = new JPanel();
        panel.setLayout(new BorderLayout());

        displayArea = new JTextArea();
        displayArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(displayArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Options");
        JMenuItem addItem = new JMenuItem("Add Expense");
        JMenuItem updateItem = new JMenuItem("Update Expense");
        JMenuItem deleteItem = new JMenuItem("Delete Expense");
        JMenuItem reportItem = new JMenuItem("Category Report");
        JMenuItem exportItem = new JMenuItem("Export to CSV");
        JMenuItem budgetItem = new JMenuItem("Check Budget");
        JMenuItem monthReportItem = new JMenuItem("Month-wise Report");

        menu.add(addItem);
        menu.add(updateItem);
        menu.add(deleteItem);
        menu.add(reportItem);
        menu.add(exportItem);
        menu.add(budgetItem);
        menu.add(monthReportItem);
        menuBar.add(menu);
        frame.setJMenuBar(menuBar);

        addItem.addActionListener(e -> addExpense());
        updateItem.addActionListener(e -> updateExpense());
        deleteItem.addActionListener(e -> deleteExpense());
        reportItem.addActionListener(e -> categoryReport());
        exportItem.addActionListener(e -> exportToCSV());
        budgetItem.addActionListener(e -> checkBudget());
        monthReportItem.addActionListener(e -> monthWiseReport());

        frame.add(panel);
        frame.setVisible(true);
    }

    private static void loadExpenses() {
        try {
            List<String> lines = Files.readAllLines(Paths.get(FILE_NAME));
            for (String line : lines) {
                Expense expense = Expense.fromCSV(line);
                expenses.add(expense);
                idCounter = Math.max(idCounter, expense.id + 1);
            }
        } catch (IOException e) {
            System.out.println("No existing data found. Starting fresh.");
        }
    }

    private static void saveExpenses() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Expense e : expenses) {
                writer.println(e);
            }
        } catch (IOException e) {
            System.out.println("Failed to save expenses.");
        }
    }

    private static void addExpense() {
        String category = (String) JOptionPane.showInputDialog(
                frame, 
                "Select Category", 
                "Add Expense", 
                JOptionPane.QUESTION_MESSAGE, 
                null, 
                VALID_CATEGORIES.toArray(), 
                "Groceries");

        if (category == null || category.equals("")) {
            return;
        }

        double amount = Double.parseDouble(JOptionPane.showInputDialog(frame, "Enter Amount:"));
        String note = JOptionPane.showInputDialog(frame, "Enter Note:");

        Expense expense = new Expense(idCounter++, category, amount, LocalDate.now(), note);
        expenses.add(expense);
        saveExpenses();
        displayArea.append("Expense Added: " + expense + "\n");
    }

    private static void updateExpense() {
        int id = Integer.parseInt(JOptionPane.showInputDialog(frame, "Enter Expense ID to update:"));
        for (Expense e : expenses) {
            if (e.id == id) {
                String category = (String) JOptionPane.showInputDialog(
                        frame, 
                        "Select Category", 
                        "Update Expense", 
                        JOptionPane.QUESTION_MESSAGE, 
                        null, 
                        VALID_CATEGORIES.toArray(), 
                        e.category);

                if (category == null || category.equals("")) {
                    return;
                }

                double amount = Double.parseDouble(JOptionPane.showInputDialog(frame, "New Amount:"));
                String note = JOptionPane.showInputDialog(frame, "New Note:");

                e.category = category;
                e.amount = amount;
                e.note = note;
                e.date = LocalDate.now();
                saveExpenses();
                displayArea.append("Expense Updated: " + e + "\n");
                return;
            }
        }
        displayArea.append("Expense not found.\n");
    }

    private static void deleteExpense() {
        int id = Integer.parseInt(JOptionPane.showInputDialog(frame, "Enter Expense ID to delete:"));
        expenses.removeIf(e -> e.id == id);
        saveExpenses();
        displayArea.append("Expense Deleted.\n");
    }

    private static void categoryReport() {
        displayArea.append("==== Category-wise Report ====\n");
        Map<String, Double> report = expenses.stream()
                .collect(Collectors.groupingBy(e -> e.category, Collectors.summingDouble(e -> e.amount)));
        report.forEach((cat, amt) -> displayArea.append(cat + ": ₹" + amt + "\n"));
    }

    private static void exportToCSV() {
        saveExpenses();
        displayArea.append("Data exported to " + FILE_NAME + "\n");
    }

    private static void checkBudget() {
        double currentMonthTotal = expenses.stream()
                .filter(e -> e.date.getMonth() == LocalDate.now().getMonth() &&
                             e.date.getYear() == LocalDate.now().getYear())
                .mapToDouble(e -> e.amount)
                .sum();
        displayArea.append("This month's total spending: ₹" + currentMonthTotal + "\n");
        if (currentMonthTotal > MONTHLY_BUDGET) {
            displayArea.append("⚠️ Budget exceeded! Limit: ₹" + MONTHLY_BUDGET + "\n");
        } else {
            displayArea.append("✅ You're within budget.\n");
        }
    }

    private static void monthWiseReport() {
        displayArea.append("==== Month-wise Expense Report ====\n");

        Map<String, Double> monthlyExpenses = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getMonthYear, Collectors.summingDouble(e -> e.amount)));

        List<String> months = Arrays.asList("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December");
        
        for (String monthYear : monthlyExpenses.keySet()) {
            displayArea.append(monthYear + ": ₹" + monthlyExpenses.get(monthYear) + "\n");

            String[] monthYearParts = monthYear.split("-");
            String month = monthYearParts[0];
            int monthIndex = months.indexOf(month);
            
            double lastMonthExpense = 0;
            if (monthIndex > 0) {
                String lastMonth = months.get(monthIndex - 1) + "-" + monthYearParts[1];
                lastMonthExpense = monthlyExpenses.getOrDefault(lastMonth, 0.0);
            }
            
            if (monthlyExpenses.get(monthYear) > lastMonthExpense) {
                displayArea.append("⚠️ Spending increased compared to last month.\n");
            } else if (monthlyExpenses.get(monthYear) < lastMonthExpense) {
                displayArea.append("✅ Well done! You've reduced your spending this month.\n");
            }
        }
    }
}
