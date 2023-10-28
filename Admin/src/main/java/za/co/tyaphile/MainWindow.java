package za.co.tyaphile;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class MainWindow extends JFrame {
    private JFrame window = new JFrame();
    private JTextField customerName, customerSurname, accountNumber, cardNumber;
    private JTable accounts;
    private DefaultTableModel tableModel;
    private JButton search, accClear, showTransactions, viewDetails, linkedCards, linkedFraud, linkedDispute,
            manageActiveCard, manageAccount, openAccount;
    private JPanel rightPane;
    private static final Map<String, Object> searchResults = new HashMap<>();

    public MainWindow() {
        setupGUI();
    }

    private void setupGUI() {
        try {
            for (UIManager.LookAndFeelInfo feel:UIManager.getInstalledLookAndFeels()) {
                if (feel.getName().equals("Nimbus")) {
                    UIManager.setLookAndFeel(feel.getClassName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        initViews();
        initListeners();

        JPanel topPanel =  new JPanel(new GridLayout(5, 4, 5, 5));
        topPanel.add(new JLabel("Customer name"));
        topPanel.add(customerName);
        topPanel.add(new JLabel("Customer surname"));
        topPanel.add(customerSurname);
        topPanel.add(new JLabel("Account number"));
        topPanel.add(accountNumber);
        topPanel.add(new JLabel("Card number"));
        topPanel.add(cardNumber);
        topPanel.add(accClear);
        topPanel.add(search);

        JScrollPane accountTable = new JScrollPane(accounts);
        accounts.setFillsViewportHeight(true);

        rightPane = new JPanel(new GridLayout(7, 1, 5, 5));
        rightPane.setPreferredSize(new Dimension(150, 0));
        rightPane.setVisible(false);
        rightPane.add(viewDetails);
        rightPane.add(showTransactions);
        rightPane.add(linkedCards);
        rightPane.add(linkedDispute);
        rightPane.add(linkedFraud);
        rightPane.add(manageAccount);
        rightPane.add(manageActiveCard);

        JPanel leftPane = new JPanel(new GridLayout(7, 1, 5, 5));
        leftPane.setPreferredSize(new Dimension(150, 0));
        leftPane.add(openAccount);

        JPanel topHolder = new JPanel(new BorderLayout());
        topHolder.add(topPanel, BorderLayout.CENTER);
        JPanel blank = new JPanel();
        JPanel blank1 = new JPanel();
        blank.setPreferredSize(new Dimension(150, 0));
        blank1.setPreferredSize(new Dimension(150, 0));
        topHolder.add(blank, BorderLayout.EAST);
        topHolder.add(blank1, BorderLayout.WEST);

        add(topHolder, BorderLayout.NORTH);
        add(accountTable, BorderLayout.CENTER);
        add(rightPane, BorderLayout.LINE_END);
        add(leftPane, BorderLayout.LINE_START);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 800));
        pack();
        setLocationRelativeTo(null);
//        setResizable(false);
        setVisible(true);
    }

    private void initViews() {
        String[] accountTableColumns = {"Name", "Surname", "Account", "Account type", "Closed", "Card"};

        customerName = new JTextField(15);
        customerSurname = new JTextField();
        accountNumber = new JTextField();
        cardNumber = new JTextField();
        tableModel = new DefaultTableModel(accountTableColumns, 0);
        accounts = new JTable(tableModel);
        accounts.removeColumn(accounts.getColumnModel().getColumn(accountTableColumns.length - 1));
        search = new JButton("Search account");
        accClear = new JButton("Clear search");
        showTransactions = new JButton("Show transactions");
        viewDetails = new JButton("View Account details");
        linkedCards = new JButton("Linked cards");
        linkedFraud = new JButton("Linked fraud");
        manageActiveCard = new JButton();
        linkedDispute = new JButton();
        manageAccount = new JButton();
        openAccount = new JButton("Open new Account");

//        accounts.setAutoCreateRowSorter(true);
        accounts.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    }

    private void initListeners() {
        search.addActionListener(e -> {
            runSearch();
            rightPane.setVisible(false);
        });

        accClear.addActionListener(e -> {
            accounts.clearSelection();
            tableModel.setRowCount(0);
            searchResults.clear();
            customerName.setText("");
            customerSurname.setText("");
            accountNumber.setText("");
            cardNumber.setText("");
            rightPane.setVisible(false);
        });

        viewDetails.addActionListener(evt -> {
            try {
                Vector<String> item = tableModel.getDataVector().elementAt(accounts.convertRowIndexToView(accounts.getSelectedRow()));
                String acc = item.get(2);
                System.out.println(acc + " -> " + searchResults.get(acc));
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        });

        accounts.getSelectionModel().addListSelectionListener(e -> {
            rightPane.setVisible(true);
            try {
                Vector<String> item = tableModel.getDataVector().elementAt(accounts.convertRowIndexToView(accounts.getSelectedRow()));
                String acc = item.get(2);
                showTransactions.setText("<html>Show transactions <br>on " + acc.replaceAll("-", "") + "</html>");
                linkedCards.setText("<html>Show cards linked <br>to " + acc.replaceAll("-", "") + "</html>");
                viewDetails.setText("<html>View Account details <br>for " + acc.replaceAll("-", "") + "</html>");
                linkedFraud.setText("<html>Fraud linked <br>to " + acc.replaceAll("-", "") + "</html>");
                linkedDispute.setText("<html>Disputes linked <br>to " + acc.replaceAll("-", "") + "</html>");
                manageAccount.setText("<html>Manage account <br> number " + acc.replaceAll("-", "") + "</html>");
                manageActiveCard.setText("<html>Manage active card <br>" +
                        formatCard(((Map<?, ?>) searchResults.get(acc)).get("card").toString()) + "</html>");

            } catch (ArrayIndexOutOfBoundsException ignored) {}
        });

        openAccount.addActionListener(evt -> new OpenAccount(window));
    }

    private void runSearch() {
        accounts.clearSelection();
        tableModel.setRowCount(0);
        searchResults.clear();
        try {
            String name = customerName.getText().trim();
            String surname = customerSurname.getText().trim();
            String account = accountNumber.getText().trim().replaceAll("-", "");
            String card = cardNumber.getText().trim().replaceAll("\\s+", "");
            ArrayList<Map<?, ?>> accounts;
            if (!customerName.getText().trim().isBlank() && !customerSurname.getText().trim().isBlank()) {
                if (!(accountNumber.getText().trim().isBlank() && cardNumber.getText().trim().isBlank())) {
                    accounts = (ArrayList<Map<?, ?>>) Client.sendRequest(
                            Client.searchAccount(name, surname, account, card, true)
                    ).get("data");
                } else {
                    accounts = (ArrayList<Map<?, ?>>) Client.sendRequest(
                            Client.searchAccount(name, surname, account, card, false)
                    ).get("data");
                }
            } else {
                if (!accountNumber.getText().trim().isBlank()) {
                    accounts = (ArrayList<Map<?, ?>>) Client.sendRequest(Client.searchAccount(account)).get("data");
                } else if (!cardNumber.getText().trim().isBlank()) {
                    accounts = (ArrayList<Map<?, ?>>) Client.sendRequest(Client.searchAccount(card)).get("data");
                } else if (!customerSurname.getText().trim().isBlank()) {
                    accounts = (ArrayList<Map<?, ?>>) Client.sendRequest(Client.searchAccount(surname)).get("data");
                } else if (!customerName.getText().trim().isBlank()) {
                    accounts = (ArrayList<Map<?, ?>>) Client.sendRequest(Client.searchAccount(name)).get("data");
                } else {
                    accounts = (ArrayList<Map<?, ?>>) Client.sendRequest(Client.searchAccount()).get("data");
                }
            }

            if (accounts.isEmpty()) {
                JOptionPane.showMessageDialog(null, "No results found", "No results", JOptionPane.INFORMATION_MESSAGE);
            } else {
                for (Map<?, ?> acc : accounts) {
                    String cName = acc.get("name").toString();
                    String cSurname = acc.get("surname").toString();
                    String accNum = acc.get("account_number").toString();
                    String accType = acc.get("type").toString();
                    Boolean accClosed = (Boolean) acc.get("on_close");
                    String cardNo = acc.get("card").toString();
                    searchResults.put(accNum, acc);
                    tableModel.addRow(new Object[]{cName, cSurname, accNum, accType, accClosed, cardNo});
                }
            }
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    private String formatCard(String card) {
        card = card.replaceAll("\\s+", "").trim();
        if (card.length() < 16) return "Invalid card";
        StringBuilder buffer = new StringBuilder(card);
        return buffer.replace(4, 12, " **** **** ").toString();
    }
}